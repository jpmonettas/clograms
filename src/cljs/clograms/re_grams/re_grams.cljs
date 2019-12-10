(ns clograms.re-grams.re-grams
  (:require [reagent.core :as r]
            [cljs.pprint :as pp]
            [goog.string :as gstring]
            [clojure.string :as str]
            [clojure.core.matrix :as matrix]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx reg-sub subscribe]]))

(defn initial-db []
  {::diagram {:nodes {}
              :links {}
              :link-config {:arrow-start? false
                            :diagram.link/type :clograms/straight-line
                            :arrow-end? true}
              :main-tool-config {:tool :selection}
              :scale 1
              :translate [0 0]
              :selected-nodes-ids #{}}})

(defonce node-components (atom {}))
(defonce link-components (atom {}))

(defn client-coord->dia-coord [{:keys [translate scale]} [client-x client-y]]
  (let [[tx ty] translate]
    [(quot (- client-x tx) scale)
     (quot (- client-y ty) scale)]))

(defn dia-coord->client-coord [{:keys [translate scale]} [dia-x dia-y]]
  (let [[tx ty] translate]
    [(int (* (+ dia-x tx) scale))
     (int (* (+ dia-y ty) scale))]))

(defn grab [db grab-obj client-grab-origin]
  (assoc-in db [::diagram :grab] {:cli-origin client-grab-origin
                                  :grab-object grab-obj
                                  :cli-current client-grab-origin}))

(defn grab-release [db]
  (assoc-in db [::diagram :grab] nil))

(defn gen-random-id [] (str (random-uuid)))

(defn add-node [db {:keys [:client-x :client-y :x :y :w :h ::id] :as data}]
  (let [node-id (or id (gen-random-id))
        [dia-x dia-y] (cond
                        (and x y) [x y]
                        (and client-x client-y) (client-coord->dia-coord (::diagram db) [client-x client-y])
                        :else (client-coord->dia-coord (::diagram db) [500 500]))]
    (-> db
        (update-in [::diagram :nodes] assoc node-id (merge {::id node-id
                                                            :x dia-x
                                                            :y dia-y
                                                            ;; automatically set after render
                                                            :w 0 :h 0
                                                            :ports (->> (range 0 8)
                                                                        (map (fn [pid]
                                                                               [pid {:id pid :w 0 :h 0 :x 0 :y 0}]))
                                                                        (into {}))}
                                                           (dissoc data
                                                                   :client-x
                                                                   :client-y))))))

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

(defn get-node [db node-id]
  (get-in db [::diagram :nodes node-id]))

;;;;; Node selection

(defn selected-nodes-ids [db]
  (get-in db [::diagram :selected-nodes-ids]))

(defn selected-nodes [db]
  (-> db
      ::diagram
      :nodes
      (select-keys (selected-nodes-ids db))
      vals))

(defn add-node-to-selection
  "If node isn't selected add it to selection, if it is already part of selection remove it."
  [db node-id]
  (if (contains? (get-in db [::diagram :selected-nodes-ids]) node-id)
    (update-in db [::diagram :selected-nodes-ids] disj node-id)
    (update-in db [::diagram :selected-nodes-ids] conj node-id)))

(defn remove-node-from-selection [db node-id]
  (update-in db [::diagram :selected-nodes-ids] disj node-id))

(defn clear-nodes-selection [db]
  (assoc-in db [::diagram :selected-nodes-ids] #{}))

(defn select-single-node
  "Leave this node as the only selected node, unless it is part of the current selection.
  In that case it is a nop."
  [db node-id]
  (if-not (contains? (get-in db [::diagram :selected-nodes-ids]) node-id)
    (assoc-in db [::diagram :selected-nodes-ids] #{node-id})
    db))

;; ----------------------------

(defn set-node-extra-data [db node-id extra-data]
  (assoc-in db [::diagram :nodes node-id :extra-data] extra-data))

(defn node-extra-data [db node-id]
  (get-in db [::diagram :nodes node-id :extra-data]))

(defn node-ports [node]
  (:ports node))

(defn add-link [db [from-node from-port :as from] [to-node to-port :as to]]
  (let [link-id (gen-random-id)
        {:keys [:arrow-start? :arrow-end? :diagram.link/type]} (get-in db [::diagram :link-config])]
    (update-in db [::diagram :links] assoc link-id {::id link-id
                                                    :from-port from
                                                    :to-port to
                                                    :arrow-start? arrow-start?
                                                    :arrow-end? arrow-end?
                                                    :diagram.link/type type})))

(defn set-link-type [db link-type]
  (assoc-in db [::diagram :link-config :diagram.link/type] link-type))

(defn set-link-label [db link-id label]
  (assoc-in db [::diagram :links link-id :label] label))

(defn remove-link [db link-id]
  (update-in db [::diagram :links] dissoc link-id))

(defn set-link-config [db link-config]
  (assoc-in db [::diagram :link-config] link-config))

;;;;;;;;;;
;; Port ;;
;;;;;;;;;;

(defn set-port-dimensions [db node-id port-id {:keys [w h client-x client-y]}]
  (let [[x y] (client-coord->dia-coord (::diagram db) [client-x client-y])
        scale (get-in db [::diagram :scale])]
    (update-in db [::diagram :nodes node-id :ports port-id]
              (fn [dims]
                (assoc dims
                       :w (quot w scale)
                       :h (quot h scale)
                       :x x
                       :y y)))))

(def svg-port-side 7)

(defn port [node p]
  (let [node-comp  (get @node-components (:diagram.node/type node))
        grab-sub (subscribe [::grab])
        node-id (::id node)
        update-after-render (fn [p-cmp]
                              (let [dn (r/dom-node p-cmp)
                                    brect (.getBoundingClientRect dn)]
                                (dispatch [::set-port-dimensions node-id (:id p) {:w (.-width brect)
                                                                                  :h (.-height brect)
                                                                                  :client-x (.-x brect)
                                                                                  :client-y (.-y brect)}])))]
    (r/create-class
     {:component-did-mount (fn [this] (update-after-render this))
      :component-did-update (fn [this] (update-after-render this))
      :reagent-render
      (fn [node p]
        (let [props-map {:class (str "port-" (:id p))
                         :on-mouse-down (fn [evt]
                                          (.stopPropagation evt)
                                          (.preventDefault evt)
                                          (dispatch [::grab {:diagram.object/type :link
                                                             :tmp-link-from [node-id (:id p)]}
                                                     [(.-clientX evt) (.-clientY evt)]]))
                         :on-mouse-up (fn [evt]
                                        (dispatch [::add-link (get-in @grab-sub [:grab-object :tmp-link-from]) [node-id (:id p)]]))} ]
          (case (:type node-comp)
           :div [:div.div-port props-map [:div.port-inner]]
           :svg [:rect.svg-port (merge props-map p {:width svg-port-side :height svg-port-side})])

         ))})))

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
                        :w (quot w scale)
                        :h (quot h scale))))))

(def left-button 1)
(def right-button 2)

;; Graphical debugging stuff

(def debug false)

(defn node-debug [{:keys [x y w h] :as node}]
  (let [s @(subscribe [::scale])
        t @(subscribe [::translate])
        [cx cy] (dia-coord->client-coord {:translate t :scale s} [x y])]
   [:ul.node-debug
    [:li (str "x: " x)]
    [:li (str "y: " y)]
    [:li (str "w: " w)]
    [:li (str "h: " h)]
    [:li (str "cx: " cx)]
    [:li (str "cy: " cy)]]))

(defn debug-bar [scale translate]
  [:div.debug-bar
   [:span (gstring/format "translate: %s, scale: %1.3f" translate scale)]])

(defn build-node-click-handler [n]
  (fn [evt]
    (if (.-ctrlKey evt)
      (dispatch [::add-node-to-selection (::id n)])
      (dispatch [::select-single-node (::id n)]))))

(defn build-node-mouse-down-handler [n]
  (fn [evt]
    (.stopPropagation evt)
    (when-not (.. evt -target -attributes -contenteditable)
      ;; this makes things like text areas inside nodes unable to get focus
      ;; TODO : also add it when target is a input
        (.preventDefault evt))
    (when (= left-button (.-buttons evt))
      (dispatch [::grab {:diagram.object/type :node
                         ::id (::id n)}
                 [(.-clientX evt) (.-clientY evt)]]))))

(defn build-svg-node-resizer-handler [n]
  (let [{:keys [prop-resize?]} (get @node-components (:diagram.node/type n) default-node)]
    (fn [evt]
      (.stopPropagation evt)
      (when (= left-button (.-buttons evt))
        (dispatch [::grab {:diagram.object/type :node-resizer
                           ::id (::id n)
                           :prop-resize? prop-resize?}
                   [(.-clientX evt) (.-clientY evt)]])))))

(defn port-position [node port-id]
  (let [[nx ny nw nh] ((juxt :x :y :w :h) node)
        fix (- svg-port-side)]
    (get {0 {:x fix         :y fix}
          1 {:x (quot nw 2) :y fix}
          2 {:x nw          :y fix}
          3 {:x nw          :y (quot nh 2)}
          4 {:x nw          :y nh}
          5 {:x (quot nw 2) :y nh}
          6 {:x fix         :y nh}
          7 {:x fix         :y (quot nh 2)}}
               port-id)))

(defn svg-node [n]
  (let [node-comp (get @node-components (:diagram.node/type n) default-node)
        selected-nodes-ids @(subscribe [::selected-nodes-ids])]
    [:g.svg-node.node {:class (when (selected-nodes-ids (::id n)) "selected")
                       :on-click (build-node-click-handler n)
                       :on-mouse-down (build-node-mouse-down-handler n)
                       :transform (gstring/format "translate(%d,%d)" (:x n) (:y n))}

     (for [p (vals (:ports n))]
       ^{:key (:id p)}
       [port n (merge p (port-position n (:id p)))])

     [(:comp node-comp) n (:svg-url node-comp)]
     [:circle.resizer {:cx (:w n)
                       :cy (:h n)
                       :r 20
                       :on-mouse-down (build-svg-node-resizer-handler n)}]]))

(defn div-node [n]
  (let [node-comp (get @node-components (:diagram.node/type n) default-node)
        selected-nodes-ids (subscribe [::selected-nodes-ids])
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
        [:div.node {:class (when (@selected-nodes-ids (::id n)) "selected")
                    :on-click (build-node-click-handler n)
                    :on-mouse-down (build-node-mouse-down-handler n)
                    :style {:position :absolute
                            :top (:y n)
                            :left (:x n)}}
         (when debug [node-debug n])

         (for [p (vals (:ports n))]
           ^{:key (:id p)}
           [port n p])

         [(:comp node-comp) n]])})))

