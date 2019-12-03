(ns clograms.server
  (:require [clograms.handler :as handler]
            [config.core :refer [env]]
            [clograms.core :as core]
            [org.httpkit.server :as http-server]
            [datascript.core :as d]
            [compojure.core :as compojure :refer [GET POST]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [get-sch-adapter]]
            )
  (:gen-class))

(defn build-websocket []
  ;; TODO: move all this stuff to sierra components or something like that
  (let [{:keys [ch-recv send-fn connected-uids
                ajax-post-fn ajax-get-or-ws-handshake-fn]}
        (sente/make-channel-socket! (get-sch-adapter)
                                    {:csrf-token-fn nil})]

    {:ws-routes (compojure/routes (GET  "/chsk" req (ajax-get-or-ws-handshake-fn req))
                                  (POST "/chsk" req (ajax-post-fn                req)))
     :ws-send-fn send-fn
     :connected-uids-atom connected-uids}))

(defn -main [& [folder platform]]
  (let [port (Integer/parseInt (or (env :port) "3000"))
        {:keys [ws-routes ws-send-fn]} (build-websocket)
        server (http-server/run-server (-> (compojure/routes ws-routes #'handler/routes)
                                           (wrap-cors :access-control-allow-origin [#"http://localhost:9500"]
                                                      :access-control-allow-methods [:get :put :post :delete])
                                           wrap-keyword-params
                                           wrap-params)
                                       {:port port})]
    (println "Indexing " folder "for platform" platform)
    (core/re-index-all folder (keyword platform) ws-send-fn)

    (println "Indexing done, open http://localhost:3000")))

(comment
  (-main "/home/jmonetta/my-projects/clindex"                "clj")
  (-main "/home/jmonetta/my-projects/clograms"                "cljs")
  (-main "/home/jmonetta/my-projects/district0x/memefactory" "cljs")


  )
