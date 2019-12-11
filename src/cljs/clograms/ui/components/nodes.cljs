(ns clograms.ui.components.nodes
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [clograms.subs :as subs]
            [clograms.events :as events]
            [clograms.ui.components.menues :as menues]
            [clograms.re-grams.re-grams :as rg]
            [clojure.string :as str]
            [clograms.ui.components.general :as gral-components]
            [goog.string :as gstring]))

(defn node-wrapper [{:keys [ctx-menu node]} child]
  (let [node-color (re-frame/subscribe [::subs/node-color (:entity node)])]
    (fn [{:keys [ctx-menu node]} child]
      (let [node-comment (-> node :extra-data :comment)]
       [:div.node-wrapper {:on-context-menu (fn [evt]
                                              (.preventDefault evt)
                                              (let [x (.. evt -nativeEvent -pageX)
                                                    y (.. evt -nativeEvent -pageY)]
                                                (re-frame/dispatch
                                                 [::events/show-context-menu
                                                  {:x x
                                                   :y y
                                                   :menu ctx-menu}])))
                           :on-double-click (fn [evt]
                                              (re-frame/dispatch [::events/set-node-comment (::rg/id node) ""]))
                           :style (when-let [color @node-color]
                                    {:color color
                                     :box-shadow "10px 10px 18px"})}
        (when node-comment
          [:div.node-comment {:contentEditable true
                              :style {:bottom (str (:h node) "px")}
                              :on-key-down (fn [evt]
                                             ;; remove comment when backspace and comment already empty
                                             (let [div-txt (.. evt -target -innerHTML)]
                                               (when (and (= 8 (.-keyCode evt))
                                                          (str/blank? div-txt))
                                                 (re-frame/dispatch [::events/remove-node-comment (::rg/id node)]))))
                              :on-blur (fn [evt]
                                         (let [div-txt (.. evt -target -innerHTML)]
                                           (re-frame/dispatch [::events/set-node-comment (::rg/id node) div-txt])))
                              :dangerouslySetInnerHTML {:__html node-comment}}])

        child]))))

(defn project-node-component [{:keys [entity] :as node}]
  (let [project @(re-frame/subscribe [::subs/project-entity (:project/id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name project))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.project-node.custom-node
      [:div.node-body
       [:div.header
        [:div.title
         [:div.project-name (gral-components/project-name project)]
         [:div.project-version (:project/version project)]]]]]]))

(defn namespace-node-component [{:keys [entity] :as node}]
  (let [ns @(re-frame/subscribe [::subs/namespace-entity (:namespace/id entity)])
        collapsed? (-> node :extra-data :collapsed?)]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name ns))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name ns))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.namespace-node.custom-node
      [:div.node-body
       [:div.header
        [:div.title
         [:span.namespace-name (str (:namespace/name ns))]
         [:span.project-name (str "(" (:project/name ns) ")")]]
        [gral-components/collapse-button collapsed? {:on-click #(re-frame/dispatch [::events/toggle-collapse-node (::rg/id node)])}]]
       (when (and (not collapsed?)
                  (not-empty (:namespace/docstring ns)))
         [:pre.namespace-doc {:on-wheel (fn [e] (.stopPropagation e))}
          (:namespace/docstring ns)])]]]))

(defn function-node-component [{:keys [entity] :as node}]
  (let [var @(re-frame/subscribe [::subs/function-entity (:var/id entity) (::rg/id node)])
        spec-source (:fspec.alpha/source-str var)
        collapsed? (-> node :extra-data :collapsed?)]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name var))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name var))
                              (menues/find-references (:var/id entity) (::rg/id node))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.var-node.custom-node
      [:div.node-body
       [:div.header
        [:div.title
         [gral-components/collapse-button collapsed? {:on-click #(re-frame/dispatch [::events/toggle-collapse-node (::rg/id node)])}]
         [:span.namespace-name (str (:namespace/name var) "/")]
         [:span.var-name (:var/name var)]]]
       (if collapsed?
         [:ul.fn-args
          (for [args-vec (:function/args var)]
            ^{:key (str args-vec)}
            [:li.args-vec args-vec])]
         [:div
          (when spec-source
            [:pre.spec-source {:on-wheel (fn [e] (.stopPropagation e))
                               :dangerouslySetInnerHTML {:__html spec-source}}])
          [:pre.source {:on-wheel (fn [e] (.stopPropagation e))
                        :dangerouslySetInnerHTML {:__html (:function/source-str var)}}]])]]]))

(defn var-node-component [{:keys [entity] :as node}]
  (let [var @(re-frame/subscribe [::subs/var-entity (:var/id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name var))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name var))
                              (menues/find-references (:var/id entity) (::rg/id node))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.var-node.custom-node
      [:div.node-body
       [:div.header
        [:div.title
         [:span.namespace-name (str (:namespace/name var) "/")]
         [:span.var-name (:var/name var)]]]]]]))

