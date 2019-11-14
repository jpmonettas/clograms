(ns clograms.ui.components.general
  (:require [re-frame.core :refer [subscribe dispatch]]))

(defn loading-spinner []
  [:div.loading-overlay
   [:div.spinner-outer
    [:div.text "Loading..."]
    [:svg.spinner-inner {:viewBox"0 0 66 66"}
     [:circle {:fill "#504945"
               :cx "33"
               :cy "33"
               :r "30"
               :stroke-width "2"
               :stroke "url(#gradient)"
               :class "spinner-path"}]
     [:linearGradient {:id "gradient"}
      [:stop {:offset "50%"
              :stop-color "#e7ebf8"
              :stop-opacity "1"}]
      [:stop {:offset "65%"
              :stop-color "#e7ebf8"
              :stop-opacity ".5"}]
      [:stop {:offset "100%"
              :stop-color "#e7ebf8"
              :stop-opacity "0"}]]]]])

(defn accordion [accordion-id items-map]
  (let [active-item @(subscribe [:accordion/active-item accordion-id])]
   [:div.accordion
    (for [[item-id {:keys [title child]}] items-map]
      ^{:key (str item-id)}
      [:div.item {:class (when (= item-id active-item) "active")}
       [:div.title {:on-click #(dispatch [:accordion/activate-item accordion-id item-id])}
        title]
       [:div.body
        child]])]))
