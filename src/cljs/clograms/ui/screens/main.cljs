(ns clograms.ui.screens.main
  (:require [re-frame.core :as re-frame]
            [clograms.subs :as subs]
            [clograms.ui.components.toolbars :as toolbars]
            [clograms.events :as events]
            [clograms.re-grams.re-grams :as rg]
            [clograms.ui.components.general :as general-components]
            [clograms.ui.components.menues :as menues]))

(defn diagram []
  (let [dia @(re-frame/subscribe [::rg/diagram])]
    [:div.diagram-wrapper
     {:on-drop (fn [evt]
                 (let [{:keys [:entity/type :id :link-to]} (-> (cljs.reader/read-string (-> evt .-dataTransfer (.getData "entity-data"))))]
                   (re-frame/dispatch [::events/add-entity-to-diagram type id
                                       {:link-to-node-id link-to
                                        :client-x (.-clientX evt)
                                        :client-y (.-clientY evt)}])))
      :on-drag-over (fn [evt ] (.preventDefault evt))
      :on-click (fn [evt]
                  (re-frame/dispatch [::events/hide-context-menu]))}
     [rg/diagram dia]]))

(defn main-panel []
  (let [ctx-menu @(re-frame/subscribe [::subs/ctx-menu])
        loading? @(re-frame/subscribe [::subs/loading?])]
    [:div
     (if loading?
       [general-components/loading-spinner]
       [:div.app-wrapper {:class (when loading? "loading")}
        (when ctx-menu [menues/context-menu ctx-menu])
        [toolbars/top-bar]
        [toolbars/side-bar]
        [toolbars/bottom-bar]
        [diagram]])]))
