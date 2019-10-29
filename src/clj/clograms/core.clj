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

(defn- log-db [datascript-db-str]
  (spit "./datascript-db.edn" datascript-db-str)
  datascript-db-str)

(defn db-edn []
  (-> {:schema schema/schema
       :datoms (->> (d/datoms (idx/db @current-platform) :eavt)
                    (map (fn [[e a v]]
                           (if (or (= a :function/source-form)
                                   (= a :multimethod/source-form))
                             [e a (binding [*print-meta* true] (pr-str v))]
                             [e a v]))))}
      pr-str
      #_(log-db)))
