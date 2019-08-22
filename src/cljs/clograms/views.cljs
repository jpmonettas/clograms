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

(defn dependency-explorer []
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


(defn link [full-name path line]
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
                              (str (:namespace/name e) "/" (:var/name e)))
      :on-change (fn [e]
                   (when (map? e)
                     (re-frame/dispatch [::events/add-entity-to-diagram e])))]]))

(defn fn-ref-node [r]
  [:li {:draggable true
        :on-drag-start (fn [event]
                         (-> event
                             .-dataTransfer
                             (.setData "entity-data" (pr-str r))))}
   [:div
    [:div.namespace-name (:namespace/name r)]
    [:div.var-name (name (:var/name r) )]]
   [:div.project-name (str "(" (:project/name r) ")")]])

(defn side-bar []
  (let [refs @(re-frame/subscribe [::subs/selected-entity-refs])]
    [:div.side-bar
     [:div
      [:div.calls-to "Calls to"]
      (when-let [cbr (:calls-to refs)]
        [:ul
         (doall
          (for [r cbr]
            (let [s (str (:project/name r) ">" (:namespace/name r) "/" (:var/name r))]
              ^{:key s}
              [fn-ref-node r])))])]
     [:ul
      [:div.calls-to "Called by"]
      (when-let [cbr (:called-by refs)]
        (doall
         (for [r cbr]
           (let [s (str (:project/name r) ">" (:namespace/name r) "/" (:var/name r))]
             ^{:key s}
             [fn-ref-node r]))))]]))

(defn main-panel []
  [:div
   [entity-selector]
   [side-bar]])
