(ns clograms.subs
  (:require [re-frame.core :as re-frame]
            [datascript.core :as d]
            [clograms.db :refer [project-browser-level-idx->key] :as db]
            [clograms.re-grams.re-grams :as rg]
            [clograms.utils :as utils]
            [clojure.zip :as zip]
            [goog.string :as gstring]))

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
   (->> (d/q '[:find ?pname ?nsname ?vname ?vid ?fsrcf ?fsrcs
               :in $
               :where
               [?vid :var/name ?vname]
               [?vid :var/namespace ?nsid]
               [?fid :function/var ?vid]
               [?fid :function/source-form ?fsrcf]
               [?fid :function/source-str ?fsrcs]
               [?nsid :namespace/name ?nsname]
               [?pid :project/name ?pname]
               [?nsid :namespace/project ?pid]]
             db)
        (map #(zipmap [:project/name :namespace/name :var/name :var/id :function/source-form :function/source-str] %)))))

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
        (project-browser-level-idx->key))))

(re-frame/reg-sub
 ::side-bar-browser-selected-project
 (fn [{:keys [:projects-browser :datascript/db]}]
   (let [project-id (:selected-project projects-browser)]
     {:project/id project-id
      :project/name (:project/name (d/entity db project-id))})))

(re-frame/reg-sub
 ::side-bar-browser-selected-namespace
 (fn [{:keys [:projects-browser :datascript/db]}]
   (let [ns-id (:selected-namespace projects-browser)]
     {:namespace/id ns-id
      :namespace/name (:namespace/name (d/entity db ns-id))})))

(defn project-items [datascript-db]
  (when datascript-db
    (->> (d/q '[:find ?pid ?pname
                :in $
                :where
                [?pid :project/name ?pname]]
              datascript-db)
         (map #(zipmap [:project/id :project/name] %))
         (map #(assoc % :type :project))
         (sort-by (comp str :project/name)))))

(defn namespaces-items [datascript-db pid]
  (when datascript-db
    (->> (d/q '[:find ?pid ?nsid ?nsname
               :in $ ?pid
               :where
               [?nsid :namespace/project ?pid]
               [?nsid :namespace/name ?nsname]]
             datascript-db
             pid)
        (map #(zipmap [:project/id :namespace/id :namespace/name] %))
        (map #(assoc % :type :namespace))
        (sort-by (comp str :namespace/name)))))

(defn vars-items [datascript-db nsid]
  (when datascript-db
    (->> (d/q '[:find ?pid ?nsid ?vid ?vname ?vpub ?vline ?fid ?fsrcf ?fsrcs
                :in $ ?nsid
                :where
                [?nsid :namespace/project ?pid]
                [?vid :var/namespace ?nsid]
                [?vid :var/name ?vname]
                [?vid :var/public? ?vpub]
                [?vid :var/line ?vline]
                [?fid :function/var ?vid]
                [?fid :function/source-form ?fsrcf]
                [?fid :function/source-str ?fsrcs]
                #_[?vid :function/_var ?fid]
                #_[(get-else $ ?fid :function/var nil) ?x]
                #_[(get-else $ ?fid :file/name "N/A") ?fname]]
              datascript-db
              nsid)
         (map #(zipmap [:project/id :namespace/id :var/id :var/name :var/public?
                        :var/line :function/id :function/source-form :function/source-str] %))
         (map #(assoc % :type :var))
         (sort-by :var/line))))

(re-frame/reg-sub
 ::side-bar-browser-items
 (fn [db _]
   (let [{:keys [level selected-project selected-namespace]} (:projects-browser db)
         level-key (project-browser-level-idx->key level)]
     (case level-key
       :projects (let [all-projects (project-items (:datascript/db db))
                       is-main-project #(when (= (:project/name %) 'clindex/main-project) %)
                       main-project (some is-main-project all-projects)]
                   (into [main-project] (remove is-main-project all-projects)))
       :namespaces (namespaces-items (:datascript/db db) selected-project)
       :vars (vars-items (:datascript/db db) selected-namespace)))))

(def callers-refs
  (memoize
   (fn [db var-id]
     (when db
       (let [q-result (d/q '[:find ?pname ?vrnsn ?in-fn ?fsrcf ?fsrcs ?fnvid
                             :in $ ?vid
                             :where
                             [?vid :var/namespace ?nid]
                             [?vid :var/name ?vn]
                             [?vrid :var-ref/var ?vid]
                             [?vrid :var-ref/namespace ?vrnid]
                             [?vrid :var-ref/in-function ?fnid]
                             [?fnid :function/var ?fnvid]
                             [?fnid :function/source-form ?fsrcf]
                             [?fnid :function/source-str ?fsrcs]
                             [?fnvid :var/name ?in-fn]
                             [?vrnid :namespace/name ?vrnsn]
                             [?pid :project/name ?pname]
                             [?vrnid :namespace/project ?pid]
                             [(get-else $ ?fid :file/name "N/A") ?fname]] ;; while we fix the file issue
                           db
                           var-id)]
         (->> q-result
              (map #(zipmap [:project/name :namespace/name :var/name :function/source-form
                             :function/source-str :var/id] %))))))))

