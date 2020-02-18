(ns clograms.fxs
  (:require [re-frame.core :as re-frame]
            [clograms.events :as events]
            [cljs.core.async :as async]
            [taoensso.sente  :as sente :refer [cb-success?]])
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go-loop]]))

(re-frame/reg-fx
 :start-websocket
 (fn [{:keys [port]}]
   (println "Starting websocket on port " port)
   ;; Read from websocket
   (let [{:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket-client! "/chsk"
                                                                                 nil
                                                                                 {:type :auto
                                                                                  :host "localhost"
                                                                                  :port port})] ; e/o #{:auto :ajax :ws}
     (go-loop [{:keys [?data]} (async/<! ch-recv)]
       (js/console.log "[WebSocket event]" ?data)
       (let [[ev dat] ?data]
         (case ev
           :updates/datoms (re-frame/dispatch [::events/new-datoms dat])
           nil))
       (recur (async/<! ch-recv)))
     (js/console.info "Websocket connection ready"))))
