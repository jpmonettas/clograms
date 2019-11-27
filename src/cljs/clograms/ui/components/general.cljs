(ns clograms.ui.components.general
  (:require [re-frame.core :refer [subscribe dispatch dispatch]]
            [reagent.core :as r]))

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
  (let [active-item (or @(subscribe [:accordion/active-item accordion-id])
                        (first (keys items-map)))]
   [:div.accordion
    (for [[item-id {:keys [title child]}] items-map]
      ^{:key (str item-id)}
      [:div.item {:class (when (= item-id active-item) "active")}
       [:div.title {:on-click #(dispatch [:accordion/activate-item accordion-id item-id])}
        title]
       [:div.body
        child]])]))

(defn collapse-button [collapsed? {:keys [on-click]}]
  [:i.collapse-button.zmdi {:on-click on-click
                            :class (if collapsed? "zmdi-caret-right" "zmdi-caret-down")}])

(defn min-max-button [collapsed? {:keys [on-click]}]
  [:i.collapse-button.zmdi {:on-click on-click
                            :class (if collapsed? "zmdi-window-maximize" "zmdi-window-minimize")}])

(defn text-edit-modal [on-text-set-event]
  (let [text (r/atom "")]
    (fn [on-text-set-event]
      [:div.modal-overlay
       [:div.text-edit-modal
        [:input {:value @text
                 :on-change (fn [evt] (reset! text (.. evt -target -value)))
                 :on-key-down (fn [evt]
                                (when (and (= 13 (.-keyCode evt))
                                           (dispatch [:text-edit-modal/set (conj on-text-set-event @text)]))))}]]])))

(defn project-name [project]
  (if (= (:project/name project) 'clindex/main-project)
    "Indexed project"
    (str (:project/name project))))
