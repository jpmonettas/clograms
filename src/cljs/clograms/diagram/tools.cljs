(ns clograms.diagram.tools
  (:require [clograms.db :as db]))

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
