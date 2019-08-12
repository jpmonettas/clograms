(ns clograms.core
  (:require [datascript.core :as d]
            [clindex.api :as idx]))

(defn re-index-all
  [folder platform]
  (idx/index-project! folder platform))

(defn db-edn []
  (pr-str (idx/index-db)))
