(ns clograms.subs
  (:require [re-frame.core :as re-frame]
            [datascript.core :as d]
            [clograms.db :refer [project-browser-level-idx->key] :as db]
            [clograms.db.components :as components-db]
            [clograms.re-grams.re-grams :as rg]
            [clograms.utils :as utils]
            [clojure.zip :as zip]
            [goog.string :as gstring]
            [clograms.models :as models]
            [clojure.string :as str]
            [pretty-spec.core :as pspec]))

(re-frame/reg-sub
 ::all-searchable-entities
 (fn [{:keys [:datascript/db]} _]
   (-> []
       (into (map (fn [p] (assoc p :search-str (str (:project/name p))))
                  (db/all-projects db)))
       (into (map (fn [n] (assoc n :search-str (str (:namespace/name n))))
                  (db/all-namespaces db)))
       (into (map (fn [v] (assoc v :search-str (str (:var/name v))))
                  (db/all-vars db)))
       (into (map (fn [s] (assoc s :search-str (str (:spec.alpha/key s))))
                  (db/all-specs db))))))

(re-frame/reg-sub
 ::selected-entity
 (fn [{:keys [selected-entity]} _]
   selected-entity))

(re-frame/reg-sub
 ::datascript-db
 (fn [db _]
   (:datascript/db db)))

;;;;;;;;;;;;;;;;;;;
;; Right sidebar ;;
;;;;;;;;;;;;;;;;;;;

(re-frame/reg-sub
 :accordion/active-item
 (fn [db [_ accordion-id]]
   (components-db/accordion-active-item db accordion-id)))

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

(re-frame/reg-sub
 ::side-bar-search
 (fn [db]
   (db/side-bar-search db)))

(defn project-items [datascript-db]
  (when datascript-db
    (->> (db/all-projects datascript-db)
         (map #(assoc %
                      :type :project
                      :search-name (str (:project/name %))))
         (sort-by (comp str :project/name)))))

(defn namespaces-items [datascript-db pid]
  (when datascript-db
    (->> (db/all-namespaces-for-project datascript-db pid)
         (map #(assoc %
                      :type :namespace
                      :search-name (str (:namespace/name %))))
        (sort-by (comp str :namespace/name)))))

(defn vars-items [datascript-db nsid]
  (when datascript-db
    (->> (db/all-vars-for-ns datascript-db nsid)
         (map #(assoc %
                      :type :var
                      :search-name (str (:var/name %))))
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

(re-frame/reg-sub
 ::side-bar-browser-items+query
 :<- [::side-bar-browser-items]
 :<- [::side-bar-search]
 (fn [[items query] [_]]
   (->> items
        (filter (fn [i]
                  (if (string? (:search-name i))
                    (str/includes? (:search-name i) query)
                    (do
                      (js/console.warn "Wrong search name for item " i)
                      nil)))))))

(re-frame/reg-sub
 ::current-var-references
 (fn [db _]
   (db/current-var-references db)))

(re-frame/reg-sub
 ::bottom-bar
 (fn [db _]
   (db/bottom-bar db)))

(re-frame/reg-sub
 ::ref-frame-feature
 :<- [::datascript-db]
 (fn [datascript-db [_ feature-key]]
   (db/all-re-frame-feature datascript-db feature-key)))

(re-frame/reg-sub
 ::specs
 :<- [::datascript-db]
 :<- [::side-bar-search]
 (fn [[datascript-db query] [_]]
   (->> (db/all-specs datascript-db)
        (filter #(str/includes? (str (:spec.alpha/key %)) query))
        (sort-by (comp str :spec.alpha/key)))))

;; Re-frame features subscriptions
(re-frame/reg-sub
 ::re-frame-feature-tree
 (fn [[_ feature-key]]
   [(re-frame/subscribe [::ref-frame-feature feature-key])
    (re-frame/subscribe [::side-bar-search])])
 (fn [[feature-keys query] [_ feature-key]]
   (->> feature-keys
        (filter #(str/includes? (subs (str (:re-frame/key %)) 1) query))
        (group-by :namespace/name)
        (map (fn [[ns-symb ns-subs]]
               {:type :namespace
                :data {:namespace/name ns-symb
                       :project/name (:project/name (first ns-subs))}
                :childs (map (fn [sub]
                               {:data sub
                                :type feature-key})
                             ns-subs)})))))

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
     (cond-> e
       true                         (update :function/source-str enhance-source-str var-id (:function/source-form e) node-id)
       (:fspec.alpha/source-form e) (assoc  :fspec.alpha/source-str
                                            (with-out-str
                                              (pspec/pprint (:fspec.alpha/source-form e)
                                                            {:ns-aliases {"clojure.spec.alpha" "s"
                                                                          "clojure.core.specs.alpha" "score"
                                                                          "clojure.core" nil}})))))))

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
 ::var-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ id]]
   (db/var-entity datascript-db id)))

(re-frame/reg-sub
 ::re-frame-subs-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ id]]
   (db/re-frame-subs-entity datascript-db id)))

(re-frame/reg-sub
 ::re-frame-event-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ id]]
   (db/re-frame-event-entity datascript-db id)))

(re-frame/reg-sub
 ::re-frame-fx-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ id]]
   (db/re-frame-fx-entity datascript-db id)))

(re-frame/reg-sub
 ::re-frame-cofx-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ id]]
   (db/re-frame-cofx-entity datascript-db id)))

(re-frame/reg-sub
 ::spec-entity
 :<- [::datascript-db]
 (fn [datascript-db [_ spec-id]]
   (let [s (db/spec-entity datascript-db spec-id)]
     (assoc s :spec.alpha/source-str
            (with-out-str
              (pspec/pprint (:spec.alpha/source-form s)
                            {:ns-aliases {"clojure.spec.alpha" "s"
                                          "clojure.core.specs.alpha" "score"
                                          "clojure.core" nil}}))))))

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
                               :var (let [ve (db/var-entity ds-db (:var/id entity))]
                                           [(:project/name ve)
                                            (:namespace/name ve)])
                               :multimethod (let [ve (db/var-entity ds-db (:var/id entity))]
                                              [(:project/name ve)
                                               (:namespace/name ve)])
                               :namespace (let [nse (db/namespace-entity ds-db (:namespace/id entity))]
                                            [(-> nse :project/_namespaces :project/name)
                                             (:namespace/name nse)])
                               :project [(:project/name (db/project-entity ds-db (:project/id entity)))
                                         nil]
                               :re-frame-subs  (let [r (db/re-frame-subs-entity ds-db (:id entity))] [(:project/name r) (:namespace/name r)])
                               :re-frame-event (let [r (db/re-frame-event-entity ds-db (:id entity))] [(:project/name r) (:namespace/name r)])
                               :re-frame-fx    (let [r (db/re-frame-fx-entity ds-db (:id entity))] [(:project/name r) (:namespace/name r)])
                               :re-frame-cofx  (let [r (db/re-frame-cofx-entity ds-db (:id entity))] [(:project/name r) (:namespace/name r)])
                               :spec           (let [s (db/spec-entity ds-db (:spec/id entity))] [(:project/name s) (:namespace/name s)]))]
     (or (get ns-colors ns-name) (get proj-colors proj-name)))))

(re-frame/reg-sub
 :text-edit-modal/event
 (fn [db [_]]
   (components-db/text-edit-modal-event db)))
