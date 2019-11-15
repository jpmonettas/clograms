(ns clograms.styles.main
  (:require [garden.stylesheet :refer [at-media at-keyframes]]))

(def color {:super-light-grey "#EEE"
            :light-grey "rgb(60,60,60)"
            :dark-grey "#2f2f2f"
            :green "#98971a"
            :red "#cc241d"

            :code-background "#a89984"
            :selection "#665c54"
            :node-selection "#fb4934"
            :side-bar "#3c3836"
            :background "#504945"

            :project-node "#458588"
            :namespace-node "#b16286"
            :var-node "#689d6a"

            :main-font "#eee"
            })

(def border-radius "3px")

(def debug
  [[:.node-debug {:position :absolute
                    :top "-83px"
                    :font-size "10px"
                    :opacity 0.5
                  :z-index 10000}]
   [:.debug-bar {:position :absolute
                 :height "100px !important"
                 :bottom "-50px"
                 :left "10px"}]])

(def diagram
  [:.diagram-wrapper {:width "100%"
                      :height "100%"
                      :position :absolute
                      :background (color :background)}
   [:.diagram-layer {:width "100%"
                     :height "100%"
                     :position :absolute}
    ["> *" {:height "100%"}]
    [:.custom-node {:padding "5px"
                    :border-radius border-radius
                    :display :flex
                    :align-items :center}]
    [:.port {:display :inline-block
             :width "10px"
             :height "10px"
             :margin "5px"
             :border-radius border-radius
             :background-color :red
             :color :transparent}
     [:&:hover {:background-color (str "yellow !important")
                :cursor :crosshair}]]
    [:.node [:&.selected {:border "2px solid"
                          :border-color (color :node-selection)
                          :border-radius border-radius}]
     [:.node-wrapper {}
      [:.node-comment {:position :absolute
                       :opacity 0.5
                       :border-radius border-radius
                       :background-color (color :side-bar)
                       :margin-left "5%"
                       :width "90%"}]]]
    [:.node-body {:display :inline-block
                  :color (color :main-font)}
     [:.header {:display :flex
                :justify-content :space-between
                :margin-bottom "5px"}
      [:.title {:whitespace :nowrap}]
      [:.dispatch-val {:white-space :nowrap
                       :font-size "12px"}]
      [:.collapse-node {:display :inline-block
                        :cursor :pointer
                        :background (color :side-bar)
                        :padding "1px 10px 1px 10px"
                        :border-radius border-radius
                        :margin-left "10px"}]]]
    [:.project-node {:border (str "2px solid " (color :project-node))
                     :background-color (color :project-node)}
     [:.port {:background-color (color :project-node)}]]
    [:.namespace-node {:border  (str "2px solid " (color :namespace-node))
                       :background-color (color :namespace-node)}
     [:.port {:background-color (color :namespace-node)}]
     [:.namespace-doc {:max-width "500px"
                       :font-size "10px"
                       :max-height "200px"
                       :background-color (color :namespace-node)
                       :padding 0
                       :border :unset
                       :color (color :main-font)
                       :opacity 0.7}]]
    [:.var-node {:border (str "2px solid " (color :var-node))
                 :background-color (color :var-node)}
     [:.port {:background-color (color :var-node)}]
     [:.var-name {:font-weight :bold}]
     [:.source {:max-width "500px"
                :font-size "10px"
                :max-height "200px"
                :border :unset
                :background-color (color :main-font)}]]
    [:.re-frame-node {:border (str "2px solid " (color :var-node))}]]])

(def general
  [:body {:font-size "11px"
          :background-color (str (color :background) " !important")
          :color (str (color :main-font) " !important")}
   [:.app-wrapper
    [:&.loading {:opacity 0.5}]
    [:.context-menu {:background (color :side-bar)
                     :min-width "200px"
                     :border-radius border-radius
                     :overflow :hidden
                     :font-size "11px"}
     [:ul {:padding "10px"}
      [:li {:padding "4px"
            :cursor :pointer}
       [:&:hover {:background-color (color :selection)}]]]]
    [:ul {:list-style :none
          :padding 0}]
    [:.project-name {}]
    [:.namespace-name {}]
    [:.var-name {}]
    [:.draggable-entity
     {:padding "5px"
      :border-radius border-radius
      :margin "5px"
      :background-color (color :background)
      :font-size "11px"
      :cursor :no-drop}]
    [:.draggable-project {:border  (str "1px solid " (color :project-node))}
     [:&.main-project {:border-width "2px"}]]
    [:.draggable-namespace {:border (str "1px solid " (color :namespace-node))}]
    [:.draggable-var {:border (str "1px solid " (color :var-node))}
     [:.var {:display :inline-block
             :margin-right "4px"
             :width "7px"
             :height "7px"
             :border-radius border-radius}
      [:&.private {:background-color (color :red)}]
      [:&.public {:background-color (color :green)}]]
     [:.var-type {:margin-right "3px"}]]
    [:.draggable-re-frame-feature {:border (str "1px solid " (color :var-node))}]]])

