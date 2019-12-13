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
        (db/set-bottom-bar-title (gstring/format "References: %s/%s (%s)"
                                                 (:namespace/name var-entity)
                                                 (:var/name var-entity)
                                                 (:project/name var-entity)))
        (db/set-var-references node-id references)
        (db/uncollapse-bottom-bar))))

(defn find-project-protocols [db project-id]
  (let [references (db/find-project-protocols (:datascript/db db) project-id)
        project-entity (db/project-entity (:datascript/db db) project-id)]
    (-> db
        (db/set-bottom-bar-title (gstring/format "Protocols: %s (%s)"
                                                 (:project/name project-entity)
                                                 (:project/version project-entity)))
        (db/set-var-references nil references)
        (db/uncollapse-bottom-bar))))

(defn find-project-multimethods [db project-id]
  (let [references (db/find-project-multimethods (:datascript/db db) project-id)
        project-entity (db/project-entity (:datascript/db db) project-id)]
    (-> db
        (db/set-bottom-bar-title (gstring/format "Multimethods: %s (%s)"
                                                 (:project/name project-entity)
                                                 (:project/version project-entity)))
        (db/set-var-references nil references)
        (db/uncollapse-bottom-bar))))

(defn find-unreferenced-functions [db project-id]
  (let [references (db/unreferenced-fns-in-project (:datascript/db db) project-id)
        project-entity (db/project-entity (:datascript/db db) project-id)]
    (-> db
        (db/set-bottom-bar-title (gstring/format "Unreferenced functions: %s (%s)"
                                                 (:project/name project-entity)
                                                 (:project/version project-entity)))
        (db/set-var-references nil references)
        (db/uncollapse-bottom-bar))))
