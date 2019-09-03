(ns clograms.reagent-diagrams
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]
            [goog.string :as gstring]
            [clojure.string :as str]
            [clojure.core.matrix :as matrix]))

(def identity-transform [[1 0 0]
                         [0 1 0]
                         [0 0 1]])

(defonce diagram-atom
  (r/atom {:nodes {"nid1" {::id "nid1"
                            :diagram.node/type :custom-node-1
                            :selected true
                            :x 20
                            :y 20
                            :ports {"pid1" {::id "pid1"
                                            :diagram.port/type :custom-port1}
                                    "pid2" {::id "pid2"
                                            :diagram.port/type :custom-port1}}}
                    "nid2" {::id "nid2"
                            :diagram.node/type :custom-node-2
                            :selected false
                            :x 510
                            :y 510
                            :ports {"pid3" {::id "pid3"
                                            :diagram.port/type :custom-port1}}}
                   }
           ;; :links {"lid1" {::id "lid1"
           ;;                 :from-port ["nid1" "pid2"]
           ;;                 :to-port ["nid2" "pid3"]}}

           :scale 1
           :scale-origin [0 0]
           :translate [0 0]
           ;; :grab {:grab-origin [0 0]
           ;;        :grab-object {:diagram.object/type :node
           ;;                      ::id "nid1"
           ;;                      :start-pos [100 100]}}
           }))

(defonce node-components (atom {}))

(defn grab [d grab-obj grab-origin]
  (assoc d :grab {:grab-origin grab-origin
                  :grab-object grab-obj}))

(defn grab-release [d]
  (dissoc d :grab))

(defn add-node [d node-type {:keys [x y] :or {x 500 y 500} :as data}]
  (let [node-id (str (random-uuid))]
    (update d :nodes assoc node-id (merge {::id node-id
                                           :diagram.node/type node-type}
                                          data))))

(defn add-node-port [d node-id port-type]
  (let [port-id (str (random-uuid))]
    (update-in d [:nodes node-id :ports] assoc port-id {::id port-id
                                                        :diagram.port/type port-type})))

(defn add-link [d [from-node from-port :as from] [to-node to-port :as to]]

  (let [link-id (str (random-uuid))]
    (update d :links assoc link-id {::id link-id
                                    :from-port from
                                    :to-port to})))

;;;;;;;;;;
;; Port ;;
;;;;;;;;;;

(defn default-port [props]
  [:div {:style {:width "10px"
                 :height "10px"
                 :background :green}}])

(defn set-port-dimensions [d node-id port-id dims]
  (update-in d [:nodes node-id :ports port-id] merge dims))

(defn port [node p]
  (let [node-id (::id node)
        port-component (get @node-components (:diagram.port/type p) default-port)
        update-after-render (fn [p-cmp]
                              (let [dn (r/dom-node p-cmp)
                                    brect (.getBoundingClientRect dn)]
                                (swap! diagram-atom set-port-dimensions node-id (::id p) {:w (.-width brect)
                                                                                          :h (.-height brect)
                                                                                          :x (.-x brect)
                                                                                          :y (.-y brect)})))]
    (r/create-class
     {:component-did-mount (fn [this] (update-after-render this))
      :component-did-update (fn [this] (update-after-render this))
      :reagent-render
      (fn [node p]
        [:div.port {:on-mouse-down (fn [evt]
                                     (.stopPropagation evt)
                                     (swap! diagram-atom add-link [node-id (::id p)] nil)
                                     (swap! diagram-atom grab
                                            {:diagram.object/type :link
                                             :link-origin-port [node-id (::id p)]}
                                            [(.-clientX evt) (.-clientY evt)]))
                    :on-mouse-up (fn [evt]
                                   (swap! diagram-atom (fn [d]
                                                         (add-link d [node-id (::id p)] (get-in d [:grab :grab-object :link-origin-port]))) ))}

         [port-component p]])})))

;;;;;;;;;;
;; Node ;;
;;;;;;;;;;

(defn default-node [props]
  [:div {:style {:max-width 200
                 :overflow :hidden
                 :background :red}}
   (str "Node 1 type" props)])

(defn set-node-dimensions [d node-id dims]
  (update-in d [:nodes node-id] merge dims))

