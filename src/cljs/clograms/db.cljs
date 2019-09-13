(ns clograms.db
  (:require [clograms.re-grams :as rg]))

(def project-browser-transitions
  [:projects :namespaces :vars])

(def project-browser-level-idx->key (->> project-browser-transitions
                                         (map-indexed #(vector %1 %2))
                                         (into {})))
(def project-browser-level-key->idx (->> project-browser-transitions
                                         (map-indexed #(vector %2 %1))
                                         (into {})))

(defn select-project [db p]
  (assoc-in db [:projects-browser :selected-project] p))

(defn select-namespace [db ns]
  (assoc-in db [:projects-browser :selected-namespace] ns))

(defn set-ctx-menu [db ctx-menu]
  (assoc db :ctx-menu ctx-menu))

(def default-db
  (merge
   {:side-bar {:selected-side-bar-tab :projects-browser}
    :projects-browser {:level 0
                       :selected-project nil
                       :selected-namspace nil}
    :ctx-menu nil}
   (rg/initial-db)))
