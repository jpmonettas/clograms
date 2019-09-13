(ns clograms.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [clograms.events :as events]
            [clograms.views :as views]
            [clograms.config :as config]
            [clograms.re-grams :as re]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re/register-node-component! :clograms/project-node views/project-node-component)
  (re/register-node-component! :clograms/namespace-node views/namespace-node-component)
  (re/register-node-component! :clograms/var-node views/var-node-component)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
