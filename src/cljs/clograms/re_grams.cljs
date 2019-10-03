(ns clograms.re-grams
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]
            [goog.string :as gstring]
            [clojure.string :as str]
            [clojure.core.matrix :as matrix]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub subscribe]]))

;; Schema
#_{:nodes {"nid1" {::id "nid1"
                   :diagram.node/type :custom-node-1
                   :x 20
                   :y 20
                   :ports {"pid1" {::id "pid1"
                                   :diagram.port/type :custom-port1}
                           "pid2" {::id "pid2"
                                   :diagram.port/type :custom-port1}}}
           "nid2" {::id "nid2"
                   :diagram.node/type :custom-node-2
                   :x 510
                   :y 510
                   :ports {"pid3" {::id "pid3"
                                   :diagram.port/type :custom-port1}}}
           }
   ;; :links {"lid1" {::id "lid1"
   ;;                 :from-port ["nid1" "pid2"]
   ;;                 :to-port ["nid2" "pid3"]}}

   :scale 1
   :translate [0 0]
   :grab {:cli-origin [0 0]
          :cli-current [10 10]
          :grab-object {:diagram.object/type :node
                        ::id "nid1"
                        :start-pos [100 100]}}}

(defn initial-db []
  {::diagram {:nodes {}
              :links {}
              :scale 1
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
                                  :grab-object grab-obj
                                  :cli-current client-grab-origin}))

(defn grab-release [db]
  (assoc-in db [::diagram :grab] nil))

(defn gen-random-id [] (str (random-uuid)))

(defn add-node-port [db node-id {:keys [::id]:as port}]
  (let [port-id (or id (gen-random-id))]
    (update-in db [::diagram :nodes node-id :ports] assoc port-id (merge {::id port-id
                                                                          ;; automatically set after render
                                                                          :x 0 :y 0 :w 0 :h 0}
                                                                         port))))

