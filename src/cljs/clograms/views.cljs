(ns clograms.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [clograms.subs :as subs]
            [clograms.events :as events]
            [clojure.string :as str]
;;            cljsjs.d3
            [dorothy.core :as dorothy]
            [goog.string :as gstring]
            [re-com.core :as re-com]))

(defn all-projects [& {:keys [on-change selected-id]}]
  [:div "All projects"]
  #_(let [all @(re-frame/subscribe [::subs/all-projects])]
    [:div
     [ui/select-field {:floating-label-text "Projects"
                       :value (or selected-id (:db/id (first all)))
                       :on-change (fn [ev idx val] (on-change val))}
      (for [{:keys [:project/name :db/id]} all]
        ^{:key id}
        [ui/menu-item {:value id
                       :primary-text name}])]]))

(defn all-project-namespaces [& {:keys [on-change selected-id]}]
  [:div "All namespaces"]
#_  (let [all @(re-frame/subscribe [::subs/all-project-namespaces])]
    [:div
     [ui/select-field {:floating-label-text "Namespaces"
                       :value (or selected-id (:db/id (first all)))
                       :on-change (fn [ev idx val] (on-change val))}
      (for [{:keys [:namespace/name :db/id]} all]
        ^{:key id}
        [ui/menu-item {:value id
                       :primary-text name}])]]))

#_(defn dependency-explorer []
  (let [edges (re-frame/subscribe [::subs/projecs-dependencies-edges])
        graphviz (atom nil)
        redraw-graph (fn []
                       (let [eds @edges
                             all-nodes (into #{} (mapcat identity eds))]
                         (-> @graphviz
                             (.renderDot (->> all-nodes
                                              (map (fn [{:keys [:project/name :painted?]}]
                                                     [(str name) (cond-> {:shape :rectangle
                                                                    :fontname :helvetica
                                                                    :fontsize 10}
                                                             painted? (assoc :color :red
                                                                             :label (str name)))]))
                                              (into (map (fn [[n1 n2]]
                                                           [(str (:project/name n1))
                                                            (str (:project/name n2))])
                                                         eds))
                                              dorothy/digraph
                                              dorothy/dot)))))]
    (r/create-class
     {:component-did-mount (fn []
                             (reset! graphviz (-> (.select js/d3 "#dependency-graph")
                                                  .graphviz
                                                  (.transition (fn []
                                                                 (-> js/d3
                                                                     (.transition "main")
                                                                     (.ease (.-easeLinear js/d3))
                                                                     (.duration 800))))))
                             (redraw-graph))
      :component-did-update redraw-graph
      :reagent-render (fn []
                        [:div.dependency-explorer {:style {:margin 10}}
                         [:div.tree-panel
                          [:div#dependency-graph]]])})))


#_(defn link [full-name path line]
  (let [full-name (str full-name)
        name-style {:font-weight :bold
                    :color :blue}]
   [:a {:href (str "/open-file?path=" path "&line=" line "#line-tag") :target "_blank"}
    (if (str/index-of full-name "/")
      (let [[_ ns name] (str/split full-name #"(.+)/(.+)")]
        [:div {:style {:font-size 12}}
         [:span {:style {:color "#bbb"}}
          (str ns "/")]
         [:span.name {:style name-style} name]])
      [:div
       [:span.name {:style name-style} full-name]])]))

(defn entity-selector []
  (let [all-entities (re-frame/subscribe [::subs/all-entities])]
    [:div.entity-selector
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
                     (re-frame/dispatch [::events/add-entity-to-diagram (assoc e :type :var)])))]]))

(defn draggable-ref-node [r]
  [:li.draggable-var.draggable-entity
   {:draggable true
    :on-drag-start (fn [event]
                     (-> event
                         .-dataTransfer
                         (.setData "entity-data" (assoc r :type :var))))}
   [:div
    [:div.namespace-name (:namespace/name r)]
    [:div.var-name (name (:var/name r) )]]
   [:div.project-name (str "(" (:project/name r) ")")]])


(defn draggable-project [project]
  [:div.draggable-project.draggable-entity
   {:draggable true
    :on-drag-start (fn [event]
                     (-> event
                         .-dataTransfer
                         (.setData "entity-data" (assoc project :type :project))))
    :on-click (fn [_]
                (re-frame/dispatch [::events/side-bar-browser-select-project project]))}
   [:div (:project/name project)]])

(defn draggable-namespace [namespace]
  [:div.draggable-namespace.draggable-entity
   {:draggable true
    :on-drag-start (fn [event]
                     (-> event
                         .-dataTransfer
                         (.setData "entity-data" (assoc namespace :type :namespace))))
    :on-click (fn [_]
                (re-frame/dispatch [::events/side-bar-browser-select-namespace namespace]))}
   [:div (:namespace/name namespace)]])

(defn draggable-var [var]
  [:div.draggable-var.draggable-entity
   {:draggable true
    :on-drag-start (fn [event]
                     (-> event
                         .-dataTransfer
                         (.setData "entity-data" (assoc var :type :var))))}
   [:div (:var/name var)]])

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


(defn selected-browser []
  (let [refs (re-frame/subscribe [::subs/selected-var-refs])
        var-list (fn [vars]
                   [:ul
                    (doall
                     (for [r vars]
                       ^{:key (:var-id r)}
                       [draggable-ref-node r]))])]
    (fn []
      [:div.selected-browser
       [:div
        [:div.header "Calls to"]
        (when-let [vars (:calls-to @refs)]
          [var-list vars])]

       [:div
        [:div.header "Called by"]
        (when-let [vars (:called-by @refs)]
          [var-list vars])]])))

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
       :selected-browser [selected-browser])]
    ))

(defn main-panel []
  [:div
   [entity-selector]
   [side-bar]])