(defn drag-nodes [db grab-node-id [drag-x drag-y]]
  (let [selection (selected-nodes-ids db)
        update-node (fn [d nid]
                      (-> d
                          (update-in [::diagram :nodes nid :x] + drag-x)
                          (update-in [::diagram :nodes nid :y] + drag-y)))]
    (if (> (count selection) 1)
      (reduce (fn [d sel-nid]
                (update-node d sel-nid))
              db
              selection)

      (update-node db grab-node-id))))

(defn drag [db current-cli-coord]
  (if-let [{:keys [cli-origin grab-object]} (get-in db [::diagram :grab])]
    (let [[cli-x cli-y] current-cli-coord
          [current-dia-x current-dia-y] (client-coord->dia-coord (::diagram db) current-cli-coord)
          before-cli-coord (get-in db [::diagram :grab :cli-current])
          [drag-x drag-y] (let [[before-x before-y] (client-coord->dia-coord (::diagram db) before-cli-coord)]
                            [(- current-dia-x before-x) (- current-dia-y before-y)])

          db' (assoc-in db [::diagram :grab :cli-current] current-cli-coord)]
      (case (:diagram.object/type grab-object)
        :node (drag-nodes db' (::id grab-object) [drag-x drag-y])
        :node-resizer (let [[drag-x drag-y] (if (:prop-resize? grab-object)
                                              (let [drag (max drag-x drag-y)]
                                                [drag drag])
                                              [drag-x drag-y])]

                        (-> db'
                            (update-in [::diagram :nodes (::id grab-object) :w] + drag-x)
                            (update-in [::diagram :nodes (::id grab-object) :h] + drag-y)))
        :diagram (let [[before-cli-x before-cli-y] before-cli-coord
                       cli-drag-x (- cli-x before-cli-x)
                       cli-drag-y (- cli-y before-cli-y)]
                   (-> db'
                       (update-in [::diagram :translate 0] + cli-drag-x)
                       (update-in [::diagram :translate 1] + cli-drag-y)))
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
                         :translate [(int new-translate-x) (int new-translate-y)]
                         :scale new-scale)
                  dia))))))

