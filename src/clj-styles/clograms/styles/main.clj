(ns clograms.styles.main
  (:require [garden.stylesheet :refer [at-media at-keyframes]]))

(def color {:super-light-grey "#EEE"
            :light-grey "rgb(60,60,60)"
            :dark-grey "#2f2f2f"
            :green "greenyellow"
            :red "#cc241d"

            :code-background "#a89984"
            :selection "#5076B2"
            :node-selection "#fb4934"
            :tool-bars "#3c3836"
            :background "#504945"

            :project-node "#689d6a"
            :namespace-node "#b16286"
            :var-node "#458588"
            :re-frame "#896dad"
            :spec-node "#c17055"
            :main-font "#eee"
            :shape "#336699"
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

(def port-fix "-10px")

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
                    :align-items :center
                    :border "1px solid black"
                    }]
    [:.node [:&.selected [:.port-inner {:background-color :yellow}]]
     [:&:hover [:.port-inner
                {:background-color (str "yellow !important")}]]

     [:.div-port {:display :flex
                  :justify-content :center
                  :align-items :center
                  :width "15px"
                  :height "15px"
                  :border-radius border-radius
                  :color :transparent}
      [:&:hover {:cursor :crosshair}]
      [:&.port-0 {:position :absolute
                  :top port-fix :left port-fix}]
      [:&.port-1 {:position :absolute
                  :top port-fix :left "50%"}]
      [:&.port-2 {:position :absolute
                  :top port-fix :right port-fix}]
      [:&.port-3 {:position :absolute
                  :top "50%" :right port-fix}]
      [:&.port-4 {:position :absolute
                  :bottom port-fix :right port-fix}]
      [:&.port-5 {:position :absolute
                  :bottom port-fix :left "50%"}]
      [:&.port-6 {:position :absolute
                  :bottom port-fix :left port-fix}]
      [:&.port-7 {:position :absolute
                  :top "50%" :left port-fix}]
      [:.port-inner {:width "5px"
                     :height "5px"}]]

     [:.node-wrapper {}


      [:.node-comment {:position :absolute
                       :opacity 0.5
                       :border-radius border-radius
                       :color (color :main-font)
                       :font-family "monospace"
                       :font-size "0.8em"
                       :background-color (color :tool-bars)
                       :margin-left "5%"
                       :width "90%"}]]]
    [:.node-body {:display :inline-block
                  :color (color :main-font)
                  :max-width "500px"}
     [:.source {:font-size "10px"
                :max-height "200px"
                :border :unset
                :background-color (color :main-font)}]
     [:.spec-source {:max-width "500px"
                     :font-size "9px"
                     :opacity 0.7
                     :max-height "100px"
                     :border :unset
                     :background-color (color :main-font)}]
     [:.args-vec {:white-space :nowrap
                  :overflow-x :auto}]
     [:.header {:white-space :nowrap}
      [:.title {:white-space :nowrap}]
      [:.dispatch-val {:display :inline-block
                       :white-space :nowrap
                       :font-size "12px"}]]]
    [:.project-node {:background-color (color :project-node)}
     [:.project-version {:text-align :center
                         :border-top "1px solid"}]
     [:.port {:background-color (color :project-node)}]]
    [:.namespace-node {:background-color (color :namespace-node)}
     [:.port {:background-color (color :namespace-node)}]
     [:.namespace-doc {:max-width "500px"
                       :font-size "10px"
                       :max-height "200px"
                       :background-color (color :namespace-node)
                       :padding 0
                       :border :unset
                       :color (color :main-font)
                       :opacity 0.7}]]
    [:.var-node {:background-color (color :var-node)}
     [:.port {:background-color (color :var-node)}]
     [:.var-name {:font-weight :bold}]]
    [:.re-frame-node {:background-color (color :re-frame)}
     [:.key {:font-weight :bold
             :margin-right "5px"}]]
    [:.spec-node {:background-color (color :spec-node)}]]])

