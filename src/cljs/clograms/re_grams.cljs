(ns clograms.re-grams
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]
            [goog.string :as gstring]
            [clojure.string :as str]
            [clojure.core.matrix :as matrix]
            [re-frame.core :refer [dispatch reg-event-db reg-sub subscribe]]))

#_(defonce diagram-atom
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
           :grab {:cli-origin [0 0]
                  :cli-current [10 10]
                  :grab-object {:diagram.object/type :node
                                ::id "nid1"
                                :start-pos [100 100]}}
           }))

(defn initial-db []
  {::diagram {:nodes {
                      "nid1" {::id "nid1"
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
                      "nid3" {::id "nid3"
                              :diagram.node/type :custom-node-1
                              :selected false
                              :x 710
                              :y 710
                              :ports {"pid4" {::id "pid4"
                                              :diagram.port/type :custom-port1}}}
                      }
              :scale 1
              :scale-origin [0 0]
              :translate [0 0]}})

(defonce node-components (atom {}))

(defn client-coord->dia-coord [{:keys [translate scale]} [client-x client-y]]
  (let [[tx ty] translate]
    [(/ (- client-x tx) scale)
     (/ (- client-y ty) scale)]))

(defn dia-coord->client-coord [{:keys [translate scale]} [dia-x dia-y]]
  (let [[tx ty] translate]
    [(* (+ dia-x tx) scale)
     (* (+ dia-y ty) scale)]))

(defn grab [db grab-obj client-grab-origin]
  (assoc-in db [::diagram :grab] {:cli-origin client-grab-origin
                                  :grab-object grab-obj}))

(defn grab-release [db]
  (assoc-in db [::diagram :grab] nil))

(defn add-node [db node-type {:keys [x y] :or {x 500 y 500} :as data}]
  (let [node-id (str (random-uuid))]
    (update-in db [::diagram :nodes] assoc node-id (merge {::id node-id
                                                           :diagram.node/type node-type}
                                                          data))))

(defn add-node-port [db node-id port-type]
  (let [port-id (str (random-uuid))]
    (update-in db [::diagram :nodes node-id :ports] assoc port-id {::id port-id
                                                                   :diagram.port/type port-type})))

(defn add-link [db [from-node from-port :as from] [to-node to-port :as to]]
  (let [link-id (str (random-uuid))]
    (update-in db [::diagram :links] assoc link-id {::id link-id
                                                   :from-port from
                                                    :to-port to})))

;;;;;;;;;;
;; Port ;;
;;;;;;;;;;

(defn default-port [props]
  [:div {:style {:width "10px"
                 :height "10px"
                 :background :green}}])

(defn set-port-dimensions [db node-id port-id {:keys [w h client-x client-y]}]
  (let [[x y] (client-coord->dia-coord (::diagram db) [client-x client-y])
        scale (get-in db [::diagram :scale])]
    (update-in db [::diagram :nodes node-id :ports port-id]
              (fn [dims]
                (assoc dims
                       :w (/ w scale)
                       :h (/ h scale)
                       :x x
                       :y y)))))

(defn port [node p]
  (let [grab-sub (subscribe [::grab])
        node-id (::id node)
        port-component (get @node-components (:diagram.port/type p) default-port)
        update-after-render (fn [p-cmp]
                              (let [dn (r/dom-node p-cmp)
                                    brect (.getBoundingClientRect dn)]
                                (println "PORT UPDATED")
                                (dispatch [::set-port-dimensions node-id (::id p) {:w (.-width brect)
                                                                                   :h (.-height brect)
                                                                                   :client-x (.-x brect)
                                                                                   :client-y (.-y brect)}])))]
    (r/create-class
     {:component-did-mount (fn [this] (update-after-render this))
      :component-did-update (fn [this] (update-after-render this))
      :reagent-render
      (fn [node p]
        (println "Re painting port")
        [:div.port {:on-mouse-down (fn [evt]
                                     (.stopPropagation evt)
                                     (.preventDefault evt)
                                     (dispatch [::grab {:diagram.object/type :link
                                                        :tmp-link-from [node-id (::id p)]} [(.-clientX evt) (.-clientY evt)]]))
                    :on-mouse-up (fn [evt]
                                   (dispatch [::add-link [node-id (::id p)] (get-in @grab-sub [:grab-object :tmp-link-from])]))}

         [port-component p]])})))

;;;;;;;;;;
;; Node ;;
;;;;;;;;;;

(defn default-node [props]
  [:div {:style {:max-width 200
                 :overflow :hidden
                 :background :red}}
   (str "Node 1 type" props)])

(defn set-node-dimensions [db node-id {:keys [w h]}]
  (let [scale (get-in db [::diagram :scale])]
    (update-in db [::diagram :nodes node-id]
               (fn [dims]
                 (assoc dims
                        :w (/ w scale)
                        :h (/ h scale))))))

(defn node [n]
  (let [node-component (get @node-components (:diagram.node/type n) default-node)
        update-after-render (fn [n-cmp]
                              (let [dn (r/dom-node n-cmp)
                                    brect (.getBoundingClientRect dn)]
                                (dispatch [::set-node-dimensions (::id n) {:w (.-width brect)
                                                                           :h (.-height brect)}])))]
    (r/create-class
     {:component-did-mount (fn [this] (update-after-render this))
      :component-did-update (fn [this] (update-after-render this))
      :reagent-render
      (fn [n]
        (println "Re painting node")
        [:div.node {:on-mouse-down (fn [evt]
                                     (.stopPropagation evt)
                                     (.preventDefault evt)
                                     (dispatch [::grab {:diagram.object/type :node
                                                        ::id (::id n)
                                                        :start-pos [(:x n) (:y n)]}
                                                [(.-clientX evt) (.-clientY evt)]]))
                    :style {:position :absolute
                            :top (:y n)
                            :left (:x n)}}
         [:div.ports {:style {:display :flex
                              :justify-content :space-between}}
          (for [p (vals (:ports n))]
            ^{:key (::id p)}
            [port n p])]
         [node-component n]])})))

(defn drag [db current-cli-coord]
  (if-let [{:keys [cli-origin grab-object]} (get-in db [::diagram :grab])]
    (let [[cli-x cli-y] current-cli-coord
          [grab-start-x grab-start-y] cli-origin
          [obj-start-x obj-start-y] (:start-pos grab-object)
          db' (assoc-in db [::diagram :grab :cli-current] current-cli-coord)]
      (case (:diagram.object/type grab-object)
        :node (let [[current-dia-x current-dia-y] (client-coord->dia-coord (::diagram db) current-cli-coord)
                    [start-dia-x start-dia-y] (client-coord->dia-coord (::diagram db) cli-origin)
                    drag-x (- current-dia-x start-dia-x)
                    drag-y (- current-dia-y start-dia-y)
                    obj-end-x (+ obj-start-x drag-x)
                    obj-end-y (+ obj-start-y drag-y)]
                (-> db'
                    (assoc-in [::diagram :nodes (::id grab-object) :x] obj-end-x)
                    (assoc-in [::diagram :nodes (::id grab-object) :y] obj-end-y)))
        :diagram (let [drag-x (- cli-x grab-start-x)
                       drag-y (- cli-y grab-start-y)
                       obj-end-x (+ obj-start-x drag-x)
                       obj-end-y (+ obj-start-y drag-y)]
                   (-> db'
                       (assoc-in [::diagram :translate] [obj-end-x obj-end-y])))
        :link db'))
    db))


(defn zoom [db delta [client-center-x client-center-y :as cli-coords]]
  (let [zoom-dir (if (pos? delta) 1 -1)
        [dia-x dia-y] (client-coord->dia-coord (::diagram db) cli-coords)]
    (update db ::diagram
            (fn [{:keys [translate scale] :as dia}]
              (let [new-scale (+ scale (* zoom-dir 0.08))
                    scaled-dia-x (* dia-x scale)
                    scaled-dia-y (* dia-y scale)
                    new-scaled-dia-x (* dia-x new-scale)
                    new-scaled-dia-y (* dia-y new-scale)
                    x-scale-diff (- scaled-dia-x new-scaled-dia-x)
                    y-scale-diff (- scaled-dia-y new-scaled-dia-y)
                    [tx ty] translate
                    new-translate-x (+ tx x-scale-diff)
                    new-translate-y (+ ty x-scale-diff)]
                (assoc dia
                       :translate [new-translate-x new-translate-y]
                       :scale new-scale))))))

(defn link-curve-string [[[fpx fpy] & points]]
  (gstring/format "M%f %f C%s" fpx fpy (str/join "," (map #(str/join " " %) points))))

(defn translate [db to]
  (assoc-in db [::diagram :translate] to))

(defn link [nodes {:keys [from-port to-port]  :as l}]
  (let [[from-n from-p] from-port
        [to-n to-p] to-port

        x1 (:x (get-in nodes [from-n :ports from-p]))
        y1 (:y (get-in nodes [from-n :ports from-p]))
        x2 (or (:x (get-in nodes [to-n :ports to-p]))
               (:to-x l))
        y2 (or (:y (get-in nodes [to-n :ports to-p]))
               (:to-y l))]
    (when (and (pos? x1) (pos? x2)) ;; so it doesn't fail when we still don't have port coordinates (waiting for render)
      [:g
       [:path {:stroke :gray
               :fill :none
               :stroke-width 3
               :d (link-curve-string [[x1 y1] [(+ 50 x1) y1] [(- x2 50) y2] [x2 y2]])}]])))

(defn diagram [dia]
  (r/create-class
   {:component-did-mount (fn [this])
    :reagent-render (fn [dia]
                      (let [{:keys [nodes links grab translate scale scale-origin]} dia
                            [tx ty] translate]
                        [:div.diagram-layer {:style {:overflow :hidden}
                                             :on-mouse-down (fn [evt]
                                                              (.stopPropagation evt)
                                                              (.preventDefault evt)
                                                              (dispatch [::grab {:diagram.object/type :diagram :start-pos [tx ty]} [(.-clientX evt) (.-clientY evt)]]))
                                             :on-wheel (fn [evt]
                                                         (dispatch [::zoom (.-deltaY evt) [(.-clientX evt) (.-clientY evt)]]))
                                             :on-mouse-move (fn [evt]
                                                              (when grab
                                                                (dispatch [::drag [(.-clientX evt) (.-clientY evt)]])))
                                             :on-mouse-up (fn [evt]
                                                            (dispatch [::grab-release]))}
                         [:div.translate-div {:style {:transform (gstring/format "translate(%dpx,%dpx) scale(%f)" tx ty scale)
                                                      :transform-origin "0px 0px"
                                                      :height "100%"
                                                      ;; For debugging
                                                      ;; :background "#e2acac"
                                                      }}
                          [:div.nodes-and-links-wrapper
                           [:svg.links {:style {:overflow :visible}}
                            (for [l (vals links)]
                              ^{:key (::id l)}
                              [link nodes l])

                            (when-let [lf (get-in grab [:grab-object :tmp-link-from])]
                              [link nodes (let [[current-x current-y] (client-coord->dia-coord {:translate translate
                                                                                                :scale scale}
                                                                                               (:cli-current grab))]
                                            {:from-port lf :to-x current-x :to-y current-y})])]
                           [:div.nodes
                            (doall (for [n (vals nodes)]
                                     ^{:key (::id n)}
                                     [node n]))]]]]))}))

(defn register-node-component! [node-type component-fn]
  (swap! node-components assoc node-type component-fn))

(reg-event-db ::grab (fn [db [_ & args]] (apply grab (into [db] args))))
(reg-event-db ::zoom (fn [db [_ & args]] (apply zoom (into [db] args))))
(reg-event-db ::drag (fn [db [_ & args :as e]] (apply drag (into [db] args))))
(reg-event-db ::add-link (fn [db [_ & args]] (apply add-link (into [db] args))))
(reg-event-db ::grab-release (fn [db [_ & args]] (apply grab-release (into [db] args))))
(reg-event-db ::set-port-dimensions (fn [db [_ & args]] (apply set-port-dimensions (into [db] args))))
(reg-event-db ::set-node-dimensions (fn [db [_ & args]] (apply set-node-dimensions (into [db] args))))
(reg-event-db ::translate (fn [db [_ & args]] (apply translate (into [db] args))))

(reg-sub ::diagram (fn [db _] (::diagram db)))
(reg-sub ::translate (fn [db _] (get-in db [::diagram :translate])))
(reg-sub ::scale (fn [db _] (get-in db [::diagram :scale])))

(reg-sub ::grab (fn [db _] (get-in db [::diagram :grab])))