(defn node [n]
  (let [node-component (get @node-components (:diagram.node/type n) default-node)
        update-after-render (fn [n-cmp]
                              (let [dn (r/dom-node n-cmp)
                                    brect (.getBoundingClientRect dn)]
                                (swap! diagram-atom set-node-dimensions (::id n) {:w (.-width brect)
                                                                                  :h (.-height brect)
                                                                                  :x (.-x brect)
                                                                                  :y (.-y brect)})))]
    (r/create-class
     {:component-did-mount (fn [this] (update-after-render this))
      :component-did-update (fn [this]
                              (update-after-render this))
      :reagent-render
      (fn [n]
        [:div.node {:on-mouse-down (fn [evt]
                                     (.stopPropagation evt)
                                     (swap! diagram-atom (fn [d]
                                                           (grab d
                                                                 (assoc (select-keys n [::id :x :y])
                                                                        :diagram.object/type :node)
                                                                 [(.-clientX evt) (.-clientY evt)]))))
                    :style {:position :absolute
                            :top (:y n)
                            :left (:x n)}}
         [:div.ports {:style {:display :flex
                              :justify-content :space-between}}
          (for [p (vals (:ports n))]
            ^{:key (::id p)}
            [port n p])]
         [node-component n]])})))

(defn drag [d mouse-x mouse-y]
  (if-let [{:keys [grab-origin grab-object]} (:grab @diagram-atom)]
    (let [[grab-start-x grab-start-y] grab-origin
          d' (assoc-in d [:grab :grab-current] [mouse-x mouse-y])]
      (case (:diagram.object/type grab-object)
        :node (let [{obj-start-x :x obj-start-y :y} grab-object
                    obj-end-x (+ obj-start-x (- mouse-x grab-start-x))
                    obj-end-y (+ obj-start-y (- mouse-y grab-start-y))]
                (-> d'
                    (assoc-in [:nodes (::id grab-object) :x] obj-end-x)
                    (assoc-in [:nodes (::id grab-object) :y] obj-end-y)))
        :diagram (let [[start-translate-x start-translate-y] (:start-translate grab-object)]
                   (assoc d' :translate [(+ start-translate-x (- mouse-x grab-start-x))
                                         (+ start-translate-y (- mouse-y grab-start-y))]))
        :link d'))
    d))


(defn zoom [d delta center-x center-y]
  (let [zoom-dir (if (pos? delta) 1 -1)]
    (-> d
        (update :scale #(+ % (* zoom-dir 0.08)))
        (assoc :scale-origin [center-x center-y]))))

(defn link-curve-string [[[fpx fpy] & points]]
  (gstring/format "M%f %f C%s" fpx fpy (str/join "," (map #(str/join " " %) points))))


(defn link [{:keys [nodes grab]} {:keys [from-port to-port]}]
  (let [[from-n from-p] from-port
        [to-n to-p] to-port
        x1 (:x (get-in nodes [from-n :ports from-p]))
        y1 (:y (get-in nodes [from-n :ports from-p]))
        x2 (or (:x (get-in nodes [to-n :ports to-p]))
               (get-in grab [:grab-current 0]))
        y2 (or (:y (get-in nodes [to-n :ports to-p]))
               (get-in grab [:grab-current 1]))]
    (when (and x1 x2) ;; so it doesn't fail when we still don't have port coordinates (waiting for render)
      [:g
      [:path {:stroke :gray
              :fill :none
              :stroke-width 3
              :d (link-curve-string [[x1 y1] [(+ 50 x1) y1] [(- x2 50) y2] [x2 y2]])}]])))

(defn clean-unfinished-links [d]
  (update d :links #(->> %
                        (remove (fn [[id {:keys [from-port to-port]}]]
                                  (nil? to-port)))
                        (into {}))))

(defn diagram []
  (let [{:keys [nodes links translate scale scale-origin] :as d} @diagram-atom]
    [:div.diagram-layer {:style {:overflow :hidden}
                         :on-mouse-down (fn [evt]
                                          (swap! diagram-atom grab {:diagram.object/type :diagram :start-translate translate} [(.-clientX evt) (.-clientY evt)]))
                         :on-wheel (fn [evt] (swap! diagram-atom zoom (.-deltaY evt) (.-clientX evt) (.-clientY evt)))
                         :on-mouse-move (fn [evt]
                                          (swap! diagram-atom drag (.-clientX evt) (.-clientY evt)))
                         :on-mouse-up (fn [evt]
                                        (swap! diagram-atom grab-release)
                                        (swap! diagram-atom clean-unfinished-links))
                         }
     [:div.zoom-div {:style {:transform (gstring/format "scale(%f)" scale)
                             :transform-origin (gstring/format "%dpx %dpx" (first scale-origin) (second scale-origin))}}
      [:div.translate-div {:style {:transform (gstring/format "translate(%dpx,%dpx)" (first translate) (second translate))}}
       [:div.nodes-and-links-wrapper
        [:svg.links {:style {:overflow :visible}}
         (for [l (vals links)]
           ^{:key (::id l)}
           [link d l])]
        [:div.nodes
         (for [n (vals nodes)]
           ^{:key (::id n)}
           [node n])]]]]]))



(defn register-node-component! [node-type component-fn]
  (swap! node-components assoc node-type component-fn))