(defn multimethod-node-component [{:keys [entity] :as node}]
  (let [expanded (r/atom {})]
    (fn [{:keys [entity] :as node}]
      (let [mm @(re-frame/subscribe [::subs/multimethod-entity (:var/id entity) (::rg/id node)])]
        [node-wrapper {:node node
                      :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name mm))
                                 (menues/set-ns-color-ctx-menu-option (:namespace/name mm))
                                 (menues/remove-entity-ctx-menu-option node)]}
        [:div.var-node.custom-node
         [:div.node-body
          [:div.header
           [:div.title
            [:span.namespace-name (str (:namespace/name mm) "/")]
            [:span.var-name (:var/name mm)]]]
          [:div
           (doall
            (for [method (:multi/methods mm)]
              ^{:key (pr-str (:multimethod/dispatch-val method))}
              [:div
               [:div.header
                [gral-components/collapse-button (not (get @expanded (:multimethod/dispatch-val method)))
                 {:on-click #(swap! expanded update (:multimethod/dispatch-val method) not)}]
                [:div.dispatch-val (pr-str (:multimethod/dispatch-val method))]]
               (when (@expanded (:multimethod/dispatch-val method))
                 [:pre.source {:on-wheel (fn [e] (.stopPropagation e))
                               :dangerouslySetInnerHTML {:__html (:multimethod/source-str method)}}])]))]]]]))))

(defn re-frame-node-body [collapsed? key-str source-str label node-id]
  [:div.node-body
   [:div.header
    [gral-components/collapse-button collapsed? {:on-click #(re-frame/dispatch [::events/toggle-collapse-node node-id])}]
    [:span.key key-str]
    [:span.title (gstring/format "(re-frame %s)" label)]]
   (when-not collapsed?
     [:div
      [:pre.source {:on-wheel (fn [e] (.stopPropagation e))
                    :dangerouslySetInnerHTML {:__html source-str}}]])])

(defn re-frame-subs-node-component [{:keys [entity] :as node}]
  (let [s @(re-frame/subscribe [::subs/re-frame-subs-entity (:id entity) (::rg/id node)])
        collapsed? (-> node :extra-data :collapsed?)]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name s))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name s))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [re-frame-node-body collapsed? (str (:re-frame.subs/key s)) (:source/str s) "subscription" (::rg/id node) ]]]))

(defn re-frame-event-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/re-frame-event-entity (:id entity) (::rg/id node)])
        collapsed? (-> node :extra-data :collapsed?)]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [re-frame-node-body collapsed? (str (:re-frame.event/key e)) (:source/str e) "event" (::rg/id node) ]]]))

(defn re-frame-fx-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/re-frame-fx-entity (:id entity) (::rg/id node)])
        collapsed? (-> node :extra-data :collapsed?)]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [re-frame-node-body collapsed? (str (:re-frame.fx/key e)) (:source/str e) "effect" (::rg/id node) ]]]))

(defn re-frame-cofx-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/re-frame-cofx-entity (:id entity) (::rg/id node)])
        collapsed? (-> node :extra-data :collapsed?)]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [re-frame-node-body collapsed? (str (:re-frame.cofx/key e)) (:source/str e) "coeffect" (::rg/id node) ]]]))

(defn spec-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/spec-entity (:spec/id entity)])
        collapsed (r/atom false)]
    (fn [{:keys [entity] :as node}]
      [node-wrapper {:node node
                     :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                                (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                                (menues/remove-entity-ctx-menu-option node)]}
       [:div.custom-node.spec-node
        [:div.node-body
         [:div.header [:div.title "Spec"]]
         [:div
          [gral-components/collapse-button @collapsed {:on-click #(swap! collapsed not)}]
          [:span (str (:spec.alpha/key e))]]
         (when-not @collapsed
           [:pre.source {:on-wheel (fn [e] (.stopPropagation e))}
            (str (:spec.alpha/source-str e))])]]])))

(defn shape-wrapper [{:keys [ctx-menu child]}]
  [:g {:on-context-menu (fn [evt]
                          (.preventDefault evt)
                          (let [x (.. evt -nativeEvent -pageX)
                                y (.. evt -nativeEvent -pageY)]
                            (re-frame/dispatch
                             [::events/show-context-menu
                              {:x x
                               :y y
                               :menu ctx-menu}])))}
   child])

(defn shape-menu [node]
  [(menues/remove-node-ctx-menu-option node)
   (menues/edit-node-label-ctx-menu-option node)])

(defn rectangle-node-component [node]
  [shape-wrapper
   {:ctx-menu (shape-menu node)
    :child [:g.rectangle-shape.custom-node
            [:rect {:width (:w node) :height (:h node) :rx 3}]
            [:text {:x (quot (:w node) 2) :y (quot (:h node) 2)
                    :text-anchor :middle}
             (-> node :extra-data :label)]]}])

(defn group-node-component [node _]
  [shape-wrapper
   {:ctx-menu (shape-menu node)
    :child
    [:g.group-shape.custom-node
     [:rect {:width (:w node)
             :height (:h node)
             :rx 3}]
     [:text {:x 5 :y 20}
      (or (-> node :extra-data :label) "<group title>") ]]}])

(defn svg-node-component [node svg-url]
  [shape-wrapper
   {:ctx-menu (shape-menu node)
    :child
    [:g.custom-svg-node
     [:image {:width (:w node) :height (:h node) :href svg-url}]
     [:text {:x (quot (:w node) 2) :y (quot (:h node) 2)
             :text-anchor :middle
             :fill "white"}
      (-> node :extra-data :label)]]}])
