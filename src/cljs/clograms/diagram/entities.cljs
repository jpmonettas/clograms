(ns clograms.diagram.entities
  (:require [clograms.models :as models]
            [re-frame.core :as re-frame]
            [clograms.re-grams.re-grams :as rg]))

(defn add-var-from-link [var-id]
  (re-frame/dispatch [:clograms.events/add-entity-to-diagram :var var-id {:link-to :last}]))

(defn add-entity-to-diagram [db entity-type id {:keys [link-to client-x client-y] :as opts}]
  (println "Adding entity to diagram" entity-type id " link to " link-to)
  (let [new-node-id (rg/gen-random-id)
        port-in-id (rg/gen-random-id)
        port-out-id (rg/gen-random-id)
        selected-node (rg/selected-node db)]
    {:dispatch-n (cond-> [[::rg/add-node (-> (models/build-node entity-type id)
                                             (assoc :client-x client-x
                                                    :client-y client-y
                                                    ::rg/id new-node-id))
                           [{:diagram.port/type nil ::rg/id port-in-id} {:diagram.port/type nil ::rg/id port-out-id}]]]
                   (and link-to selected-node) (into [[::rg/add-link

                                                       (let [port-sel-fn ({:first first :last last} link-to)
                                                             port-id (-> (rg/node-ports selected-node) vals port-sel-fn ::rg/id)]
                                                         [(::rg/id selected-node) port-id])

                                                       [new-node-id (if (= :first link-to)
                                                                      port-out-id
                                                                      port-in-id)]]]))}))

(defn remove-entity-from-diagram [db id]
  {:dispatch [::rg/remove-node id]})
