(ns clograms.styles.main)

(def color {:super-light-grey "#EEE"
            :light-grey "rgb(60,60,60)"
            :dark-grey "#2f2f2f"

            :selection "#665c54"
            :side-bar "#3c3836"
            :background "#504945"
            :project-node "#689d6a"
            :namespace-node "#b16286"
            :var-node "#458588"})

(def diagram
  [:.diagram-layer {:width "100%"
                    :height "100%"
                    :position :absolute
                    :background (color :background)}
   ["> *" {:height "100%"}]
   [:.custom-node {:padding "5px"
                   :border-radius "5px"}]
   [:.project-node {:background-color (color :project-node)}]
   [:.namespace-node {:background-color (color :namespace-node)}]
   [:.var-node {:background-color (color :var-node)}
    [:.var-name {:font-weight :bold}]
    [:.source {:max-width "500px"
               :font-size "10px"
               :max-height "200px"}]]])

(def general
  [:body {:font-size "11px"
          :color (str (color :super-light-grey) " !important")}
   [:ul {:list-style :none
         :padding 0}]
   [:.project-name {:opacity 0.5
                    :margin-left "5px"}]
   [:.namespace-name {}]])

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
                           :border-radius "5px"}
      [:&.namespaces {:background-color (color :project-node)}]
      [:&.vars {:background-color (color :namespace-node)}]]]
    [:.draggable-entity
     {:padding "5px"
      :border-radius "5px"
      :margin "5px"}]
    [:.draggable-project {:background-color (color :project-node)}]
    [:.draggable-namespace {:background-color (color :namespace-node)}]
    [:.draggable-var {:background-color (color :var-node)}]]])

;; This creates resources/public/css/main.css
(def ^:garden main
  (list
   diagram
   general
   entity-selector
   side-bar))
