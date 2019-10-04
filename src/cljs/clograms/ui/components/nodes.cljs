(ns clograms.ui.components.nodes
  (:require [re-frame.core :as re-frame]
            [clograms.subs :as subs]
            [clograms.events :as events]
            [clograms.ui.components.menues :as menues]))

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

(defn project-node-component [{:keys [entity] :as node}]
  (let [project @(re-frame/subscribe [::subs/entity entity])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name project))
                              (menues/remove-ctx-menu-option node)]}
     [:div.project-node.custom-node
      [:div.node-body.project-name (str (:project/name project))]]]))

(defn namespace-node-component [{:keys [entity] :as node}]
  (let [ns @(re-frame/subscribe [::subs/entity entity])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name entity))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name entity))
                              (menues/remove-ctx-menu-option node)]}
    [:div.namespace-node.custom-node
     [:div.node-body
      [:span.namespace-name (str (:namespace/name ns))]
      [:span.project-name (str "(" (:project/name ns) ")")]]]]))

(defn var-node-component [{:keys [entity] :as node}]
  (let [var @(re-frame/subscribe [::subs/entity entity])]
    [node-wrapper {:node node
                   :ctx-menu [(menues/set-project-color-ctx-menu-option (:project/name entity))
                              (menues/set-ns-color-ctx-menu-option (:namespace/name entity))
                              (menues/remove-ctx-menu-option node)]}
     [:div.var-node.custom-node
      [:div.node-body
       [:div [:span.namespace-name (str (:namespace/name var) "/")] [:span.var-name (:var/name var)]]
       [:pre.source {:on-wheel (fn [e] (.stopPropagation e))
                     :dangerouslySetInnerHTML {:__html (:function/source-str var)}}]]]]))
