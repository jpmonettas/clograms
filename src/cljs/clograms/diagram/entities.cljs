(ns clograms.diagram.entities
  (:require [clograms.models :as models]
            [re-frame.core :as re-frame]
            [clograms.re-grams.re-grams :as rg]
            [clograms.db :as db]))

(defn add-var-from-link [var-id from-node-id]
  (re-frame/dispatch [:clograms.events/add-entity-to-diagram :var var-id {:link-to-port :last :link-to-node-id from-node-id}]))

(defn add-entity-to-diagram [db entity-type id {:keys [link-to-port link-to-node-id client-x client-y] :as opts}]
  (println "Adding entity to diagram" entity-type id " link to node " link-to-node-id " and port " link-to-port)
  (let [new-node-id (rg/gen-random-id)
        port-in-id (rg/gen-random-id)
        port-out-id (rg/gen-random-id)
        selected-node (rg/selected-node db)
        entity-type (if (= :var entity-type) ;; figure out the concrete type if it is just a :var
                      (:var/type (db/var-entity (:datascript/db db) id))
                      entity-type)]
    {:dispatch-n (cond-> [[::rg/add-node (-> (models/build-node entity-type id)
                                             (assoc :client-x client-x
                                                    :client-y client-y
                                                    ::rg/id new-node-id))
                           [{:diagram.port/type nil ::rg/id port-in-id} {:diagram.port/type nil ::rg/id port-out-id}]]]
                   (or link-to-node-id link-to-port)  (into (let [link-to-port (or link-to-port :first)
                                                                  link-to-node (or (rg/get-node db link-to-node-id)
                                                                                   selected-node)
                                                                  port-sel-fn (get {:first first :last last} link-to-port)
                                                                  port-id (-> (rg/node-ports link-to-node) vals port-sel-fn ::rg/id)]
                                                              [[::rg/add-link

                                                                [(::rg/id link-to-node) port-id]

                                                                [new-node-id (if (= :first link-to-port)
                                                                               port-out-id
                                                                               port-in-id)]]])))}))

(defn remove-entity-from-diagram [db id]
  {:dispatch [::rg/remove-node id]})
