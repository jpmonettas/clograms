(ns clograms.diagram.entities
  (:require [clograms.models :as models]
            [re-frame.core :as re-frame]
            [clograms.re-grams.re-grams :as rg]
            [clograms.db :as db]))

(defn ^:export add-var-from-link [var-id from-node-id]
  (re-frame/dispatch [:clograms.events/add-entity-to-diagram :var var-id {:link-to-port :last :link-to-node-id from-node-id}]))

(defn auto-place-client-coords [{:keys [x y w h] :as link-to-node} where]
  (let [x-gap 50
        auto-coords {:x ((case where
                           :before -
                           :after +) x w x-gap)
                     :y y}]
    auto-coords))

;; TODO: improve this api so it is easier to express how you want to link to new node
(defn add-entity-to-diagram [db entity-type id {:keys [link-to-port link-to-node-id client-x client-y] :as opts}]
  (println "Adding entity to diagram" entity-type id " link to node " link-to-node-id " and port " link-to-port)
  (let [new-node-id (rg/gen-random-id)
        entity-type (if (= :var entity-type) ;; figure out the concrete type if it is just a :var
                      (:var/type (db/var-entity (:datascript/db db) id))
                      entity-type)
        [link-event coords] (if (or link-to-node-id link-to-port)
                              (let [link-to-port (or link-to-port :first)
                                    link-to-node (rg/get-node db link-to-node-id)
                                    [from to] (if (= link-to-port :last)
                                                ;; asume we go [dst node] -> [new node]
                                                [[(::rg/id link-to-node) 3] [new-node-id 7]]

                                                ;; asume we go [new node] -> [dst node]
                                                [[new-node-id 3] [(::rg/id link-to-node) 7]])]
                                [[::rg/add-link from to]
                                 (if (and client-x client-y)
                                   {:client-x client-x :client-y client-y}
                                   (auto-place-client-coords link-to-node (case link-to-port
                                                                            :first :before
                                                                            :last  :after)))])

                              ;; nothing to link to
                              [nil {:client-x client-x :client-y client-y}])]
    {:dispatch-n (cond-> [[::rg/add-node (-> (models/build-node entity-type id)
                                             (merge coords)
                                             (assoc ::rg/id new-node-id))]]
                   link-event  (into [link-event]))}))

(defn remove-entity-from-diagram [db id]
  {:dispatch [::rg/remove-node id]})
