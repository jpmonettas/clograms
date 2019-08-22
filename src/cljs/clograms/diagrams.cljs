(ns  clograms.diagrams
  (:require ["@projectstorm/react-diagrams" :as storm]
            ["@projectstorm/react-canvas-core" :as storm-canvas]
            ["@projectstorm/react-diagrams-defaults" :as storm-defaults]
            ["react" :as react]
            [re-frame.core :as re-frame]
            [reagent.core :as r]
            [clograms.events :as events]
            [goog.object :as gobj]))

(defonce storm-atom (atom {}))

(defn custom-node-cmp [props]
  [:div (str "Tha real thing" props)])

(do
  (defn CustomModel [m]
    (let [obj (js/Reflect.construct storm/NodeModel  #js [#js {:type "custom-node"}] CustomModel)]
      (set! (.-bla obj) (:bla m))
      obj))

  (js/Reflect.setPrototypeOf CustomModel.prototype storm/NodeModel.prototype)
  (js/Reflect.setPrototypeOf CustomModel storm/NodeModel))

(do (defn CustomFactory []
      (js/Reflect.construct storm-canvas/AbstractReactFactory  #js ["custom-node"] CustomFactory))

    (set! (-> CustomFactory .-prototype .-generateModel) (fn [event] (CustomModel {})))
    (set! (-> CustomFactory .-prototype .-generateReactWidget) (fn [event]
                                                                 (js/console.log "EVENT" event)
                                                                 (r/create-element (r/reactify-component custom-node-cmp)
                                                                                   #js {:name (-> event .-model .-bla)})))

    (js/Reflect.setPrototypeOf CustomFactory.prototype storm-canvas/AbstractReactFactory.prototype)
    (js/Reflect.setPrototypeOf CustomFactory storm-canvas/AbstractReactFactory))


(defn build-node [{:keys [entity x y] :as node-map}]
  (doto (storm/DefaultNodeModel. #js {:name (str (:namespace/name entity)
                                                 "/"
                                                 (:var/name entity))
                                      :color "rgb(0,192,255)"})
    (.setPosition x y)
    (.addInPort "In")
    (.addOutPort "Out")))

(defn create-engine-and-model! []
  (let [model (storm/DiagramModel.)
        engine (doto (storm/default)
                 (.setModel model))]

   (-> engine
        .getNodeFactories
        (.registerFactory (CustomFactory)))

   (.addAll model (CustomModel {:bla "The good name"}))


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
