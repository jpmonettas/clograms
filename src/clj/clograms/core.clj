(ns clograms.core
  (:require [datascript.core :as d]
            [clindex.api :as idx]
            [clindex.schema :as schema]
            [clograms.index.re-frame :as re-frame-idx]
            [cognitect.transit :as transit])
  (:import [java.io ByteArrayOutputStream]))

(def current-platform (atom nil))
(def extra-schema
  (merge re-frame-idx/extra-schema))

(defn- prepare-datoms-for-serialization [datoms]
  (->> datoms
       (map (fn [[e a v t add?]]
              (if (#{:function/source-form
                     :multimethod/source-form
                     :spec.alpha/source-form
                     :fspec.alpha/source-form} a)
                [e a (binding [*print-meta* true] (pr-str v)) t add?]
                [e a v t add?])))))

(defn re-index-all
  [folder platform ws-send-fn]
  (reset! current-platform platform)
  (idx/index-project! folder
                      {:platforms #{platform}
                       :extra-schema extra-schema
                       :on-new-facts
                       (fn [new-datoms]
                         (println (format "Pushing %d new datoms to the UI" (count new-datoms)))
                         (ws-send-fn :sente/all-users-without-uid [:updates/datoms (prepare-datoms-for-serialization new-datoms)]))}))

(defn- log-db [datascript-db-str]
  (spit "./datascript-db.json" datascript-db-str)
  datascript-db-str)

(defn db-edn []
  (let [edn {:schema (idx/db-schema)
             :datoms (-> (idx/db @current-platform)
                         (d/datoms  :eavt)
                         prepare-datoms-for-serialization)}
        out (ByteArrayOutputStream.)
        writer (transit/writer out :json)
        _ (transit/write writer edn)
        out-str (.toString out)]
    (-> out-str
        log-db)))
