(ns clograms.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [clograms.subs :as subs]
            [clograms.events :as events]
            [clojure.string :as str]
;;            cljsjs.d3
            [dorothy.core :as dorothy]
            [goog.string :as gstring]
            [re-com.core :as re-com]
            [clograms.re-grams :as rg]
            [clograms.db :as db]))

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

;;;;;;;;;;;;;;;;;;
;; Custom nodes ;;
;;;;;;;;;;;;;;;;;;

(defn node-wrapper [{:keys [ctx-menu node]} child]
  (let [project-color @(re-frame/subscribe [::subs/project-color (get-in node [:entity :project/name])])
        ns-color @(re-frame/subscribe [::subs/namespace-color (get-in node [:entity :namespace/name])])]
    [:div {:on-context-menu (fn [evt]
                             (let [x (.. evt -nativeEvent -pageX)
                                   y (.. evt -nativeEvent -pageY)]
                               (re-frame/dispatch
                                [::events/show-context-menu
                                 {:x x
                                  :y y
                                  :menu ctx-menu}])))
           :style {:background-color (or ns-color project-color)}}
    child]))

(defn remove-ctx-menu-option [node]
  {:label "Remove"
   :dispatch [::events/remove-entity-from-diagram (::rg/id node)]})

(defn set-ns-color-ctx-menu-option [ns-name]
  {:label (str "Set " ns-name " namespace color to selected")
   :dispatch [::events/set-namespace-color ns-name]})

(defn set-project-color-ctx-menu-option [project-name]
  {:label (str "Set " project-name " project color to selected")
   :dispatch [::events/set-project-color project-name]})

(defn project-node-component [{:keys [entity] :as node}]
  (let [project entity]
    [node-wrapper {:node node
                   :ctx-menu [(set-project-color-ctx-menu-option (:project/name project))
                              (remove-ctx-menu-option node)]}
     [:div.project-node.custom-node
      [:div.node-body.project-name (str (:project/name project))]]]))

(defn namespace-node-component [{:keys [entity] :as node}]
  (let [ns entity]
    [node-wrapper {:node node
                   :ctx-menu [(set-project-color-ctx-menu-option (:project/name entity))
                              (set-ns-color-ctx-menu-option (:namespace/name entity))
                              (remove-ctx-menu-option node)]}
    [:div.namespace-node.custom-node
     [:div.node-body
      [:span.namespace-name (str (:namespace/name ns))]
      [:span.project-name (str "(" (:project/name ns) ")")]]]]))

(defn var-node-component [{:keys [entity] :as node}]
  (let [var entity]
    [node-wrapper {:node node
                   :ctx-menu [(set-project-color-ctx-menu-option (:project/name entity))
                              (set-ns-color-ctx-menu-option (:namespace/name entity))
                              (remove-ctx-menu-option node)]}
     [:div.var-node.custom-node
      [:div.node-body
       [:div [:span.namespace-name (str (:namespace/name var) "/")] [:span.var-name (:var/name var)]]
       [:pre.source {:on-wheel (fn [e] (.stopPropagation e))
                     :dangerouslySetInnerHTML {:__html (:function/source-str var)}}
        ]]]]))

;; --------------------------------------------------------------------

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
                         (.setData "entity-data" {:entity/type :var
                                                  :id (:var/id var)})))}
   [:div
    [:div {:class (str "var " (if (:var/public? var) "public" "private"))}]
    [:span.var-name (:var/name var)]]])

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
                       ^{:key (str (:namespace/name r) (:var/name r))}
                       [draggable-ref-node r]))])]
    (fn []
      [:div.selected-browser
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
       :selected-browser [selected-browser])]))



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

(defn context-menu [{:keys [x y menu]}]
  [:div.context-menu {:style {:position :absolute
                              :top y
                              :left x
                              :z-index 100}}
   [:ul
    (for [{:keys [label dispatch]} menu]
      ^{:key label}
      [:li {:on-click (fn [evt]
                        (.stopPropagation evt)
                        (re-frame/dispatch dispatch)
                        (re-frame/dispatch [::events/hide-context-menu]))}
       label])]])

(defn diagram []
  (let [dia @(re-frame/subscribe [::rg/diagram])]
    [:div.diagram-wrapper
     {:on-drop (fn [evt]
                 (let [{:keys [:entity/type :id :link-to]} (-> (cljs.reader/read-string (-> evt .-dataTransfer (.getData "entity-data"))))]
                   (re-frame/dispatch [::events/add-entity-to-diagram type id
                                       {:link-to link-to
                                        :client-x (.-clientX evt)
                                        :client-y (.-clientY evt)}])))
      :on-drag-over (fn [evt ] (.preventDefault evt))
      :on-click (fn [evt]
                  (re-frame/dispatch [::events/hide-context-menu]))}
     [rg/diagram dia]]))

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

(defn main-panel []
  (let [ctx-menu @(re-frame/subscribe [::subs/ctx-menu])
        loading? @(re-frame/subscribe [::subs/loading?])]
    [:div
     (when loading? [loading-spinner])
     [:div.app-wrapper {:class (when loading? "loading")}
      (when ctx-menu [context-menu ctx-menu])
      [top-bar]
      [side-bar]
      [diagram]]]))