(def top-bar
  [:.top-bar {:position :absolute
              :z-index 10
              :background-color (color :side-bar)
              :padding "5px"
              :border-radius border-radius
              :display :inline-flex}
   [:.save {:background-color (color :background)
            :height "32px"
            :margin-right "3px"
            :border-radius border-radius}]
   [:label {:margin-right "3px"}]
   [:.entity-selector {:display :inline-block}
    [:.type-ahead-wrapper {:display :inline-block}
     [:.project-name {:margin-left "3px"
                      :opacity 0.5}]
     [:input {:background-color (color :background)
              :color (color :super-light-grey)}]
     [:.rc-typeahead-suggestion
      {:background-color (color :side-bar)}
      [:&.active {:background-color (color :selection)}]]]]

   [:.color-selector {:display :inline-block
                      :margin 0}
    [:.selectable-color {:display :inline-block
                         :border-radius border-radius
                         :width "30px"
                         :margin "2px"
                         :height "30px"
                         :opacity 0.5}
     [:&.selected {:opacity 1
                   :border "1px solid orange"}]]]])

(def side-bar
  [:.side-bar {:position :absolute
               :top "0px"
               :right "0px"
               :height "100%"
               :width "350px"
               :background-color (color :side-bar)
               :z-index 10}
   [:.side-bar-tabs {}
    [:li {}
     [:a {:padding "5px"}]
     [:&.active {}
      [:a {:color (color :super-light-grey)
           :background-color (color :side-bar)}]]
     [:a {:color (color :super-light-grey)
          }]]]

   [:.projects-browser {:overflow-y :scroll
                        :height "95%"}
    [:.head-bar {}
     [:.back {:background-color (color :light-grey)
              :margin "5px"}]
     [:.browser-selection {:font-size "11px"
                           :padding "3px"
                           :border-radius border-radius}
      #_[:&.namespaces {:background-color (color :project-node)}]
      #_[:&.vars {:background-color (color :namespace-node)}]]]]

   [:.selected-browser {:overflow-y :scroll
                        :height "95%"}
    [:.header {:font-weight :bold
               :margin-left "5px"}]]])

(def loading-spinner
  [[:.loading-overlay {:width "100%"
                       :height "100%"
                       :position :absolute
                       :display :flex
                       :align-items :center
                       :justify-content :center
                       :z-index 1000}
    [:.spinner-outer {:width "104px"
                      :height "104px"
                      :z-index 5}
     [:.text {:z-index 10
              :position :relative
              :top "27px"
              :left "23px"}]
     [:.spinner-path
      {:stroke-dasharray 170
       :stroke-dashoffset 20}]
     [:.spinner-inner
      {:animation "rotate 1.4s linear infinite"
       :width "104px"
       :height "104px"
       :top "-36px"
       :position :relative}]]]
   (at-keyframes :rotate [:to {:transform "rotate(360deg)"}])])

(def accordion
  [:.accordion
   [:.item {:border "1px solid"
            :border-color (color :grey)
            :padding "2px"}
    [:.body {:display :none}]

    [:&.active {:margin-top "4px"
                :margin-bottom "4px"}
     [:.body {:display :block
              :max-height "500px"
              :overflow :scroll}]]
    ]])

(def tree
  [:.tree
   [:.childs {:margin-left "5px"}]

   [:&.re-frame-feature
    [:.namespace {:font-size "12px"}
     [:.project-name {:font-size "10px"
                      :opacity 0.7}]]]
   ])

;; This creates resources/public/css/main.css
(def ^:garden main
  (list
   diagram
   general
   top-bar
   side-bar
   loading-spinner
   debug

   ;; components
   accordion
   tree
   ))