(re-frame/reg-sub
 ::selected-var-refs
 (fn [db _]
   (let [selected-entity (:entity (rg/selected-node db))
         non-interesting (fn [v] (#{'cljs.core 'clojure.core} (:namespace/name v)))
         same-as-selected (fn [v] (= (:var/id v) (:var/id selected-entity)))]

     (when (and selected-entity
                (= (:entity/type selected-entity) :var))
       (let [all-callers-refs (callers-refs (:datascript/db db) (:var/id selected-entity))]
         {:called-by (->> all-callers-refs
                          (remove same-as-selected)
                          (map #(assoc % :entity/type :var))
                          (into #{}))})))))
(re-frame/reg-sub
 ::ctx-menu
 (fn [{:keys [ctx-menu]}]
   ctx-menu))

(re-frame/reg-sub
 ::selected-color
 (fn [{:keys [selected-color]}]
   selected-color))

(re-frame/reg-sub
 ::namespace-color
 (fn [db [_ ns]]
   (db/namespace-color db ns)))

(re-frame/reg-sub
 ::project-color
 (fn [db [_ project]]
   (db/project-color db project)))

(re-frame/reg-sub
 ::loading?
 (fn [db _]
   (:loading? db)))

(defmulti fill-entity (fn [_ entity] (:entity/type entity)))

(defmethod fill-entity :project
  [ds-db {:keys [:project/id] :as e}]
  (assoc e :project/name (:project/name (d/entity ds-db id))))

(defmethod fill-entity :namespace
  [ds-db {:keys [:namespace/id] :as e}]
  (let [namespace (d/entity ds-db id)]
    (assoc e
           :project/name (:project/name (:namespace/project namespace))
           :namespace/name (:namespace/name namespace))))

(defn make-function-source-link [var-id src]
  (gstring/format "<a onclick=\"clograms.events.add_var_from_link(%d)\">%s</a>"
                  var-id src))

(defn enhance-source [datascript-db {:keys [:function/source-form] :as entity}]
  (let [{:keys [line column]} (meta source-form)
        all-symbols-meta (loop [all nil
                                zloc (utils/move-zipper-to-next (utils/code-zipper source-form) symbol?)]
                           (if (zip/end? zloc)
                             all
                             (recur (if-let [m (meta (zip/node zloc))]
                                      (conj all m)
                                      all) ;; we should add only if it points to some other var
                                    (utils/move-zipper-to-next zloc symbol?))))
        all-meta-at-origin (->>  all-symbols-meta
                                 (map (fn [m]
                                        (-> m
                                            (update :line #(- % line))
                                            (update :end-line #(- % line))
                                            (update :column #(- % column))
                                            (update :end-column #(- % column))))))]
    (reduce (fn [e {:keys [line column end-column] :as m}]
              (if (and (:var/id m)
                       (not= (:var/id entity) (:var/id m)))
                (update e :function/source-str
                        (fn [src]
                          (utils/replace-in-str-line (partial make-function-source-link (:var/id m))
                                                     src
                                                     line
                                                     column
                                                     (- end-column column))))
                e))
            entity
            all-meta-at-origin)))

(defmethod fill-entity :var
  [ds-db {:keys [:var/id] :as e}]
  (let [entity-extra (->> (d/q '[:find ?vn ?fsf ?fss ?nsn ?pn
                                 :in $ ?vid
                                 :where
                                 [?vid :var/name ?vn]
                                 [?vid :var/namespace ?nsid]
                                 [?nsid :namespace/name ?nsn]
                                 [?nsid :namespace/project ?pid]
                                 [?pid :project/name ?pn]
                                 [?fid :function/var ?vid]
                                 [?fid :function/source-form ?fsf]
                                 [?fid :function/source-str ?fss]]
                               ds-db
                               id)
                          first
                          (zipmap [:var/name
                                   :function/source-form
                                   :function/source-str
                                   :namespace/name
                                   :project/name])
                          (enhance-source ds-db))]
    (merge e entity-extra)))

(re-frame/reg-sub
 ::datascript-db
 (fn [db _]
   (:datascript/db db)))

(re-frame/reg-sub
 ::entity
 :<- [::datascript-db]
 (fn [datascript-db [_ entity]]
   (fill-entity datascript-db entity)))
