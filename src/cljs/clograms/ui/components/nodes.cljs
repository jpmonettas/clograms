(ns clograms.ui.components.nodes
  (:require [re-frame.core :as re-frame]
            [reagent.core :as r]
            [clograms.subs :as subs]
            [clograms.events :as events]
            [clograms.ui.components.menues :as menues]
            [clograms.re-grams.re-grams :as rg]
            [clojure.string :as str]
            [clograms.ui.components.general :as gral-components]))

(defn node-wrapper [{:keys [ctx-menu node]} child]
  (let [node-color (re-frame/subscribe [::subs/node-color (:entity node)])
        node-comment (re-frame/subscribe [::subs/node-comment (::rg/id node)])]
    (fn [{:keys [ctx-menu node]} child]
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
      (when-let [comment @node-comment]
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
                            :dangerouslySetInnerHTML {:__html comment}}])

      child])))

(defn project-node-component [{:keys [entity] :as node}]
  (let [project @(re-frame/subscribe [::subs/project-entity (:project/id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name project))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.project-node.custom-node
      [:div.node-body
       [:div.header
        [:div.title
         [:div.project-name (str (:project/name project))]]]]]]))

(defn namespace-node-component [{:keys [entity] :as node}]
  (let [collapsed (r/atom false)]
   (fn [{:keys [entity] :as node}]
     (let [ns @(re-frame/subscribe [::subs/namespace-entity (:namespace/id entity)])]
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
           [gral-components/collapse-button @collapsed {:on-click #(swap! collapsed not)}]]
          (when (and (not @collapsed)
                     (not-empty (:namespace/docstring ns)))
            [:pre.namespace-doc {:on-wheel (fn [e] (.stopPropagation e))}
             (:namespace/docstring ns)])]]]))))

(defn function-node-component [{:keys [entity] :as node}]
  (let [collapsed (r/atom false)]
   (fn [{:keys [entity] :as node}]
     (let [var @(re-frame/subscribe [::subs/function-entity (:var/id entity) (::rg/id node)])
           spec-source (:fspec.alpha/source-str var)]
       [node-wrapper {:node node
                      :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name var))
                                 (menues/set-ns-color-ctx-menu-option (:namespace/name var))
                                 (menues/find-references (:var/id entity) (::rg/id node))
                                 (menues/remove-entity-ctx-menu-option node)]}
        [:div.var-node.custom-node
         [:div.node-body
          [:div.header
           [:div.title
            [gral-components/collapse-button @collapsed {:on-click #(swap! collapsed not)}]
            [:span.namespace-name (str (:namespace/name var) "/")]
            [:span.var-name (:var/name var)]]]
          (if @collapsed
            [:ul.fn-args
             (for [args-vec (:function/args var)]
               ^{:key (str args-vec)}
               [:li.args-vec args-vec])]
            [:div
             (when spec-source
               [:pre.spec-source {:on-wheel (fn [e] (.stopPropagation e))
                                  :dangerouslySetInnerHTML {:__html spec-source}}])
             [:pre.source {:on-wheel (fn [e] (.stopPropagation e))
                           :dangerouslySetInnerHTML {:__html (:function/source-str var)}}]])]]]))))

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
        [:div.title "Re frame subscription"]]
       [:div (str (:re-frame.subs/key s))]]]]))

(defn re-frame-event-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/re-frame-event-entity (:id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [:div.node-body
       [:div.header
        [:div.title "Re frame event"]]
       [:div (str (:re-frame.event/key e))]]]]))

(defn re-frame-fx-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/re-frame-fx-entity (:id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [:div.node-body
       [:div.header
        [:div.title "Re frame effect"]]
       [:div (str (:re-frame.fx/key e))]]]]))

(defn re-frame-cofx-node-component [{:keys [entity] :as node}]
  (let [e @(re-frame/subscribe [::subs/re-frame-cofx-entity (:id entity)])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name e))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name e))
                              (menues/remove-entity-ctx-menu-option node)]}
     [:div.custom-node.re-frame-node
      [:div.node-body
       [:div.header
        [:div.title "Re frame co-effect"]]
       [:div (str (:re-frame.cofx/key e))]]]]))

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

(defn circle-node-component [node]
  (let [cx (+ (:x node) (quot (:w node) 2))
        cy (+ (:y node) (quot (:h node) 2))]
    [shape-wrapper
     {:ctx-menu [(menues/remove-node-ctx-menu-option node)]
      :child [:g
              [:circle {:cx cx
                        :cy cy
                        :r (quot (max (:w node) (:h node)) 2)}]
              [:text {:x cx :y cy
                      :text-anchor :middle
                      :stroke :red} "Some text"]]}]))

(defn rectangle-node-component [node]
  [shape-wrapper
   {:ctx-menu [(menues/remove-node-ctx-menu-option node)]
    :child [:g
            [:rect {:x (:x node) :y (:y node) :width (:w node) :height (:h node) :rx 3}]
            [:text {:x (+ (:x node) (quot (:w node) 2)) :y (+ (:y node) (quot (:h node) 2))
                    :text-anchor :middle
                    :stroke :red}
             "Some text"]]}])

(defn group-node-component [node]
  [shape-wrapper
   {:ctx-menu [(menues/remove-node-ctx-menu-option node)]
    :child
    [:g
     [:rect {:x (:x node)
             :y (:y node)
             :width (:w node)
             :height (:h node)
             :stroke :grey
             :stroke-width 2
             :fill :none
             :rx 3}]
     [:text {:x (+ 5 (:x node)) :y (+ (:y node) 20)
             :stroke :grey}
      "Some text"]]}])
