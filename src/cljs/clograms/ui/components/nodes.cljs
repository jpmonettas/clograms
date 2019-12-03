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

(defn re-frame-subs-node-component [{:keys [entity] :as node}]
  (let [s @(re-frame/subscribe [::subs/re-frame-subs-entity (:id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name s))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name s))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [:div.node-body
       [:div.header
        [:div.title "Re frame subscription"]
        [:div.key (str (:re-frame.subs/key s))]]]]]))

(defn re-frame-event-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/re-frame-event-entity (:id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [:div.node-body
       [:div.header
        [:div.title "Re frame event"]
        [:div.key (str (:re-frame.event/key e))]]]]]))

(defn re-frame-fx-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/re-frame-fx-entity (:id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [:div.node-body
       [:div.header
        [:div.title "Re frame effect"]
        [:div.key (str (:re-frame.fx/key e))]]]]]))

(defn re-frame-cofx-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/re-frame-cofx-entity (:id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [:div.node-body
       [:div.header
        [:div.title "Re frame co-effect"]
        [:div.key (str (:re-frame.cofx/key e))]]]]]))

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

(defn circle-node-component [node]
  (let [cx (quot (:w node) 2)
        cy (quot (:h node) 2)]
    [shape-wrapper
     {:ctx-menu (shape-menu node)
      :child [:g.circle-shape.custom-node
              [:circle {:cx cx
                        :cy cy
                        :r (quot (max (:w node) (:h node)) 2)}]
              [:text {:x cx :y cy
                      :text-anchor :middle}
               (-> node :extra-data :label)]]}]))

(defn rectangle-node-component [node]
  [shape-wrapper
   {:ctx-menu (shape-menu node)
    :child [:g.rectangle-shape.custom-node
            [:rect {:width (:w node) :height (:h node) :rx 3}]
            [:text {:x (quot (:w node) 2) :y (quot (:h node) 2)
                    :text-anchor :middle}
             (-> node :extra-data :label)]]}])

(defn group-node-component [node]
  [shape-wrapper
   {:ctx-menu (shape-menu node)
    :child
    [:g.group-shape.custom-node
     [:rect {:width (:w node)
             :height (:h node)
             :rx 3}]
     [:text {:x 5 :y 20}
      (or (-> node :extra-data :label) "<group title>") ]]}])


(defn user-node-component [node]
  (let [draw-height 350
        draw-width 266
        scale (/ (:h node) draw-height)]
    [shape-wrapper
     {:ctx-menu (shape-menu node)
      :child
      [:g.user-shape.custom-node
       [:g {:transform (gstring/format "translate(0,0) scale(%f)" scale)}
        [:path {:d "M175,171.173c38.914,0,70.463-38.318,70.463-85.586C245.463,38.318,235.105,0,175,0s-70.465,38.318-70.465,85.587 C104.535,132.855,136.084,171.173,175,171.173z"}]
        [:path {:d "M41.909,301.853C41.897,298.971,41.885,301.041,41.909,301.853L41.909,301.853z"}]
        [:path {:d "M308.085,304.104C308.123,303.315,308.098,298.63,308.085,304.104L308.085,304.104z"}]
        [:path {:d "M307.935,298.397c-1.305-82.342-12.059-105.805-94.352-120.657c0,0-11.584,14.761-38.584,14.761
        s-38.586-14.761-38.586-14.761c-81.395,14.69-92.803,37.805-94.303,117.982c-0.123,6.547-0.18,6.891-0.202,6.131
        c0.005,1.424,0.011,4.058,0.011,8.651c0,0,19.592,39.496,133.08,39.496c113.486,0,133.08-39.496,133.08-39.496
        c0-2.951,0.002-5.003,0.005-6.399C308.062,304.575,308.018,303.664,307.935,298.397z"}]]
       [:text {:x (quot (:w node) 2) :y (:h node)
                    :text-anchor :middle}
        (-> node :extra-data :label)]]}]))
