(ns clograms.browser
  (:require [clograms.db :as db]))

(defn side-bar-browser-back [db]
  (db/update-side-bar-browser-level db #(max (dec %) 0)))

(defn side-bar-browser-select-project [db p]
  (-> db
      (db/select-project (:project/id p))
      (db/update-side-bar-browser-level (constantly (db/project-browser-level-key->idx :namespaces)))))

(defn side-bar-browser-select-namespace [db ns]
  (-> db
      (db/select-namespace (:namespace/id ns))
      (db/update-side-bar-browser-level (constantly (db/project-browser-level-key->idx :vars)))))