(def svg-shapes
  [:svg
   [:.resizer {:cursor :nwse-resize
               :stroke :transparent
               :fill :transparent}]
   [:.link
    [:&:hover [:line {:stroke :red}]]]
   [:.svg-node
    [:&.selected [:.svg-port {:fill "yellow"}]]
    [:&:hover [:.svg-port {:fill "yellow"}]]
    [:.svg-port {:z-index 100
                 :fill :transparent}
     [:&:hover {:cursor :crosshair}]]]
   [:g.custom-node
    {:stroke :black
     :fill (color :shape)}
    [:text {:stroke :none :fill (color :main-font)}]
    [:&.group-shape
     [:rect {:stroke :grey
             :stroke-width 4
             :fill :transparent}]]]])

(def general
  [:body {:background-color (str (color :background) " !important")
          :color (str (color :main-font) " !important")}
   [:.app-wrapper {:font-size "12px"
                   :font-family "droid_sansregular"}
    [:&.loading {:opacity 0.5}]
    [:.collapse-button {:display :inline-block
                        :cursor :pointer
                        :font-size "16px"
                        :padding "3px"}]
    [:.tool-bar {:background-color (color :tool-bars)
                 :border-color "#777"}]
    [:input {:background-color "#777"
             :color (color :super-light-grey)
             :border :none
             :padding-left "2px"
             :height "23px"
             :border-radius border-radius}]
    [:.context-menu {:background (color :tool-bars)
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
    [:.project-name {:whitespace :nowrap}]
    [:.namespace-name {}]
    [:.var-name {}]
    [:.draggable-shape {:display :inline-block
                        :margin "5px"}]
    [:.draggable-entity
     {:padding "4px"
      :border-radius border-radius
      :margin "5px"
      :background-color (color :background)
      :font-size "11px"
      :cursor :grab}]
    [:.draggable-project {:background-color (color :project-node)}
     [:&.main-project {:border-width "2px"}]
     [:.project-version {:margin-left "5px"
                         :opacity 0.7}]]
    [:.draggable-namespace {:background-color (color :namespace-node)}]
    [:.draggable-var {:background-color (color :var-node)
                      }
     [:.var {:display :inline-block
             :margin-right "4px"
             :width "7px"
             :height "7px"
             :border-radius border-radius}
      [:&.private {:background-color (color :red)}]
      [:&.public {:background-color (color :green)}]]
     [:.var-type {:margin-right "3px"}]]
    [:.draggable-re-frame-feature {:background-color (color :re-frame)}]
    [:.draggable-spec {:background-color (color :spec-node)}]]])

(def top-bar
  [:.top-bar {:position :absolute
              :z-index 10
              :padding "5px"
              :height "37px"
              :border-radius border-radius
              :display :inline-flex}
   [:.save {:background-color (color :background)
            :border-radius border-radius
            :font-size "18px"
            :padding "0px 4px 0 4px"
            :height "23px"}]
   [:label {:margin-right "3px"}]
   [:.entity-selector {:display :inline-block
                       :margin-left "3px"
                       :margin-right "3px"}
    [:.type-ahead-wrapper {:display :inline-block}
     [:.search-icon {:background-color (color :background)
                     :border-radius "3px 0px 0px 3px"
                     :padding "4px"
                     :font-size "15px"}]
     [:input {:border-radius "0px 3px 3px 0px"}]

     [:.entity-type {:margin-left "5px"}]
     [:.project-name {:margin-left "3px"}]
     [:.selector-option
      [:&.var
       [:.var-name {:font-weight :bold}]
       [:.entity-type {:color (color :var-node)}]
       [:.project-name {:opacity 0.5}]
       [:.namespace-name {:opacity 0.5}]]
      [:&.namespace
       [:.namespace-name {:font-weight :bold}]
       [:.entity-type {:color (color :namespace-node)}]
       [:.project-name {:opacity 0.5}]]
      [:&.project [:.entity-type {:color (color :project-node)}]
       [:.project-name {:font-weight :bold}]]
      [:&.spec [:.entity-type {:color (color :spec-node)}]
       [:.spec-key {:font-weight :bold}]]]
     [:.rc-typeahead {:display :inline-block}
      [:.rc-typeahead-suggestion
       {:background-color (color :tool-bars)}
       [:&.active {:background-color (color :selection)}]]]]]

   [:.color-selector {:display :inline-block
                      :border-radius border-radius
                      :height "23px"
                      :margin 0
                      :overflow :hidden}
    [:.selectable-color {:display :inline-block
                         :width "30px"
                         :height "100%"}
     [:&.selected {:border "1px solid orange"}]]]

   [:.link-arrows-selector {:overflow :hidden
                            :border-radius border-radius
                            :height "23px"
                            :margin-left "5px"
                            :background-color (color :background)}
    [:.button {:padding "5px"
               :margin-top "3px"}
     [:&.selected {:background-color (color :selection)}]
     [:i {:font-size "15px"
          :margin-top "5px"}]]]])

(def side-bar
  [:.side-bar {:position :absolute
               :top "0px"
               :right "0px"
               :height "100%"
               :width "350px"
               :z-index 10
               :padding "5px"}
   [:.side-bar-tabs {}
    [:li {}
     [:a {:padding "5px"}]
     [:&.active {}
      [:a {:color (color :super-light-grey)
           :background-color (color :tool-bars)}]]
     [:a {:color (color :super-light-grey)
          }]]]

   [:.projects-browser {:overflow-y :scroll
                        :height "95%"}
    [:.head-bar {:background-color (color :background)
                 :padding "2px"
                 :border-radius border-radius}
     [:.back {:background-color (color :background)
              :margin "5px"
              :font-size "14px"}]
     [:.browser-selection {:font-size "11px"
                           :padding "3px"
                           :border-radius border-radius}]]]

   [:.selected-browser {:overflow-y :scroll
                        :height "95%"}
    [:.header {:font-weight :bold
               :margin-left "5px"}]]])

(def overlay-styles {:width "100%"
                     :height "100%"
                     :position :absolute
                     :display :flex
                     :align-items :center
                     :justify-content :center
                     :z-index 1000})

(def loading-spinner
  [[:.loading-overlay overlay-styles
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

(def text-edit-modal
  [:.modal-overlay overlay-styles
   [:.text-edit-modal {:padding "15px"
                       :background-color (color :tool-bars)}
    [:input {:width "200px"}]]])

(def bottom-bar
  [:.bottom-bar {:position :absolute
                 :bottom "0px"
                 :width "74%"
                 :z-index 10}
   [:.header {:padding "5px"
              :border-bottom "1px solid #777"}
    [:.title {:width "95%"
              :display :inline-block}]
    [:.collapse-button {:background-color (color :background)}]]
   [:.body {:max-height "300px"
            :overflow-y :scroll}
    [:&.collapsed {:display :none}]
    [:.references {:padding "5px"}
     [:li
      [:.var-name {:font-weight :bold}]
      [:.project-name {:margin-left "3px"
                       :opacity 0.5}]
      [:&.odd ]
      [:&.even {:background-color "#403b39"}]]]]])

(def accordion
  [:.accordion
   [:.item {:border-radius border-radius
            :border-color "#777"
            :padding "2px"
            :cursor :pointer}
    [:.body {:display :none}]

    [:&.active {:margin-top "7px"
                :margin-bottom "7px"
                :background-color (color :background)
                :border-radius border-radius}
     [:.body {:display :block
              :max-height "500px"
              :overflow-y :scroll}]]
    ]])

(def tree
  [:.tree
   [:.childs {:margin-left "5px"}]

   [:&.re-frame-feature
    [:.namespace {:font-size "12px"}
     [:.project-name {:font-size "10px"
                      :opacity 0.7}]]]
   ])

(def print
  (at-media {:print true}
            [[:.bottom-bar {:display :none}]
             [:.top-bar    {:display :none}
              [:i {:display :none}]]
             [:.side-bar   {:display :none}]]))
;; This creates resources/public/css/main.css
(def ^:garden main
  (list
   print
   diagram
   svg-shapes
   general
   top-bar
   side-bar
   bottom-bar
   loading-spinner
   text-edit-modal
   debug

   ;; components
   accordion
   tree
   ))
