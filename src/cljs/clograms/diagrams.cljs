(ns  clograms.diagrams
  (:require ["@projectstorm/react-diagrams" :as storm]
            ["@projectstorm/react-canvas-core" :as storm-canvas]
            ["@projectstorm/react-diagrams-defaults" :as storm-defaults]
            ["react" :as react]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [clograms.events :as events]
            [goog.object :as gobj]
            [zprint.core :as zp]
            [clojure.string :as str])
  (:require-macros [clograms.diagrams :refer [def-storm-custom-node]]))

(defonce storm-atom (atom {}))

(def abstract-react-factory storm-canvas/AbstractReactFactory)
(def node-model storm/NodeModel)

(def port-widget (r/adapt-react-class storm/PortWidget))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Custom nodes components ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn node-wrapper [{:keys [properties ctx-menu]} child]
  (let []
    [:div {:on-context-menu (fn [evt]
                             (let [x (.. evt -nativeEvent -pageX)
                                   y (.. evt -nativeEvent -pageY)]
                               (re-frame/dispatch
                                [::events/show-context-menu
                                 {:x x
                                  :y y
                                  :menu ctx-menu}])))}
     child]))

(defn remove-ctx-menu-option [node-model]
  {:label "Remove"
   :dispatch [::events/remove-entity-from-diagram (.. node-model -options -id)]})

(defn project-node-component [{:keys [node engine] :as all}]
  (r/create-class {:render
                   (fn [this]
                     (let [project (.-entity node)]
                       (js/console.log "REPAINTING " this #_(.. this props node -options -color))
                       [node-wrapper {:ctx-menu [(remove-ctx-menu-option node)]}
                        [:div.project-node.custom-node {:style {:background-color (.. node -options -color)}}
                         [port-widget {:engine engine :port (.getPort node "in")} ]
                         [:div.node-body.project-name (:project/name project)]
                         [port-widget {:engine engine :port (.getPort node "out")}]]]))}))

(defn namespace-node-component [{:keys [node engine]}]
  (let [ns (.-entity node)]
    [node-wrapper {:ctx-menu [(remove-ctx-menu-option node)]
                  :node-model node}
    [:div.namespace-node.custom-node
     [port-widget {:engine engine :port (.getPort node "in")} ]
     [:div.node-body
      [:span.namespace-name (:namespace/name ns)]
      [:span.project-name (str "(" (:project/name ns) ")")]]
     [port-widget {:engine engine :port (.getPort node "out")}]]]))

(defn var-node-component [{:keys [node engine]}]
  (let [var (.-entity node)]
   [node-wrapper {:ctx-menu [(remove-ctx-menu-option node)]
                  :node-model node}
    [:div.var-node.custom-node
     [port-widget {:engine engine :port (.getPort node "in")} ]
     [:div.node-body
      [:div [:span.namespace-name (str (:namespace/name var) "/")] [:span.var-name (:var/name var)]]
      [:pre.source {:on-wheel (fn [e] (.stopPropagation e))}
       (:function/source var)]]
     [port-widget {:engine engine :port (.getPort node "out")}]]]))

;;;;;;;;;;;;;;;;;;;;;;;
;; Custom node types ;;
;;;;;;;;;;;;;;;;;;;;;;;

(def-storm-custom-node "project-node"
  :node-factory-base abstract-react-factory
  :node-model-base node-model
  :node-factory-builder make-project-node-factory
  :node-model-builder   make-project-node-model
  :render project-node-component)

(def-storm-custom-node "namespace-node"
  :node-factory-base abstract-react-factory
  :node-model-base node-model
  :node-factory-builder make-namespace-node-factory
  :node-model-builder   make-namespace-node-model
  :render namespace-node-component)


(def-storm-custom-node "var-node"
  :node-factory-base abstract-react-factory
  :node-model-base node-model
  :node-factory-builder make-var-node-factory
  :node-model-builder   make-var-node-model
  :render var-node-component)


(defmulti build-node (fn [node-map] (-> node-map :entity :type)))

(defmethod build-node :project
  [node-map]
  (let [entity (:entity node-map)]
    (make-project-node-model entity)))

(defmethod build-node :namespace
  [node-map]
  (let [entity (:entity node-map)]
    (make-namespace-node-model entity)))

(defmethod build-node :var
  [node-map]
  (let [entity (:entity node-map)]
    (make-var-node-model (update entity :function/source
                                 (fn [src]
                                   (-> src
                                       (str/replace "clojure.core/" "")
                                       (str/replace "cljs.core/" "")
                                       (zp/zprint-file-str {})))))))

(defmethod build-node :default
  [{:keys [entity x y] :as node-map}]
  (doto (storm/DefaultNodeModel. #js {:name (str entity)
                                      :color "rgb(0,192,255)"})))

;;;;;;;;;;;;;;;
;; Listeners ;;
;;;;;;;;;;;;;;;

