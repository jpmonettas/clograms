(ns clograms.ui.components.menues
  (:require [re-frame.core :as re-frame]
            [clograms.events :as events]
            [clograms.re-grams.re-grams :as rg]))

(defn remove-entity-ctx-menu-option [node]
  {:label "Remove"
   :dispatch [::events/remove-entity-from-diagram (::rg/id node)]})

(defn remove-node-ctx-menu-option [node]
  {:label "Remove"
   :dispatch [::rg/remove-node (::rg/id node)]})

(defn edit-node-label-ctx-menu-option [node]
  {:label "Edit text"
   :dispatch [:text-edit-modal/create [::events/set-node-label (::rg/id node)]]})

(defn set-ns-color-ctx-menu-option [ns-name]
  {:label (str "Set " ns-name " namespace color to selected")
   :dispatch [::events/set-namespace-color ns-name]})

(defn set-project-color-ctx-menu-option [project-name]
  {:label (str "Set " project-name " project color to selected")
   :dispatch [::events/set-project-color project-name]})

(defn find-references [var-id node-id]
  {:label "Find references"
   :dispatch [::events/find-var-references var-id node-id]})

(defn edit-link-text-ctx-menu-option [link]
  {:label "Edit text"
   :dispatch [:text-edit-modal/create [::rg/set-link-label (::rg/id link)]]})

(defn remove-link-ctx-menu-option [link]
  {:label "Remove"
   :dispatch [::rg/remove-link (::rg/id link)]})

(defn find-project-protocols [project-id]
  {:label "Find project protocols"
   :dispatch [::events/find-project-protocols project-id]})

(defn find-project-multimethods [project-id]
  {:label "Find project multimethods"
   :dispatch [::events/find-project-multimethods project-id]})

(defn find-unreferenced-functions  [project-id]
  {:label "Find unreferenced functions"
   :dispatch [::events/find-unreferenced-functions project-id]})

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
