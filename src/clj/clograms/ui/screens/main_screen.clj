(ns clograms.ui.screens.main-screen
  (:require [clograms.ui.fx-grams :as fx-grams]
            [cljfx.api :as fx]
            [clojure.string :as str]
            [clograms.ui.events :as ui-events]))

(defn root-view [{:keys [fx/context]}]
  (let [{:keys [nodes links link-config grab translate scale scale-origin] :as a} (fx/sub context ::diagram)
        [tx ty] translate
        ;; div-nodes (filter (fn [n] (contains? (div-nodes-components) (:diagram.node/type n))) (vals nodes))
        ;; the order in which we render the nodes matters since nodes rendered after
        ;; ocludes nodes rendered first
        ;; svg-nodes (->> (vals nodes)
        ;;                (filter (fn [n] (contains? (svg-nodes-components) (:diagram.node/type n))))
        ;;                (sort-by :diagram.node/type svg-nodes-comparator))
        ]
    {:fx/type :stage
     :showing true
     :width 1000
     :height 1000
     :scene {:fx/type :scene
             :root {:fx/type :border-pane
                    :top {:fx/type :pane :style {:-fx-background-color :red} :pref-width 100 :pref-height 100}
                    :left {:fx/type :pane :style {:-fx-background-color :green} :pref-width 100 :pref-height 100}
                    :right {:fx/type :pane :style {:-fx-background-color :blue} :pref-width 100 :pref-height 100}
                    :bottom {:fx/type :pane :style {:-fx-background-color :purple} :pref-width 100 :pref-height 100}
                    :center {:fx/type fx-grams/diagram-view}
                    }}}))

(defmethod ui-events/handle-event ::init [{:keys [fx/context]}]
  {:context (fx/swap-context context (constantly (-> (clojure.edn/read-string (slurp "./diagram.edn"))
                                                     (assoc-in [::fx-grams/diagram :translate] [0 0])
                                                     (assoc-in [::fx-grams/diagram :scale] 1))))})

(comment

  (do
    (def *context (atom (fx/create-context {})))

    (def app (fx/create-app *context
                            :event-handler ui-events/handle-event
                            :desc-fn (fn [_]
                                       {:fx/type root-view})))
    (alter-var-root #'ui-events/dispatch-event (constantly (:handler app)))

    (ui-events/dispatch-event {:event/type ::init})
    )




  ((:renderer app))
  )
