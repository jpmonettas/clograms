(ns clograms.components.diagrams
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [clograms.subs :as subs]
            [clograms.events :as events]
            [clojure.string :as str]
            [goog.string :as gstring]
            ["@projectstorm/react-diagrams" :as storm]))

(def diagram-widget (r/adapt-react-class storm/DiagramWidget))

(defn diagram []

  (let [{:keys [:storm/engine]} @(re-frame/subscribe [::subs/diagram])]
    [:div.diagram
     [:div "Diagram here"]
     [:div.diagram-layer
      [diagram-widget
       {:diagram-engine engine
        :class-name "srd-demo-canvas"}]]]))
