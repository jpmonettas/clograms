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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Custom nodes components ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn project-node-component [{:keys [project-name]}]
  [:div.project-node.custom-node
   [:div project-name]])

(defn namespace-node-component [{:keys [project-name nsname]}]
  [:div.namespace-node.custom-node
   [:span.namespace-name nsname]
   [:span.project-name (str "(" project-name ")")]])

(defn var-node-component [{:keys [nsname var-name source]}]
  [:div.var-node.custom-node
   [:div [:span.namespace-name (str nsname "/")] [:span.var-name var-name]]
   [:pre.source source]])

;;;;;;;;;;;;;;;;;;;;;;;
;; Custom node types ;;
;;;;;;;;;;;;;;;;;;;;;;;

(def-storm-custom-node "project-node"
  :node-factory-base abstract-react-factory
  :node-model-base node-model
  :node-factory-builder (make-project-node-factory [])
  :node-model-builder   (make-project-node-model [project-name])
  :render project-node-component)

(def-storm-custom-node "namespace-node"
  :node-factory-base abstract-react-factory
  :node-model-base node-model
  :node-factory-builder (make-namespace-node-factory [])
  :node-model-builder   (make-namespace-node-model [project-name nsname])
  :render namespace-node-component)


(def-storm-custom-node "var-node"
  :node-factory-base abstract-react-factory
  :node-model-base node-model
  :node-factory-builder (make-var-node-factory [])
  :node-model-builder   (make-var-node-model [nsname var-name source])
  :render var-node-component)


(defmulti build-node (fn [node-map] (-> node-map :entity :type)))

(defmethod build-node :project
  [node-map]
  (let [entity (:entity node-map)]
    (make-project-node-model (:project/name entity))))

(defmethod build-node :namespace
  [node-map]
  (let [entity (:entity node-map)]
    (make-namespace-node-model (:project/name entity)
                               (:namespace/name entity))))

(defmethod build-node :var
  [node-map]
  (let [entity (:entity node-map)]
    (make-var-node-model (:namespace/name entity)
                         (:var/name entity)
                         (-> (:function/source entity)
                             (str/replace "clojure.core/" "")
                             (str/replace "cljs.core/" "")
                             (zp/zprint-file-str {})))))

(defmethod build-node :default
  [{:keys [entity x y] :as node-map}]
  (doto (storm/DefaultNodeModel. #js {:name (str entity)
                                      :color "rgb(0,192,255)"})
    (.addInPort "In")
    (.addOutPort "Out")))

(defn create-engine-and-model! []
  (let [model (storm/DiagramModel.)
        engine (doto (storm/default)
                 (.setModel model))]

    ;; register factories
    (-> engine .getNodeFactories (.registerFactory (make-project-node-factory)))
    (-> engine .getNodeFactories (.registerFactory (make-namespace-node-factory)))
    (-> engine .getNodeFactories (.registerFactory (make-var-node-factory)))

    (reset! storm-atom
            {:storm/model model
             :storm/engine engine})))


(defn get-node [dia-model node-id]
  (some #(when (= (-> % .-options .-id) node-id)
           %)
        (.getNodes dia-model)))

(defn get-port [node-model ptype]
  (case ptype
    :in (-> node-model .-portsIn first)
    :out (-> node-model .-portsOut first)))

(re-frame/reg-fx
 ::add-node
 (fn [node-map]
   (let [{:keys [link-to x y on-node-selected on-node-unselected]} node-map
         new-node (doto (build-node node-map)
                    (.setPosition x y))
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

     (.registerListener new-node #js {:selectionChanged (fn [obj]
                                                          (re-frame/dispatch
                                                           (conj (if (.-isSelected obj)
                                                                   on-node-selected
                                                                   on-node-unselected)
                                                                 (assoc (:entity node-map)
                                                                        :storm.entity/id (-> obj .-entity .-options .-id)))))}))))

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
                              :onDragOver (fn [e] (.preventDefault e))}
                         (react/createElement storm-canvas/CanvasWidget #js {:engine engine} nil))))
