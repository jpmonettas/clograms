(ns clograms.styles.main)

(def color {:super-light-grey "#EEE"
            :light-grey "rgb(60,60,60)"
            :dark-grey "#2f2f2f"

            :code-background "#a89984"
            :selection "#665c54"
            :side-bar "#3c3836"
            :background "#504945"
            :project-node "#689d6a"
            :namespace-node "#b16286"
            :var-node "#458588"
            :main-font "#eee"})

(def border-radius "3px")

(def diagram
  [:.diagram-layer {:width "100%"
                    :height "100%"
                    :position :absolute
                    :background (color :background)}
   ["> *" {:height "100%"}]
   [:.custom-node {:padding "5px"
                   :border-radius border-radius}]
   [:.project-node {:border  (str "2px solid " (color :project-node))}]
   [:.namespace-node {:border  (str "2px solid " (color :namespace-node))}]
   [:.var-node {:border (str "2px solid " (color :var-node))}
    [:.var-name {:font-weight :bold}]
    [:.source {:max-width "500px"
               :font-size "10px"
               :max-height "200px"
               :background-color (color :main-font)}]]])

(def general
  [:body {:font-size "11px"
          :color (str (color :main-font) " !important")}
   [:ul {:list-style :none
         :padding 0}]
   [:.project-name {:opacity 0.5
                    :margin-left "5px"}]
   [:.namespace-name {}]
   [:.draggable-entity
    {:padding "5px"
     :border-radius border-radius
     :margin "5px"
     :font-size "11px"}]
   [:.draggable-project {:border  (str "1px solid " (color :project-node))}]
   [:.draggable-namespace {:border  (str "1px solid " (color :namespace-node))}]
   [:.draggable-var {:border  (str "1px solid " (color :var-node))}]])

(def entity-selector
  [:.entity-selector {:position :absolute
                      :z-index 10
                      :left "50%"
                      :top "3%"
                      :margin-left "-300px"}
   [:input {:background-color (color :side-bar)
            :color (color :super-light-grey)}]
   [:.rc-typeahead-suggestion
    {:background-color (color :side-bar)}
    [:&.active {:background-color (color :selection)}]
    [:.namespace-name {:color "#d3869b"}]]])

(def side-bar
  [:.side-bar {:position :absolute
               :top "0px"
               :right "0px"
               :height "100%"
               :width "300px"
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
      [:&.namespaces {:background-color (color :project-node)}]
      [:&.vars {:background-color (color :namespace-node)}]]]]

   [:.selected-browser {:overflow-y :scroll
                        :height "95%"}
    [:.header {:font-weight :bold
               :margin-left "5px"}]]])

;; This creates resources/public/css/main.css
(def ^:garden main
  (list
   diagram
   general
   entity-selector
   side-bar))