(defn nodes-updated-listener [node-model]
  (when (.-isCreated node-model)
    (re-frame/dispatch
     [::events/add-node {:storm.node/id (.. node-model -node -options -id)
                         :storm.node/type (.. node-model -node -options -type)
                         :x (.. node-model -node -position -x)
                         :y (.. node-model -node -position -y)
                         :clograms/entity (.. node-model -node -entity)}])))

(defn links-updated-listener [node-model])

(defn selection-changed-listener [{:keys [on-node-selected on-node-unselected] :as node-map} obj]
       (re-frame/dispatch
        (conj (if (.-isSelected obj)
                on-node-selected
                on-node-unselected)
              {:storm.node/id (-> obj .-entity .-options .-id)})))

(defn position-changed-listener  [obj]
       (re-frame/dispatch
        [::events/update-node-position (.. obj -entity -options -id)
         (.. obj -entity -position -x)
         (.. obj -entity -position -y)]))
(defn entity-removed-listener [obj]
       (re-frame/dispatch
        [::events/remove-node (.. obj -entity -options -id)]))

;;;;;;;;;;;
;; Utils ;;
;;;;;;;;;;;

(defn create-engine-and-model! []
  (let [model (doto (storm/DiagramModel.)
                (.registerListener #js {:nodesUpdated #(nodes-updated-listener %)
                                        :linksUpdated #(links-updated-listener %)}))
        engine (doto (storm/default)
                 (.setModel model))]

    ;; register factories
    (-> engine .getNodeFactories (.registerFactory (make-project-node-factory)))
    (-> engine .getNodeFactories (.registerFactory (make-namespace-node-factory)))
    (-> engine .getNodeFactories (.registerFactory (make-var-node-factory)))

    (reset! storm-atom
            {:storm/model model
             :storm/engine engine})))

(defn create-component []
  (let [engine (-> @storm-atom :storm/engine)]
    (react/createElement "div"
                         #js {:className "diagram-layer"
                              :onDrop (fn [event]
                                        (let [entity (cljs.reader/read-string (-> event .-dataTransfer (.getData "entity-data")))
                                              points (.getRelativeMousePoint engine event)]
                                          (re-frame/dispatch [::events/add-entity-to-diagram entity
                                                              {:link-to-selected? true
                                                               :x (.-x points)
                                                               :y (.-y points)}])))
                              :onDragOver (fn [e] (.preventDefault e))
                              :onClick (fn [e]
                                         (re-frame/dispatch [::events/hide-context-menu]))}
                         (react/createElement storm-canvas/CanvasWidget #js {:engine engine} nil))))

(defn get-node [dia-model node-id]
  (some #(when (= (-> % .-options .-id) node-id)
           %)
        (.getNodes dia-model)))

(defn get-port [node-model ptype]
  (case ptype
    :in (-> node-model .-portsIn first)
    :out (-> node-model .-portsOut first)))

;;;;;;;;;
;; FXs ;;
;;;;;;;;;

(re-frame/reg-fx
 ::add-node
 (fn [node-map]
   (let [{:keys [link-to x y on-node-selected on-node-unselected]} node-map
         new-node (doto (build-node node-map)
                    (.setPosition x y)
                    (.addPort (storm/DefaultPortModel. #js {:in true :name "in"}))
                    (.addPort (storm/DefaultPortModel. #js {:in false :name "out"})))
         dia-model (-> @storm-atom :storm/model)]

     (.addAll dia-model new-node)

     (when link-to
       (let [link-to-node-model (get-node dia-model (:id link-to))
             link-to-port (get-port link-to-node-model (:port link-to))
             new-node-port (get-port new-node (case (:port link-to)
                                                :in :out
                                                :out :in))
             link (.link new-node-port link-to-port)]
         ;; TODO : Auto link disabled, don't know why doesn't work
         #_(.addAll dia-model link))
       )
     (.repaintCanvas (-> @storm-atom :storm/engine))

     (.registerListener new-node #js {:selectionChanged #(selection-changed-listener node-map %)})
     (.registerListener new-node #js {:positionChanged #(position-changed-listener %)})
     (.registerListener new-node #js {:entityRemoved #(entity-removed-listener %)}))))

(re-frame/reg-fx
 ::remove-node
 (fn [node-id]
   (let [dia-model (-> @storm-atom :storm/model)
         node (get-node dia-model node-id)]
     (.removeNode dia-model node)
     (.repaintCanvas (-> @storm-atom :storm/engine)))))

(re-frame/reg-fx
 ::set-node-properties
 (fn [[node-id props-map]]
   (let [dia-model (-> @storm-atom :storm/model)
         node (get-node dia-model node-id)]
     (set! (.. node -options -color) props-map)
     #_(set! (.. node -position -x)500)
     ;; (.setPosition node 500 500)
     (js/console.log "New properties set " node)
     (.repaintCanvas (-> @storm-atom :storm/engine))
     (js/console.log "canvas repainted ")

     )))
