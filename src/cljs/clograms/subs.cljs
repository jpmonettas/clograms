(ns clograms.subs
  (:require [re-frame.core :as re-frame]
            [datascript.core :as d]
            [clograms.db :refer [project-browser-level-idx->key] :as db]
            [clograms.re-grams.re-grams :as rg]
            [clograms.utils :as utils]
            [clojure.zip :as zip]
            [goog.string :as gstring]
            [clograms.models :as models]))

(re-frame/reg-sub
 ::all-entities
 (fn [{:keys [:datascript/db]} _]
   (db/all-entities db)))

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
 (fn [db]
   (let [project-id (db/selected-project db)]
     {:project/id project-id
      :project/name (:project/name (db/project-entity (:datascript/db db) project-id))})))

(re-frame/reg-sub
 ::side-bar-browser-selected-namespace
 (fn [db]
   (let [ns-id (db/selected-namespace db)
         ns-entity (db/namespace-entity (:datascript/db db) ns-id)]
     {:namespace/id ns-id
      :namespace/name (:namespace/name ns-entity)})))

(defn project-items [datascript-db]
  (when datascript-db
    (->> (db/all-projects datascript-db)
         (map #(assoc % :type :project))
         (sort-by (comp str :project/name)))))

(defn namespaces-items [datascript-db pid]
  (when datascript-db
    (->> (db/all-namespaces datascript-db pid)
        (map #(assoc % :type :namespace))
        (sort-by (comp str :namespace/name)))))

(defn vars-items [datascript-db nsid]
  (when datascript-db
    (->> (db/all-vars datascript-db nsid)
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
  (memoize ;; ATTENTION ! Be careful with this cache when we implement reload on file change
   (fn [datascript-db var-id]
     (when datascript-db
       (db/var-x-refs datascript-db var-id)))))

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
 (fn [db]
   (db/selected-color db)))

(re-frame/reg-sub
 ::namespace-colors
 (fn [db [_]]
   (db/namespace-colors db)))

(re-frame/reg-sub
 ::project-colors
 (fn [db [_]]
   (db/project-colors db)))

(re-frame/reg-sub
 ::loading?
 (fn [db _]
   (db/loading? db)))

(defn make-function-source-link [var-id node-id src]
  (gstring/format "<a onclick=\"clograms.diagram.entities.add_var_from_link(%d,'%s')\">%s</a>"
                  var-id node-id src))

(defn enhance-source-str
  "Returns a enhanced `source-str` with anchors added for each var that appears on `source-form`"
  [source-str var-id source-form node-id]
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
    (reduce (fn [src {:keys [line column end-column] :as m}]
              (if (and (:var/id m)
                       (not= var-id (:var/id m)))
                (utils/replace-in-str-line (partial make-function-source-link (:var/id m) node-id)
                                                     src
                                                     line
                                                     column
                                                     (- end-column column))
                src))
            source-str
            all-meta-at-origin)))

(re-frame/reg-sub
 ::datascript-db
 (fn [db _]
   (:datascript/db db)))

(re-frame/reg-sub
 ::project-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ proj-id]]
   (db/project-entity datascript-db proj-id)))

(re-frame/reg-sub
 ::namespace-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ ns-id]]
   (db/namespace-entity datascript-db ns-id)))

(re-frame/reg-sub
 ::function-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ var-id node-id]]
   (let [e (db/function-entity datascript-db var-id)]
     (update e :function/source-str enhance-source-str var-id (:function/source-form e) node-id))))

(re-frame/reg-sub
 ::multimethod-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ var-id node-id]]
   (let [e (db/multimethod-entity datascript-db var-id)]
     (update e :multi/methods
             (fn [mm]
               (->> mm
                    (map (fn [method]
                           (update method :multimethod/source-str enhance-source-str var-id (:multimethod/source-form method) node-id)))))))))

(re-frame/reg-sub
 ::node-color
 :<- [::datascript-db]
 :<- [::project-colors]
 :<- [::namespace-colors]
 (fn [[ds-db proj-colors ns-colors] [_ entity]]
   (let [[proj-name ns-name] (case (:entity/type entity)
                               :function (let [ve (db/var-entity ds-db (:var/id entity))]
                                           [(:project/name ve)
                                            (:namespace/name ve)])
                               :multimethod (let [ve (db/var-entity ds-db (:var/id entity))]
                                              [(:project/name ve)
                                               (:namespace/name ve)])
                               :namespace (let [nse (db/namespace-entity ds-db (:namespace/id entity))]
                                            [(-> nse :project/_namespaces :project/name)
                                             (:namespace/name nse)])
                               :project [(:project/name (db/project-entity ds-db (:project/id entity)))
                                         nil])]
     (or (get ns-colors ns-name) (get proj-colors proj-name)))))

(re-frame/reg-sub
 ::node-comment
 (fn [db [_ node-id]]
   (db/node-comment db node-id)))
