(ns clograms.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [clograms.events :as events]
            [clograms.ui.screens.main :as main-screen]
            [clograms.ui.components.nodes :as nodes]
            [clograms.config :as config]
            [clograms.re-grams.re-grams :as re]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

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
  (mount-root))
