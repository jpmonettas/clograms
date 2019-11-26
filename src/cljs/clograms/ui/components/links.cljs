(ns clograms.ui.components.links
  (:require [clograms.re-grams.re-grams :as rg]
            [clograms.ui.components.menues :as menues]
            [clograms.events :as events]
            [re-frame.core :as re-frame]))

(defn line-link-component [link]
  [:g {:on-context-menu (fn [evt]
                          (.preventDefault evt)
                          (.stopPropagation evt)
                          (let [x (.. evt -nativeEvent -pageX)
                                y (.. evt -nativeEvent -pageY)]
                            (re-frame/dispatch
                             [::events/show-context-menu
                              {:x x
                               :y y
                               :menu [(menues/remove-link-ctx-menu-option link)
                                      (menues/edit-link-text-ctx-menu-option link)]}])))}
   [rg/line-link link]])
