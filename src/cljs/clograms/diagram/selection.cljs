(ns clograms.diagram.selection
  (:require [clograms.db :as db]))

(defn node-selection-updated [db nodes]
  (when (= 1 (count nodes))
    (let [entity (-> nodes first :entity)]
      (case (:entity/type entity)
        :var {:db db}
        :project {:db db
                  :dispatch-n [[:clograms.events/side-bar-browser-select-project entity]]}
        :namespace {:db (let [project-id (->> (:namespace/id entity)
                                              (db/namespace-entity (:datascript/db db) )
                                              :project/id)]

                          ;; we also need to select a project and namespace since the browser back button needs it
                          (-> db
                              (db/select-project project-id)
                              (db/select-namespace (:namespace/id entity))))
                    :dispatch-n [[:clograms.events/side-bar-browser-select-namespace entity]]}
        {}))))
