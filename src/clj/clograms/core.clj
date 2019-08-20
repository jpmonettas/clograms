(ns clograms.core
  (:require [datascript.core :as d]
            [clindex.api :as idx]))

(def current-platform (atom nil))

(defn re-index-all
  [folder platform]
  (reset! current-platform platform)
  (idx/index-project! folder
                      {:platforms #{platform}}))

(defn db-edn []
  (pr-str (idx/db @current-platform)))
