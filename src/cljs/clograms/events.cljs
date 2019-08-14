(ns clograms.events
  (:require [re-frame.core :as re-frame]
            [clograms.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [datascript.core :as d]
            ["@projectstorm/react-diagrams" :as storm]
            ))


;; (d/pull @db-conn '[:project/name {:project/dependency 6}] 2)
;; (d/pull @db-conn '[:project/name :project/dependency] 1)

(defn create-diagram-engine-and-model []
  (let [engine (doto (storm/DiagramEngine.) (.installDefaultFactories))
        model (storm/DiagramModel.)
        node1 (doto (storm/DefaultNodeModel. "Node 1" "rgb(0,192,255)") (.setPosition 100 100))
        port1 (.addOutPort node1 "Out")
        node2 (doto (storm/DefaultNodeModel. "Node 2" "rgb(0,192,255)") (.setPosition 400 100))
        port2 (.addOutPort node2 "In")
        link1 (.link port1 port2)
        ]
    (.addAll model node1 node2)
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
