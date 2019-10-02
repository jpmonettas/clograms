(ns clograms.core
  (:require [datascript.core :as d]
            [clindex.api :as idx]
            [clindex.schema :as schema]))

(def current-platform (atom nil))

(defn re-index-all
  [folder platform]
  (reset! current-platform platform)
  (idx/index-project! folder
                      {:platforms #{platform}}))

(defn db-edn []
  (pr-str {:schema schema/schema
           :datoms (->> (d/datoms (idx/db @current-platform) :eavt)
                        (map (fn [[e a v]]
                               (if (= a :function/source-form)
                                 [e a (binding [*print-meta* true] (pr-str v))]
                                 [e a v]))))}))
