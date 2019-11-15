(ns clograms.diagram.selection
  (:require [clograms.db :as db]))

(defn node-selected [db node]
  (let [entity (:entity node)]
    (case (:entity/type entity)
      :var {:db db}
      :project {:db db
                :dispatch-n [[:clograms.events/side-bar-browser-select-project entity]]}
      :namespace {:db (let [project-id (->> (:namespace/id entity)
                                            (db/namespace-entity (:datascript/db db) )
                                            :namespace/project
                                            :db/id)]
                        ;; we also need to select a project since the browser back button needs it
                        (db/select-project db project-id))
                  :dispatch-n [[:clograms.events/side-bar-browser-select-namespace entity]]}
      {})))
