(ns clograms.menues
  (:require [clograms.db :as db]))

(defn show-context-menu [db ctx-menu]
  (db/set-ctx-menu db ctx-menu))

(defn hide-context-menu [db]
  (db/set-ctx-menu db nil))
