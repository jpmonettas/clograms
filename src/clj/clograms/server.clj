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
            [clojure.tools.cli :as tools-cli])
  (:gen-class))

(def server (atom nil))

(def cli-options
  [["-f" "--file FILE" "Diagram file"
    :default "diagram.edn"]
   ["-p" "--port PORT" "Port number"
    :default 3000
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-P" "--platform PLATFORM" "Platform to index, can be clj or cljs"
    :default :clj
    :parse-fn keyword
    :validate [#(contains? #{:clj :cljs} %) "Platform must be clj or cljs"]]
   ["-w" "--watch" "Watch project-folder for changes and reload"
    :default false]
   ["-h" "--help"]])

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


(defn -main [& args]
  (let [parsed-args (tools-cli/parse-opts args cli-options)]
    (if (or (-> parsed-args :options :help)
            (empty? (:arguments parsed-args)))
      (do
        (println "Usage : clograms [OPTIONS] project-folder")
        (println (-> parsed-args :summary)))

      (let [platform (-> parsed-args :options :platform)
            port (-> parsed-args :options :port)
            folder (-> parsed-args :arguments first)
            diagram-file (-> parsed-args :options :file)
            watch? (-> parsed-args :options :watch)
            {:keys [ws-routes ws-send-fn]} (build-websocket)]

        (reset! server (http-server/run-server (-> (compojure/routes ws-routes (handler/build-routes {:diagram-file diagram-file
                                                                                                      :port port
                                                                                                      :folder folder
                                                                                                      :platform platform}))
                                                   (wrap-cors :access-control-allow-origin [#"http://localhost:9500"]
                                                              :access-control-allow-methods [:get :put :post :delete])
                                                   wrap-keyword-params
                                                   wrap-params)
                                               {:port port
                                                :join? false}))

        (println "Indexing " folder "for platform " platform "watching:" watch?)

        (core/re-index-all folder platform ws-send-fn {:watch? watch?})

        (println (format "Indexing done, open http://localhost:%d" port))))))

(comment
  (-main "--platform" "clj" "--file" "mio.edn" "--port" "2000" "/home/jmonetta/my-projects/clindex")
  (-main "--platform" "cljs" "/home/jmonetta/my-projects/clograms")
  (-main "--platform" "cljs" "/home/jmonetta/my-projects/district0x/memefactory")


  )
