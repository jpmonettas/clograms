(ns clograms.subs
  (:require [re-frame.core :as re-frame]
            [datascript.core :as d]
            [clograms.db :refer [project-browser-transitions]]))

#_(defn dependency-tree [db main-project-id]
  (d/pull db '[:project/name {:project/depends 6}] main-project-id))

#_(re-frame/reg-sub
 ::projecs-dependencies-edges
 (fn [{:keys [:datascript/db :main-project/id]} _]
   (when (and db id)
     (->> (dependency-tree db id)
          (tree-seq (comp not-empty :project/depends) :project/depends)
         (mapcat (fn [{:keys [:project/depends] :as p}]
                   (map (fn [d]
                          [p d])
                        depends)))
         (into #{})))))

#_(re-frame/reg-sub
 ::diagram
 (fn [{:keys [diagram]} _]
   diagram))


(re-frame/reg-sub
 ::all-entities
 (fn [{:keys [:datascript/db]} _]
   (->> (d/q '[:find ?pname ?nsname ?vname ?fsrc
               :in $
               :where
               [?vid :var/name ?vname]
               [?vid :var/namespace ?nsid]
               [?fid :function/var ?vid]
               [?fid :function/source ?fsrc]
               [?nsid :namespace/name ?nsname]
               [?pid :project/name ?pname]
               [?nsid :namespace/project ?pid]]
             db)
        (map #(zipmap [:project/name :namespace/name :var/name :function/source] %)))))

(re-frame/reg-sub
 ::selected-entity
 (fn [{:keys [selected-entity]} _]
   selected-entity))

(re-frame/reg-sub
 ::selected-side-bar-tab
 (fn [{:keys [side-bar]} _]
   (:selected-side-bar-tab side-bar)))

(re-frame/reg-sub
 ::side-bar-browser-level
 (fn [db _]
   (->> db
        :projects-browser
        :level
        (nth project-browser-transitions))))

(re-frame/reg-sub
 ::side-bar-browser-selected-project
 (fn [{:keys [projects-browser]}]
   (:selected-project projects-browser)))

(re-frame/reg-sub
 ::side-bar-browser-selected-namespace
 (fn [{:keys [projects-browser]}]
   (:selected-namespace projects-browser)))

(defn project-items [datascript-db]
  (->> (d/q '[:find ?pid ?pname
              :in $
              :where
              [?pid :project/name ?pname]]
            datascript-db)
       (map #(zipmap [:id :project/name] %))
       (map #(-> %
                 (assoc :type :project)
                 (update :project/name str)))
       (sort-by :project/name)))

(defn namespaces-items [datascript-db pid]
  (->> (d/q '[:find ?nsid ?nsname
              :in $ ?pid
              :where
              [?nsid :namespace/project ?pid]
              [?nsid :namespace/name ?nsname]]
            datascript-db
            pid)
       (map #(zipmap [:id :namespace/name] %))
       (map #(-> %
                 (assoc :type :namespace)
                 (update :namespace/name str)))
       (sort-by :namespace/name)))

(defn vars-items [datascript-db nsid]
  (->> (d/q '[:find ?vid ?vname ?vline ?fid ?fsrc
              :in $ ?nsid
              :where
              [?vid :var/namespace ?nsid]
              [?vid :var/name ?vname]
              [?vid :var/line ?vline]
              [?fid :function/var ?vid]
              [?fid :function/source ?fsrc]
              #_[?vid :function/_var ?fid]
              #_[(get-else $ ?fid :function/var nil) ?x]
              #_[(get-else $ ?fid :file/name "N/A") ?fname]]
            datascript-db
            nsid)
       (map #(zipmap [:id :var/name :var/line :function/id :function/source] %))
       (map #(-> %
                 (assoc :type :var)
                 (update :var/name str)))
       (sort-by :var/line)))

(re-frame/reg-sub
 ::side-bar-browser-items
 (fn [db _]
   (let [{:keys [level selected-project selected-namespace]} (:projects-browser db)
         level-key (nth project-browser-transitions level)]
     (case level-key
       :projects (project-items (:datascript/db db))
       :namespaces (->> (namespaces-items (:datascript/db db) (:id selected-project))
                        (map #(assoc % :project/name (:project/name selected-project))))
       :vars (->> (vars-items (:datascript/db db) (:id selected-namespace))
                  (map #(assoc % :namespace/name (:namespace/name selected-namespace))))))))

(def callers-refs
  (memoize
   (fn [db ns vname]
     (let [q-result (d/q '[:find ?pname ?vrnsn ?in-fn ?fsrc ?vid
                           :in $ ?nsn ?vn
                           :where
                           [?nid :namespace/name ?nsn]
                           [?vid :var/namespace ?nid]
                           [?vid :var/name ?vn]
                           [?vrid :var-ref/var ?vid]
                           [?vrid :var-ref/namespace ?vrnid]
                           [?vrid :var-ref/in-function ?fnid]
                           [?fnid :function/var ?fnvid]
                           [?fnid :function/source ?fsrc]
                           [?fnvid :var/name ?in-fn]
                           [?vrnid :namespace/name ?vrnsn]
                           [?pid :project/name ?pname]
                           [?vrnid :namespace/project ?pid]
                           [(get-else $ ?fid :file/name "N/A") ?fname]] ;; while we fix the file issue
                         db
                         ns
                         vname)]
       (->> q-result
            (map #(zipmap [:project/name :namespace/name :var/name :function/source :id] %)))))))

(def calls-refs
  (memoize
   (fn [db ns vname]
     (let [q-result (d/q '[:find ?pname ?destnsname ?destvname ?fsrc ?dvid
                           :in $ ?nsn ?vn
                           :where
                           [?pid :project/name ?pname]
                           [?destns :namespace/project ?pid]
                           [?dvid :var/name ?destvname]
                           [?dvid :var/namespace ?destns]
                           [?dfid :function/var ?dvid]
                           [?dfid :function/source ?fsrc]
                           [?destns :namespace/name ?destnsname]
                           [?vrid :var-ref/var ?dvid]
                           [?vrid :var-ref/in-function ?fid]
                           [?fid :function/var ?fvid]

                           [?fvid :var/name ?vn]
                           [?fvid :var/namespace ?fvnsid]
                           [?fvnsid :namespace/name ?nsn]]
                         db
                         ns
                         vname)]
       (->> q-result
            (map #(zipmap [:project/name :namespace/name :var/name :function/source :id] %)))))))



(re-frame/reg-sub
 ::selected-var-refs
 (fn [{:keys [:datascript/db :diagram]} _]
   (let [selected-entity (:selected-node diagram)
         non-interesting (fn [v]
                           (#{'cljs.core 'clojure.core} (:namespace/name v)))
         same-as-selected (fn [v]
                            (and (= (:namespace/name v) (:namespace/name selected-entity))
                                    (= (:var/name v) (:var/name selected-entity))))]
     (when (and selected-entity
                (= (:type selected-entity) :var))
       (let [all-calls-refs (calls-refs db (:namespace/name selected-entity) (:var/name selected-entity))
             all-callers-refs (callers-refs db (:namespace/name selected-entity) (:var/name selected-entity))]
         {:calls-to (->> all-calls-refs
                         (remove non-interesting)
                         (remove same-as-selected)
                         (map #(assoc % :type :var))
                         (into #{}))
          :called-by (->> all-callers-refs
                          (remove same-as-selected)
                          (map #(assoc % :type :var))
                          (into #{}))})))))
