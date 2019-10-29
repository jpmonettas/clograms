(ns clograms.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [clograms.events :as events]
            [clograms.ui.screens.main :as main-screen]
            [clograms.ui.components.nodes :as nodes]
            [clograms.config :as config]
            [clograms.re-grams.re-grams :as re]
            [cljs.core.async :as async]
            [taoensso.sente  :as sente :refer [cb-success?]])
  (:require-macros [cljs.core.async.macros :as asyncm :refer [go-loop]]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

;; This is needed because if you try to add new datoms with with-db
;; datascript complains that it desn't know how to compare string and lists
;; TODO: review exactly why this happens and doesn't happend when you transact first time

(extend-protocol cljs.core/IComparable
  string
  (-compare [x y]
    (if (string? y)
      (.localeCompare x y)
      (throw (js/Error. (str "Cannot compare " x " to " y)))))

  cljs.core/List
  (-compare [x y]
    (if (list? y)
      (compare (str x) (str y))
      (throw (js/Error. (str "Cannot compare " x " to " y))))))

(defn mount-root []
  (re/register-node-component! :clograms/project-node nodes/project-node-component)
  (re/register-node-component! :clograms/namespace-node nodes/namespace-node-component)
  (re/register-node-component! :clograms/var-node nodes/var-node-component)
  (re-frame/clear-subscription-cache!)
  (reagent/render [main-screen/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root)

  ;; Read from websocket
  (let [{:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket-client! "/chsk"
                                                                                nil
                                                                                {:type :auto
                                                                                 :host "localhost"
                                                                                 :port 3000})] ; e/o #{:auto :ajax :ws}
    (go-loop [{:keys [?data]} (async/<! ch-recv)]
      (js/console.log "[WebSocket event]" ?data)
      (let [[ev dat] ?data]
        (case ev
          :updates/datoms (re-frame/dispatch [::events/new-datoms dat])
          nil))
      (recur (async/<! ch-recv)))
    (js/console.info "Websocket connection ready")))
