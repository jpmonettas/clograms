(ns clograms.diagram.tools
  (:require [clograms.db :as db]
            [goog.string :as gstring]))

(defn select-color [db color]
  (db/select-color db color))

(defn set-namespace-color [db ns-name]
  (db/set-namespace-color db
                    ns-name
                    (db/selected-color db)))

(defn set-project-color [db project-name]
  (db/set-project-color db
                        project-name
                        (db/selected-color db)))

(defn find-var-references [db var-id node-id]
  (let [references (db/var-x-refs (:datascript/db db) var-id)
        var-entity (db/var-entity (:datascript/db db) var-id)]
    (-> db
        (db/set-bottom-bar-title (gstring/format "References to %s/%s (%s)"
                                                 (:namespace/name var-entity)
                                                 (:var/name var-entity)
                                                 (:project/name var-entity)))
        (db/set-var-references node-id references)
        (db/uncollapse-bottom-bar))))
