(ns clograms.server
  (:require [clograms.handler :refer [handler]]
            [config.core :refer [env]]
            [clograms.core :as core]
            [ring.adapter.jetty :refer [run-jetty]]
            [shadow.cljs.devtools.api :as shadow])
  (:gen-class))

(defn -main [& [folder platform build]]

  (when (= build "true")
    (shadow/release :app))

  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (println "Indexing " folder "for platform" platform)
    (core/re-index-all folder (keyword platform))
    (run-jetty handler {:port port :join? false})
    (println "Indexing done, open http://localhost:3000")))

(comment
  (-main "/home/jmonetta/my-projects/district0x/memefactory" "cljs")
  )
