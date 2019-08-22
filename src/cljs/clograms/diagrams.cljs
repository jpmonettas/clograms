(ns  clograms.diagrams
  (:require ["@projectstorm/react-diagrams" :as storm]
            ["@projectstorm/react-canvas-core" :as storm-canvas]
            ["react" :as react]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [clograms.events :as events]))


(defonce storm-atom (atom {}))

(defn build-node [{:keys [entity] :as node-map}]
  (doto (storm/DefaultNodeModel. (str (:namespace/name entity)
                                      "/"
                                      (:var/name entity))
                                 "rgb(0,192,255)")
    (.addInPort ">")
    (.addOutPort ">")))

(defn create-engine-and-model! []
  (let [model (storm/DiagramModel.)
        engine (doto (storm/default)
                 (.setModel model))]

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
         new-node (build-node node-map)
         dia-model (-> @storm-atom :storm/model)]
     (if link-to
       (let [link-to-node-model (get-node dia-model (:id link-to))
             link-to-port (get-port link-to-node-model (:port link-to))
             new-node-port (get-port new-node (case (:port link-to)
                                                :in :out
                                                :out :in))
             _ (js/console.log "Node 1" link-to-port)
             _ (js/console.log "Node 2" new-node-port)
             link (.link link-to-port new-node-port)
             _ (js/console.log "Link" link)]
         ;; TODO : Auto link disabled, don't know why doesn't work
         (.addAll dia-model new-node #_link))
       (.addAll dia-model new-node))

     (.setPosition new-node x y)
     ;; (r/force-update-all)
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
                                          (re-frame/dispatch-sync [::events/add-entity-to-diagram entity
                                                                   {:link-to-selected? true
                                                                    :x (.-x points)
                                                                    :y (.-y points)}])))
                              :onDragOver (fn [e] (.preventDefault e))}
                         (react/createElement storm-canvas/CanvasWidget #js {:engine engine} nil))))
