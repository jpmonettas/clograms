(ns clograms.diagram.entities
  (:require [clograms.models :as models]
            [re-frame.core :as re-frame]
            [clograms.re-grams.re-grams :as rg]
            [clograms.db :as db]))

(defn add-var-from-link [var-id from-node-id]
  (re-frame/dispatch [:clograms.events/add-entity-to-diagram :var var-id {:link-to-port :last :link-to-node-id from-node-id}]))

(defn auto-place-client-coords [{:keys [x y w h] :as link-to-node}]
  (let [x-gap 50
        auto-coords {:x (+ x w x-gap)
                     :y y}]
    auto-coords))

(defn add-entity-to-diagram [db entity-type id {:keys [link-to-port link-to-node-id client-x client-y] :as opts}]
  (println "Adding entity to diagram" entity-type id " link to node " link-to-node-id " and port " link-to-port)
  (let [new-node-id (rg/gen-random-id)
        port-in-id (rg/gen-random-id)
        port-out-id (rg/gen-random-id)
        selected-node (rg/selected-node db)
        entity-type (if (= :var entity-type) ;; figure out the concrete type if it is just a :var
                      (:var/type (db/var-entity (:datascript/db db) id))
                      entity-type)
        [link-event coords] (if (or link-to-node-id link-to-port)
                              (let [link-to-port (or link-to-port :first)
                                    link-to-node (or (rg/get-node db link-to-node-id)
                                                     selected-node)
                                    port-sel-fn (get {:first first :last last} link-to-port)
                                    port-id (-> (rg/node-ports link-to-node) vals port-sel-fn ::rg/id)
                                    from [(::rg/id link-to-node) port-id]
                                    to [new-node-id (if (= :first link-to-port)
                                                      port-out-id
                                                      port-in-id)]]
                                [[::rg/add-link from to]
                                 (if (and client-x client-y)
                                   {:client-x client-x :client-y client-y}
                                   (auto-place-client-coords link-to-node))])

                              ;; nothing to link to
                              [nil {:client-x client-x :client-y client-y}])]

    {:dispatch-n (cond-> [[::rg/add-node (-> (models/build-node entity-type id)
                                             (merge coords)
                                             (assoc ::rg/id new-node-id))
                           [{:diagram.port/type nil ::rg/id port-in-id} {:diagram.port/type nil ::rg/id port-out-id}]]]
                   link-event  (into [link-event]))}))

(defn remove-entity-from-diagram [db id]
  {:dispatch [::rg/remove-node id]})
