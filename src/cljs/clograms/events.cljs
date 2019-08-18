(ns clograms.events
  (:require [re-frame.core :as re-frame]
            [clograms.db :as db]
            [reagent.dom :as rdom]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [datascript.core :as d]
            ["@projectstorm/react-diagrams" :as storm]
            ))


;; (d/pull @db-conn '[:project/name {:project/dependency 6}] 2)
;; (d/pull @db-conn '[:project/name :project/dependency] 1)


(defn create-diagram-engine-and-model []
  (let [engine (doto (storm/DiagramEngine.) (.installDefaultFactories))
        model (storm/DiagramModel.)]
    (.setDiagramModel engine model)
    {:storm/model model
     :storm/engine engine}))

(re-frame/reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db (merge
         db/default-db
         {:diagram (create-diagram-engine-and-model)})
    :dispatch [::reload-db]}))

(re-frame/reg-event-fx
 ::reload-db
 (fn [cofxs [_]]
   {:http-xhrio {:method          :get
                 :uri             "http://localhost:3000/db"
                 :timeout         8000 ;; optional see API docs
                 :response-format (ajax/raw-response-format) ;; IMPORTANT!: You must provide this.
                 :on-success      [::db-loaded]
                 :on-failure      [:bad-http-result]}}))

(re-frame/reg-event-db
 ::db-loaded
 (fn [db [_ new-db]]
   (let [datascript-db (cljs.reader/read-string new-db)
         main-project-id (d/q '[:find ?pid .
                                :in $ ?pn
                                :where [?pid :project/name ?pn]]
                              datascript-db
                              'clindex/main-project)]
     (js/console.log "Have " (count datascript-db) "datoms. Main project id " main-project-id)
     (assoc db
            :datascript/db datascript-db
            :main-project/id main-project-id))))

(re-frame/reg-event-db
 ::add-entity-to-diagram
 (fn [db [_ e {:keys [link-to-selected? x y] :as opts}]]
   (let [new-node (storm/DefaultNodeModel. (str (:namespace/name e) "/" (:var/name e)) "rgb(0,192,255)")
         new-in-port (.addInPort new-node ">")
         new-out-port (.addOutPort new-node ">")]

     (if link-to-selected?
       (let [{:keys [:storm.entity/id :storm.entity/in-port-name :storm.entity/out-port-name]} (-> db :diagram :selected-entity)
             dia-model (-> db :diagram :storm/model)
             selected-node-model (.getNode dia-model id)
             [selected-node-port new-node-port] (case (:type e)
                                                  :call-to   [(.getPort selected-node-model out-port-name)
                                                              new-in-port]
                                                  :called-by [(.getPort selected-node-model in-port-name)
                                                              new-out-port])

             link (.link new-node-port selected-node-port)]
         (.addAll (-> db :diagram :storm/model) new-node link))
       (.addAll (-> db :diagram :storm/model) new-node))

     (.setPosition new-node (or x 500) (or y 500))

     ;; super hacky
     (rdom/force-update-all)


     (.addListener new-node #js {:selectionChanged (fn [obj]
                                                     (re-frame/dispatch
                                                      [::select-entity
                                                       (assoc e
                                                              :storm.entity/id (-> obj .-entity .-id)
                                                              :storm.entity/in-port-name (.getName new-in-port)
                                                              :storm.entity/out-port-name (.getName new-out-port))]))}))
   db))

(re-frame/reg-event-db
 ::select-entity
 (fn [db [_ e]]
   (assoc-in db [:diagram :selected-entity] e)))

;; node1 (doto (storm/DefaultNodeModel. "Node 1" "rgb(0,192,255)") (.setPosition 100 100))
;; port1 (.addOutPort node1 "Out")
;; node2 (doto (storm/DefaultNodeModel. "Node 2" "rgb(0,192,255)") (.setPosition 400 100))
;; port2 (.addInPort node2 "In")
;; node3 (doto (storm/DefaultNodeModel. "Node 3" "rgb(0,192,255)") (.setPosition 600 300))
;; port3 (.addInPort node3 "In")
;; link1 (doto (.link port1 port2)
;;         (.addLabel "Whats Up"))

;; // add a selection listener to each
;;  models.forEach(item => {
;;      item.addListener({
;;          selectionChanged: action("selectionChanged")
;;      });
;;  });
