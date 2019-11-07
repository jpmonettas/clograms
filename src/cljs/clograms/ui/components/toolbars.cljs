(ns clograms.ui.components.toolbars
  (:require [re-frame.core :as re-frame]
            [clograms.subs :as subs]
            [clograms.events :as events]
            [clojure.string :as str]
            [re-com.core :as re-com]
            [clograms.db :as db]))

(defn draggable-project [project]
  [:div.draggable-project.draggable-entity
   {:draggable true
    :class (when (= (:project/name project) 'clindex/main-project) "main-project")
    :on-drag-start (fn [event]
                     (-> event
                         .-dataTransfer
                         (.setData "entity-data" {:entity/type :project
                                                  :id (:project/id project)})))
    :on-click (fn [_]
                (re-frame/dispatch [::events/side-bar-browser-select-project project]))}
   [:div (str (:project/name project))]])

(defn draggable-namespace [namespace]
  [:div.draggable-namespace.draggable-entity
   {:draggable true
    :on-drag-start (fn [event]
                     (-> event
                         .-dataTransfer
                         (.setData "entity-data" {:entity/type :namespace
                                                  :id (:namespace/id namespace)})))
    :on-click (fn [_]
                (re-frame/dispatch [::events/side-bar-browser-select-namespace namespace]))}
   [:div (:namespace/name namespace)]])

(defn draggable-var [var]
  [:div.draggable-var.draggable-entity
   {:draggable true
    :on-drag-start (fn [event]
                     (-> event
                         .-dataTransfer ;; TODO: this should be serialized/deserialized  by hand to avoid loosing meta
                         (.setData "entity-data" {:entity/type (:var/type var)
                                                  :id (:var/id var)})))}
   [:div
    [:div {:class (str "var " (if (:var/public? var) "public" "private"))}]
    [:span.var-type (case (:var/type var)
                      :multimethod "(M)"
                      "")]
    [:span.var-name (:var/name var)]]])

(defn draggable-ref-node [r]
  [:li.draggable-var.draggable-entity
   {:draggable true
    :on-drag-start (fn [event]
                     (-> event
                         .-dataTransfer
                         (.setData "entity-data" {:entity/type :var
                                                  :id (:var/id r)
                                                  :link-to :first})))}
   [:div
    [:div.namespace-name (:namespace/name r)]
    [:div.var-name (name (:var/name r) )]]
   [:div.project-name (str "(" (:project/name r) ")")]])

(defn selected-var-browser []
  (let [refs (re-frame/subscribe [::subs/selected-var-refs])
        var-list (fn [vars]
                   [:ul
                    (doall
                     (for [r vars]
                       ^{:key (str (:namespace/name r) (:var/name r))}
                       [draggable-ref-node r]))])]
    (fn []
      [:div.selected-browser
       [:div
        [:div.header "Called by"]
        (when-let [vars (:called-by @refs)]
          [var-list vars])]])))

(defn projects-browser []
  (let [browser-level @(re-frame/subscribe [::subs/side-bar-browser-level])
        items @(re-frame/subscribe [::subs/side-bar-browser-items])
        item-component (case browser-level
                         :projects draggable-project
                         :namespaces draggable-namespace
                         :vars draggable-var)]
    [:div.projects-browser
     [:div.head-bar
      [:button.back {:on-click #(re-frame/dispatch [::events/side-bar-browser-back])} "<"]
      [:span.browser-selection {:class (str "draggable-" ({:vars "namespace" :namespaces "project"} browser-level))}
       (case browser-level
         :projects ""
         :namespaces (-> @(re-frame/subscribe [::subs/side-bar-browser-selected-project])
                         :project/name)
         :vars (-> @(re-frame/subscribe [::subs/side-bar-browser-selected-namespace])
                   :namespace/name))]]
     [:div.items
      (for [i items]
        ^{:key (str (case browser-level
                      :projects (:project/id i)
                      :namespaces (:namespace/id i)
                      :vars (:var/id i)))}
        [item-component i])]]))


(defn entity-selector []
  (let [all-entities (re-frame/subscribe [::subs/all-entities])]
    [:div.entity-selector
     [:label "Search:"]
     [:div.type-ahead-wrapper
      [re-com/typeahead
       :width "600px"
       :change-on-blur? true
       :data-source (fn [q]
                      (when (> (count q) 2)
                        (filter #(str/includes? (name (:var/name %)) q) @all-entities)))
       :render-suggestion (fn [e q]
                            [:span.selector-option
                             [:span.namespace-name (str (:namespace/name e) "/")]
                             [:span.var-name (:var/name e)]
                             [:span.project-name (str "(" (:project/name e) ")")]])
       :suggestion-to-string (fn [e]
                               (when (:namespace/name e)
                                 (str (:namespace/name e) "/" (:var/name e))))
       :on-change (fn [e]
                    (when (map? e)
                      (re-frame/dispatch [::events/add-entity-to-diagram :var (:var/id e)])))]]]))

(defn color-selector []
  (let [selected-color @(re-frame/subscribe [::subs/selected-color])]
    [:div.color-selector
     (for [c db/selectable-colors]
       ^{:key c}
       [:div.selectable-color {:style {:background-color c}
                               :class (when (= c selected-color) "selected")
                               :on-click #(re-frame/dispatch [::events/select-color c])}])]))

(defn top-bar []
  [:div.top-bar
   [:button.save {:on-click #(re-frame/dispatch [::events/save-diagram])}
    "Save"]
   [entity-selector]
   [color-selector]])

(defn side-bar []
  (let [refs @(re-frame/subscribe [::subs/selected-var-refs])
        tab @(re-frame/subscribe [::subs/selected-side-bar-tab])]
    [:div.side-bar
     [re-com/horizontal-tabs
      :model tab
      :class "side-bar-tabs"
      :tabs [{:id :projects-browser
              :label "Browser"}
             {:id :selected-browser
              :label "Selection"}]
      :on-change #(re-frame/dispatch [::events/select-side-bar-tab %])]
     (case tab
       :projects-browser [projects-browser]
       :selected-browser [selected-var-browser])]))
