(ns clograms.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [clograms.events :as events]
            [clograms.views :as views]
            [clograms.config :as config]
            [clograms.diagrams :as diagrams]
            ["react-dom" :as react-dom]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (let [diagram-component nil #_(react-dom/render (diagrams/create-component)
                                            (.getElementById js/document "diagram-canvas"))]
    ;;(swap! diagrams/storm-atom assoc :storm/diagram-component diagram-component)
    (re-frame/clear-subscription-cache!)
    (reagent/render [views/main-panel]
                    (.getElementById js/document "app"))
    (reagent/render (diagrams/create-component)
                    (.getElementById js/document "diagram-canvas"))

    ))

(defn ^:export init []
  (diagrams/create-engine-and-model!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
