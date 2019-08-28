(ns clograms.db)

(def project-browser-transitions
  [:projects :namespaces :vars])

(def project-browser-level-idx->key (->> project-browser-transitions
                                         (map-indexed #(vector %1 %2))
                                         (into {})))
(def project-browser-level-key->idx (->> project-browser-transitions
                                         (map-indexed #(vector %2 %1))
                                         (into {})))

(defn node [db node-id]
  (get-in db [:diagram :nodes node-id]))

(defn add-node [db node]
  (assoc-in db [:diagram :nodes (:storm.node/id node)] node))

(defn update-node-position [db node-id x y]
  (-> db
      (assoc-in [:diagram :nodes node-id :x] x)
      (assoc-in [:diagram :nodes node-id :y] y)))

(defn remove-node [db node-id]
  (update db :diagram
          (fn [dia]
            (update dia :nodes dissoc node-id))))

(defn select-node [db node-id]
  (assoc-in db [:diagram :selected-node] node-id))

(defn selected-node [db]
  (get-in db [:diagram :selected-node]))

(defn select-project [db p]
  (assoc-in db [:projects-browser :selected-project] p))

(defn select-namespace [db ns]
  (assoc-in db [:projects-browser :selected-namespace] ns))

;; Node
;; {:storm.node/id "7338a7d8-0827-4186-9da9-94015b110565"
;;  :storm.node/type "project-node"
;;  :x 505
;;  :y 400
;;  :clograms/entity {}}

(def default-db
  {:side-bar {:selected-side-bar-tab :projects-browser}
   :projects-browser {:level 0
                      :selected-project nil
                      :selected-namspace nil}
   :diagram {:selected-node nil ;; node id
             :nodes {}
             :links {}}})
