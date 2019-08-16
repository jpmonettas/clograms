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
     [:div.diagram-layer
      {:on-drop (fn [event]
                  (let [r (cljs.reader/read-string (-> event .-dataTransfer (.getData "ref-data")))
                        points (.getRelativeMousePoint engine event)]
                    (re-frame/dispatch [::events/add-entity-to-diagram r {:link-to-selected? true
                                                                          :x (.-x points)
                                                                          :y (.-y points)}])))
       :on-drag-over (fn [e] (.preventDefault e))}
      [diagram-widget
       {:diagram-engine engine
        :max-number-points-per-link 0
        :class-name "srd-demo-canvas"}]]]))
