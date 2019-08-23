(ns clograms.subs
  (:require [re-frame.core :as re-frame]
            [datascript.core :as d]))

(defn dependency-tree [db main-project-id]
  (d/pull db '[:project/name {:project/depends 6}] main-project-id))

(re-frame/reg-sub
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

(re-frame/reg-sub
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

(def callers-refs
  (memoize
   (fn [db ns vname]
     (let [q-result (d/q '[:find ?pname ?vrnsn ?vn ?in-fn ?vrline ?vrcolumn ?fname
                           :in $ ?nsn ?vn
                           :where
                           [?nid :namespace/name ?nsn]
                           [?vid :var/namespace ?nid]
                           [?vid :var/name ?vn]
                           [?vrid :var-ref/var ?vid]
                           [?vrid :var-ref/namespace ?vrnid]
                           [?vrid :var-ref/line ?vrline]
                           [?vrid :var-ref/column ?vrcolumn]
                           [?vrid :var-ref/in-function ?fnid]
                           [?fnid :function/var ?fnvid]
                           [?fnvid :var/name ?in-fn]
                           [?vrnid :namespace/name ?vrnsn]
                           [?pid :project/name ?pname]
                           [?vrnid :namespace/project ?pid]
                           [?vrnid :namespace/file ?fid]
                           [(get-else $ ?fid :file/name "N/A") ?fname]] ;; while we fix the file issue
                         db
                         ns
                         vname)]
       (->> q-result
            (map #(zipmap [:project :ns :var-name :in-fn :line :column :file] %)))))))

(defn callers-fns [x-refs]
  (->> x-refs
       (map (fn [x]
              {:type :called-by
               :project/name (:project x)
               :namespace/name (:ns x)
               :var/name (:in-fn x)}))
       (into #{})))

(def calls-refs
  (memoize
   (fn [db ns vname]
     (let [q-result (d/q '[:find ?pname ?destnsname ?destvname
                           :in $ ?nsn ?vn
                           :where
                           [?pid :project/name ?pname]
                           [?destns :namespace/project ?pid]
                           [?dvid :var/name ?destvname]
                           [?dvid :var/namespace ?destns]
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
            (map #(zipmap [:project :ns :var-name] %)))))))

(defn calls-fns [x-refs]
  (->> x-refs
       (map (fn [x]
              {:type :call-to
               :project/name (:project x)
               :namespace/name (:ns x)
               :var/name (:var-name x)}))
       (into #{})))

(re-frame/reg-sub
 ::selected-entity-refs
 (fn [{:keys [:datascript/db :diagram]} _]
   (when-let [selected-entity (:selected-node diagram)]
     (let [all-calls-refs (calls-refs db (:namespace/name selected-entity) (:var/name selected-entity))
           calls (calls-fns all-calls-refs)
           all-callers-refs (callers-refs db (:namespace/name selected-entity) (:var/name selected-entity))
           called-by (callers-fns all-callers-refs)]
       {:calls-to calls
        :called-by called-by}))))