(defn line-link [{:keys [arrow-start? arrow-end? x1 y1 x2 y2]}]
  [:g
   [:line (cond-> {:x1 x1 :y1 y1 :x2 x2 :y2 y2
                   :stroke :gray
                   :stroke-width 3}
            arrow-start?  (assoc :marker-start "url(#arrow-start)")
            arrow-end?    (assoc :marker-end "url(#arrow-end)"))]])

(defn link-curve-string [[[fpx fpy] & points]]
  (gstring/format "M%f %f C%s" fpx fpy (str/join "," (map #(str/join " " %) points))))

(defn curve-link [{:keys [x1 y1 x2 y2 arrow-start? arrow-end?]}]
  [:path {:stroke :gray
          :fill :none
          :stroke-width 3
          :d (link-curve-string [[x1 y1] [(- x1 50) y1] [(+ x2 50) y2] [x2 y2]])}])

(defn translate-diagram [db to]
  (assoc-in db [::diagram :translate] to))

(defn link [nodes {:keys [from-port to-port]  :as l}]
  (let [text-y-gap -7 ;; this is so the text doesn't shows oevr the link
        link-component (get @link-components (:diagram.link/type l) line-link)
        center (fn [{:keys [x y w h]}] (when (and x y w h)
                                         [(+ x (quot w 2)) (+ y (quot h 2))]))]
   (fn [nodes {:keys [from-port to-port]  :as l}]
     (let [[from-n from-p] from-port
           [to-n to-p] to-port
           [x1 y1] (center (get-in nodes [from-n :ports from-p]))
           [x2 y2] (center (get-in nodes [to-n :ports to-p]))
           x2 (or x2 (:to-x l))
           y2 (or y2 (:to-y l))
           link-center-x (+ x1 (quot (- x2 x1) 2))
           link-center-y (+ y1 (quot (- y2 y1) 2) text-y-gap)]
       (when (and x1 y1) ;; so it doesn't fail when we still don't have port coordinates (waiting for render)
         [:g
          (when-not (str/blank? (:label l))
            [:text {:text-anchor :middle
                    :stroke :none
                    :fill :grey
                    :x link-center-x
                    :y link-center-y}
             (:label l)])
          [:g.link
           [link-component {:x1 x1 :y1 y1 :x2 x2 :y2 y2
                            ::id (::id l)
                            :arrow-start? (:arrow-start? l)
                            :arrow-end?   (:arrow-end? l)}]]])))))

(defn arrow-markers []
  [:defs
   [:marker {:id "arrow-end" :marker-width 10 :marker-height 10 :ref-x 5 :ref-y 2 :orient "auto" :marker-units "strokeWidth"}
    [:path {:d "M0,0 L0,4 L6,2 z" :fill "gray"}]]
   [:marker {:id "arrow-start" :marker-width 10 :marker-height 10 :ref-x 0 :ref-y 2 :orient "auto" :marker-units "strokeWidth"}
    [:path {:d "M0,2 L6,4 L6,0 z" :fill "gray"}]]])

(defn svg-nodes-components []
  (let [nodes-comp @node-components
        svg-keys (keep (fn [[k c]] (when (= :svg (:type c)) k)) nodes-comp)]
    (select-keys nodes-comp svg-keys )))

(defn div-nodes-components []
  (let [nodes-comp @node-components
        div-keys (keep (fn [[k c]] (when (= :div (:type c)) k)) nodes-comp)]
    (select-keys nodes-comp div-keys)))

(defn svg-nodes-comparator [type1 type2]
  ;; let put groups first so they don't oclude other svg nodes
  (cond
    (= type1 type2) 0
    (= type1 :clograms/group-node) -1
    (= type2 :clograms/group-node) 1
    :else 0))

(defn diagram [dia]
  (r/create-class
   {:component-did-mount (fn [this])
    :reagent-render (fn [dia]
                      (let [{:keys [nodes links link-config grab translate scale scale-origin]} dia
                            [tx ty] translate
                            div-nodes (filter (fn [n] (contains? (div-nodes-components) (:diagram.node/type n))) (vals nodes))
                            ;; the order in which we render the nodes matters since nodes rendered after
                            ;; ocludes nodes rendered first
                            svg-nodes (->> (vals nodes)
                                           (filter (fn [n] (contains? (svg-nodes-components) (:diagram.node/type n))))
                                           (sort-by :diagram.node/type svg-nodes-comparator))]
                        [:div.diagram-layer {:style {:overflow :hidden}
                                             :on-mouse-down (fn [evt]
                                                              (when (= left-button (.-buttons evt))
                                                                (.stopPropagation evt)
                                                                (.preventDefault evt)
                                                                (dispatch [::grab {:diagram.object/type :diagram}
                                                                           [(.-clientX evt) (.-clientY evt)]])
                                                                (dispatch [::clear-nodes-selection])))
                                               :on-wheel (fn [evt]
                                                           (dispatch [::zoom (.-deltaY evt) [(.-clientX evt) (.-clientY evt)]]))
                                               :on-mouse-move (fn [evt]
                                                                (when grab
                                                                  (dispatch [::drag [(.-clientX evt) (.-clientY evt)]])))
                                               :on-mouse-up (fn [evt]
                                                              (dispatch [::grab-release]))}
                         (when debug [debug-bar scale translate])
                         [:div.transform-div {:style {:transform (gstring/format "translate(%dpx,%dpx) scale(%f)" tx ty scale)
                                                      :transform-origin "0px 0px"
                                                      :height "100%"
                                                      ;; For debugging
                                                      ;; :background "#e2acac"
                                                      }}
                          [:svg {:style {:overflow :visible}}
                           [arrow-markers]

                           ;; current link (a temp link while drawing it)
                           (when-let [lf (get-in grab [:grab-object :tmp-link-from])]
                             [link nodes (let [[current-x current-y] (client-coord->dia-coord {:translate translate
                                                                                               :scale scale}
                                                                                              (:cli-current grab))]
                                           (merge {:from-port lf :to-x current-x :to-y current-y}
                                                  link-config))])

                           ;; ATTENTION !!! : the order matters here, if shapes are rendered after links they will hide them

                           ;; svg nodes
                           (for [n svg-nodes]
                             ^{:key (::id n)}
                             [svg-node n])

                           ;; links
                           (for [l (vals links)]
                             ^{:key (::id l)}
                             [link nodes l])]

                          [:div.nodes
                           (doall (for [n div-nodes]
                                    ^{:key (::id n)}
                                    [div-node n]))]]]))}))

(defn register-node-component! [node-type comp-desc]
  (swap! node-components assoc node-type comp-desc))

(defn register-link-component! [link-type component-fn]
  (swap! link-components assoc link-type component-fn))

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
(reg-sub ::selected-nodes-ids (fn [db _] (get-in db [::diagram :selected-nodes-ids])))
(reg-sub ::link-config (fn [db _] (get-in db [::diagram :link-config])))
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
(reg-event-db ::set-link-config (fn [db [_ link-config]] (set-link-config db link-config)))
(reg-event-db ::set-link-label (fn [db [_ link-id label]] (set-link-label db link-id label)))
(reg-event-db ::remove-link (fn [db [_ link-id]] (remove-link db link-id)))
(reg-event-db ::add-node (fn [db [_ node]] (add-node db node)))
(reg-event-db ::remove-node (fn [db [_ node-id]] (remove-node db node-id)))
(reg-event-fx ::select-single-node (fn [{:keys [db]} [_ node-id]]
                                     (let [db' (select-single-node db node-id)]
                                       {:db db'
                                        :dispatch [::node-selection-updated (selected-nodes db')]})))
(reg-event-fx ::add-node-to-selection (fn [{:keys [db]} [_ node-id]]
                                        (let [db' (add-node-to-selection db node-id)]
                                          {:db db'
                                           :dispatch [::node-selection-updated (selected-nodes db')]})))
(reg-event-db ::clear-nodes-selection (fn [db [_]] (clear-nodes-selection db)))



;; Intended for users to override and listen
;; -----------------------------------------

(reg-event-db ::node-selection-updated identity)


;;;;;;;;;;;;;;;;;;;;;;
;; For repl testing ;;
;;;;;;;;;;;;;;;;;;;;;;

(comment

  )
