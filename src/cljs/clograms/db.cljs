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

(defn select-color [db color]
  (assoc db :selected-color color))

(defn selected-color [db]
  (get db :selected-color))

(defn set-namespace-color [db ns color]
  (assoc-in db [:namespace/colors ns] color))

(defn namespace-color [db ns]
  (get-in db [:namespace/colors ns]))

(defn set-project-color [db project color]
  (assoc-in db [:project/colors project] color))

(defn project-color [db project]
  (get-in db [:project/colors project]))

(def selectable-colors #{"red" "green" "blue" "yellow"})

(def default-db
  (merge
   {:side-bar {:selected-side-bar-tab :projects-browser}
    :projects-browser {:level 0
                       :selected-project nil
                       :selected-namspace nil}
    :ctx-menu nil
    :selected-color (first selectable-colors)}
   (rg/initial-db)))
