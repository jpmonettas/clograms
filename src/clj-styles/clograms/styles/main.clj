(ns clograms.styles.main)

(def color {:super-light-grey "#EEE"
            :light-grey "rgb(60,60,60)"
            :dark-grey "#2f2f2f"})

(def diagram
  [:.diagram-layer {:width "100%"
                    :height "100%"
                    :position :absolute
                    :background (color :light-grey)}
   ["> *" {:height "100%"}]
   [:.namespace-node {:background-color :orange}]
   [:.function-node {:background-color :red}]])

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
   [:input {:background-color (color :dark-grey)
            :color (color :super-light-grey)}]
   [:.rc-typeahead-suggestion
    {:background-color (color :dark-grey)
     :color (color :super-light-grey)}
    [:&.active {:background-color :green}]
    [:.namespace-name {:color :orange}]]])

(def side-bar
  [:.side-bar {:position :absolute
               :top "0px"
               :right "0px"
               :height "100%"
               :width "300px"
               :background-color (color :dark-grey)
               :z-index 10}])

;; This creates resources/public/css/main.css
(def ^:garden main
  (list
   diagram
   general
   entity-selector
   side-bar))