(defn add-node [db {:keys [:client-x :client-y ::id] :as data} & [ports]]
  (let [node-id (or id (gen-random-id))
        [dia-x dia-y] (client-coord->dia-coord (::diagram db) [(or client-x 500) (or client-y 500)])
        db' (update-in db [::diagram :nodes] assoc node-id (merge {::id node-id
                                                                   :x dia-x
                                                                   :y dia-y
                                                                   ;; automatically set after render
                                                                   :w 0 :h 0}
                                                                  data))]

    (if (seq ports)
      (reduce #(add-node-port %1 node-id %2) db' ports)
      db')))

(defn remove-node [db node-id]
  (-> db
      (update-in [::diagram :nodes] dissoc node-id)
      (update-in [::diagram :links]
                 (fn [links]
                   (->> links
                        (remove (fn [[lid {:keys [from-port to-port]}]]
                                  (or (= (first from-port) node-id)
                                      (= (first to-port) node-id))))
                        (into {}))))))

(defn selected-node [db]
  (let [selected-node-id (get-in db [::diagram :selected-node-id])]
    (get-in db [::diagram :nodes selected-node-id])))

(defn select-node [db node-id]
  (assoc-in db [::diagram :selected-node-id] node-id))

(defn add-link [db [from-node from-port :as from] [to-node to-port :as to]]
  (let [link-id (gen-random-id)]
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
                                (dispatch [::set-port-dimensions node-id (::id p) {:w (.-width brect)
                                                                                   :h (.-height brect)
                                                                                   :client-x (.-x brect)
                                                                                   :client-y (.-y brect)}])))]
    (r/create-class
     {:component-did-mount (fn [this] (update-after-render this))
      :component-did-update (fn [this] (update-after-render this))
      :reagent-render
      (fn [node p]
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

(def left-button 1)
(def right-button 2)

(defn node [n]
  (let [node-component (get @node-components (:diagram.node/type n) default-node)
        selected-node-id (subscribe [::selected-node-id])
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
        [:div.node {:class (when (= @selected-node-id (::id n)) "selected")
                    :on-click (fn [evt] (dispatch [::select-node (::id n)]))
                    :on-mouse-down (fn [evt]
                                     (.stopPropagation evt)
                                     (.preventDefault evt)
                                     (when (= left-button (.-buttons evt))
                                       (dispatch [::grab {:diagram.object/type :node
                                                          ::id (::id n)
                                                          :start-pos [(:x n) (:y n)]}
                                                  [(.-clientX evt) (.-clientY evt)]])))
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

(def max-scale 2)
(def min-scale 0.1)
(defn zoom [db delta [client-center-x client-center-y :as cli-coords]]
  (let [zoom-dir (if (pos? delta) -1 1)
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
                    new-translate-y (+ ty y-scale-diff)]
                (if (< min-scale new-scale max-scale)
                  (assoc dia
                         :translate [new-translate-x new-translate-y]
                         :scale new-scale)
                  dia))))))

(defn link-curve-string [[[fpx fpy] & points]]
  (gstring/format "M%f %f C%s" fpx fpy (str/join "," (map #(str/join " " %) points))))

(defn translate-diagram [db to]
  (assoc-in db [::diagram :translate] to))

(defn link [nodes {:keys [from-port to-port]  :as l}]
  (let [center (fn [{:keys [x y w h]}] (when (and x y w h)
                                         [(+ x (/ w 2)) (+ y (/ h 2))]))
        [from-n from-p] from-port
        [to-n to-p] to-port
        [x1 y1] (center (get-in nodes [from-n :ports from-p]))
        [x2 y2] (center (get-in nodes [to-n :ports to-p]))
        x2 (or x2 (:to-x l))
        y2 (or y2 (:to-y l))]
    (when (and x1 y1) ;; so it doesn't fail when we still don't have port coordinates (waiting for render)
      [:g
       [:path {:stroke :gray
               :fill :none
               :stroke-width 3
               :d (link-curve-string [[x1 y1] [(- x1 50) y1] [(+ x2 50) y2] [x2 y2]])}]])))

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
                         [:div.transform-div {:style {:transform (gstring/format "translate(%dpx,%dpx) scale(%f)" tx ty scale)
                                                      :transform-origin "0px 0px"
                                                      :height "100%"
                                                      ;; For debugging
                                                      ;; :background "#e2acac"
                                                      }}
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
                                    [node n]))]]]))}))

(defn register-node-component! [node-type component-fn]
  (swap! node-components assoc node-type component-fn))

;;;;;;;;;;;;;;;;;;;
;; Subscriptions ;;
;;;;;;;;;;;;;;;;;;;

;; Internals
;; ---------

(reg-sub ::grab (fn [db _] (get-in db [::diagram :grab])))

;; Intended for users
;; ------------------

(reg-sub ::diagram (fn [db _] (::diagram db)))
(reg-sub ::translate (fn [db _] (get-in db [::diagram :translate])))
(reg-sub ::scale (fn [db _] (get-in db [::diagram :scale])))
(reg-sub ::selected-node-id (fn [db _] (get-in db [::diagram :selected-node-id])))

;;;;;;;;;;;;;
;; Events  ;;
;;;;;;;;;;;;;

;; Internals
;; ---------

(reg-event-db ::grab (fn [db [_ grab-obj client-grab-origin]] (grab db grab-obj client-grab-origin)))
(reg-event-db ::grab-release (fn [db _] (grab-release db)))
(reg-event-db ::drag (fn [db [_ current-cli-coord]] (drag db current-cli-coord)))
(reg-event-db ::set-port-dimensions (fn [db [_ node-id port-id dims]] (set-port-dimensions db node-id port-id dims)))
(reg-event-db ::set-node-dimensions (fn [db [_ node-id dims]] (set-node-dimensions db node-id dims)))

;; Intended for users to call
;; --------------------------

(reg-event-db ::zoom (fn [db [_ delta cli-coords]] (zoom db delta cli-coords)))
(reg-event-db ::translate-diagram (fn [db to] (translate-diagram db to)))
(reg-event-db ::add-link (fn [db [_ from-port to-port]] (add-link db from-port to-port)))
(reg-event-db ::add-node (fn [db [_ node ports]] (add-node db node ports)))
(reg-event-db ::remove-node (fn [db [_ node-id]] (remove-node db node-id)))
(reg-event-db ::add-node-port (fn [db [_ node-id port]] (add-node-port db node-id port)))
(reg-event-fx ::select-node (fn [{:keys [db]} [_ node-id]]
                              (let [db' (select-node db node-id)]
                                {:db db'
                                 :dispatch [::node-selected (selected-node db')]})))

;; Intended for users to override and listen
;; -----------------------------------------

(reg-event-db ::node-selected identity)


;;;;;;;;;;;;;;;;;;;;;;
;; For repl testing ;;
;;;;;;;;;;;;;;;;;;;;;;

(comment
  (dispatch [::add-node  {:title "NODE 1"
                          ::id "nid1"
                          :diagram.node/type :custom-node-1}
             [{::id "pid1" :diagram.port/type :custom-port-1}
              {::id "pid2" :diagram.port/type :custom-port-1}]])

  (dispatch [::add-node {:title "NODE 2"
                         ::id "nid2"
                         :client-x 10
                         :client-y 10}
             [{::id "pid1" :diagram.port/type :custom-port-1}
              {::id "pid2" :diagram.port/type :custom-port-1}]])

  (dispatch [::add-link ["nid1" "pid1"] ["nid2" "pid2"]])
  )
