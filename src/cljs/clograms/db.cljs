(ns clograms.db
  (:require [clograms.re-grams.re-grams :as rg]
            [datascript.core :as d]
            [cljs.tools.reader :as tools-reader]
            [clojure.string :as str]
            [cognitect.transit :as transit]
            [goog.string :as gstring]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Re-frame DB                                                                 ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def project-browser-transitions
  [:projects :namespaces :vars])

(def project-browser-level-idx->key (->> project-browser-transitions
                                         (map-indexed #(vector %1 %2))
                                         (into {})))
(def project-browser-level-key->idx (->> project-browser-transitions
                                         (map-indexed #(vector %2 %1))
                                         (into {})))

(defn diagram [db]
  (::rg/diagram db))

(defn select-project [db p-id]
  (assoc-in db [:projects-browser :selected-project] p-id))

(defn selected-project [db]
  (get-in db [:projects-browser :selected-project]))

(defn select-namespace [db ns-id]
  (assoc-in db [:projects-browser :selected-namespace] ns-id))

(defn selected-namespace [db]
  (get-in db [:projects-browser :selected-namespace]))

(defn set-ctx-menu [db ctx-menu]
  (assoc db :ctx-menu ctx-menu))

(defn select-color [db color]
  (assoc db :selected-color color))

(defn selected-color [db]
  (get db :selected-color))

(defn set-namespace-color [db ns color]
  (assoc-in db [:namespace/colors ns] color))

(defn namespace-colors [db]
  (get db :namespace/colors))

(defn set-project-color [db project color]
  (assoc-in db [:project/colors project] color))

(defn project-colors [db]
  (get db :project/colors))

(defn set-var-references [db node-id references]
  (assoc-in db [:bottom-bar :references] (cond-> {:vars references}
                                           node-id (assoc :node-id node-id))))

(defn current-var-references [db]
  (get-in db [:bottom-bar :references]))

(defn set-bottom-bar-title [db title]
  (assoc-in db [:bottom-bar :title] title))

(defn bottom-bar-title [db]
  (get-in db [:bottom-bar :title]))

(defn bottom-bar [db]
  (get db :bottom-bar))

(defn toggle-bottom-bar-collapse [db]
  (update-in db [:bottom-bar :collapsed?] not))

(defn uncollapse-bottom-bar [db]
  (assoc-in db [:bottom-bar :collapsed?] false))

(defn set-side-bar-search [db query]
  (assoc-in db [:side-bar :query] query))

(defn side-bar-search [db]
  (get-in db [:side-bar :query]))

(defn update-side-bar-browser-level [db f]
 (update-in db [:projects-browser :level] f))

(defn loading? [db]
  (:loading? db))

(def selectable-colors #{"#984545"
                         "#68ba41"
                         "#c5973c"
                         "#afbc4f"})

(def default-db
  (-> (rg/initial-db)
      (merge {:side-bar {:query ""}
              :bottom-bar {:collapsed? true}
              :projects-browser {:level 0
                                 :selected-project nil
                                 :selected-namespace nil}
              :ctx-menu nil
              :selected-color (first selectable-colors)
              :loading? true})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Datascript DB                                                               ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn deserialize-source [src-str]
  (try
    (tools-reader/read-string src-str)
    (catch js/Error e
      (js/console.warn (str "[Skipping] Couldn't parse source-form " (subs src-str 0 30) "... probably because of reg exp non compatible with JS"))
      nil)))

(defn deserialize-datascript-db [ds-db-str]
  (try
    (let [json-reader (transit/reader :json)
          {:keys [schema datoms]} (transit/read json-reader ds-db-str)
          conn (d/create-conn schema)]
      (time (d/transact! conn datoms))
      (d/db conn))
    (catch js/Error e
      (js/console.error "Couldn't read db" (ex-data e) e))))

(defn add-datoms [datascript-db datoms]
  (d/db-with datascript-db datoms))

(defn main-project-id [datascript-db]
  (d/q '[:find ?pid .
         :in $ ?pn
         :where [?pid :project/name ?pn]]
       datascript-db
       'clindex/main-project))

(defn function-entity [datascript-db var-id]
  (let [var (d/entity datascript-db var-id)
        func (:var/function var)
        ns (:namespace/_vars var)
        proy (:project/_namespaces ns)]
    {:var/name (:var/name var)
     :var/docstring (:var/docstring var)
     :function/source-form (deserialize-source (:source/form func))
     :function/source-str (:source/str func)
     :fspec.alpha/source-form (deserialize-source (:source/form (:function/spec.alpha func)))
     :function/args (:function/args func)
     :namespace/name (:namespace/name ns)
     :project/name (:project/name proy)}))

(defn multimethod-entity [datascript-db var-id]
  (let [var (d/entity datascript-db var-id)
        multi (:var/multi var)
        ns (:namespace/_vars var)
        proy (:project/_namespaces ns)]
    {:var/name (:var/name var)
     :var/docstring (:var/docstring var)
     :multi/dispatch-form (:multi/dispatch-form multi)
     :multi/methods (map (fn [multi-method]
                           (let [mm (select-keys multi-method
                                                 [:multimethod/dispatch-val
                                                  :source/form
                                                  :source/str])]
                             (-> mm
                                 (assoc :multimethod/source-form (deserialize-source (:source/form mm)))
                                 (assoc :multimethod/source-str (:source/str mm)))))
                           (:multi/methods multi))
     :namespace/name (:namespace/name ns)
     :project/name (:project/name proy)}))

(defn var-entity [datascript-db var-id]
  (let [var (d/entity datascript-db var-id)
        ns (:namespace/_vars var)
        proy (:project/_namespaces ns)]
    {:var/name (:var/name var)
     :var/docstring (:var/docstring var)
     :var/type (cond
                 (:var/function var) :function
                 (:var/multi var)    :multimethod
                 :else               :var)
     :namespace/name (:namespace/name ns)
     :project/name (:project/name proy)}))

(defn project-entity [datascript-db project-id]
  (let [proj (d/entity datascript-db project-id)]
    {:project/id project-id
     :project/name (:project/name proj)
     :project/version (or (:project/version proj) "UNKNOWN")}))

(defn namespace-entity [datascript-db namespace-id]
  (let [ns (d/entity datascript-db namespace-id)
        ns-proj (:project/_namespaces ns)]
    {:namespace/id namespace-id
     :project/name (:project/name ns-proj)
     :project/id (:db/id ns-proj)
     :namespace/name (:namespace/name ns)
     :namespace/docstring (:namespace/docstring ns)}))

(defn re-frame-subs-entity [datascript-db id]
  (let [sub (d/entity datascript-db id)
        ns (:namespace/_re-frame-subs sub)]
    {:id id
     :re-frame.subs/key (:re-frame.subs/key sub)
     :source/form (deserialize-source (:source/form sub))
     :source/str (:source/str sub)
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)}))

(defn re-frame-event-entity [datascript-db id]
  (let [e (d/entity datascript-db id)
        ns (:namespace/_re-frame-events e)]
    {:id id
     :re-frame.event/key (:re-frame.event/key e)
     :source/form (deserialize-source (:source/form e))
     :source/str (:source/str e)
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)}))

(defn re-frame-fx-entity [datascript-db id]
  (let [e (d/entity datascript-db id)
        ns (:namespace/_re-frame-fxs e)]
    {:id id
     :re-frame.fx/key (:re-frame.fx/key e)
     :source/form (deserialize-source (:source/form e))
     :source/str (:source/str e)
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)}))

(defn re-frame-cofx-entity [datascript-db id]
  (let [e (d/entity datascript-db id)
        ns (:namespace/_re-frame-cofxs e)]
    {:id id
     :re-frame.cofx/key (:re-frame.cofx/key e)
     :project/name (:project/name (:project/_namespaces ns))
     :source/form (deserialize-source (:source/form e))
     :source/str (:source/str e)
     :namespace/name (:namespace/name ns)}))

(defn spec-entity [datascript-db spec-id]
  (let [e (d/entity datascript-db spec-id)
        ns (:namespace/_specs-alpha e)]
    {:spec/id spec-id
     :spec.alpha/key (:spec.alpha/key e)
     :spec.alpha/source-form (deserialize-source (:source/form e))
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)}))

(defn all-projects [datascript-db]
  (->> (d/q '[:find ?pid ?pname ?pver
              :in $
              :where
              [?pid :project/name ?pname]
              [(get-else $ ?pid :project/version "UNKNOWN") ?pver]]
            datascript-db)
       (map #(zipmap [:project/id :project/name :project/version] %))
       (map #(assoc % :entity/type :project))))

(defn all-namespaces [datascript-db]
  (->> (d/q '[:find ?pname ?nsid ?nsname
              :in $
              :where
              [?pid :project/namespaces ?nsid]
              [?pid :project/name ?pname]
              [?nsid :namespace/name ?nsname]]
            datascript-db)
       (map #(zipmap [:project/name :namespace/id :namespace/name] %))
       (map #(assoc % :entity/type :namespace))))


(defn all-namespaces-for-project [datascript-db project-id]
  (->> (d/q '[:find ?pid ?nsid ?nsname
              :in $ ?pid
              :where
              [?pid :project/namespaces ?nsid]
              [?nsid :namespace/name ?nsname]]
            datascript-db
            project-id)
       (map #(zipmap [:project/id :namespace/id :namespace/name] %))
       (map #(assoc % :entity/type :namespace))))

(defn all-vars [datascript-db]
  (->> (d/q '[:find ?pname ?nsname ?vid ?vname
              :in $
              :where
              [?pid :project/namespaces ?nsid]
              [?nsid :namespace/vars ?vid]
              [?vid :var/name ?vname]
              [?pid :project/name ?pname]
              [?nsid :namespace/name ?nsname]]
            datascript-db)
       (map (fn [data]
              (let [var (zipmap [:project/name :namespace/name :var/id :var/name] data)]
                (assoc var :entity/type :var))))))

(defn all-vars-for-ns
  [datascript-db ns-id]
  (->> (d/q '[:find ?pid ?nsid ?vid ?vname ?vpub ?vline ?fid ?mid
              :in $ ?nsid
              :where
              [?pid :project/namespaces ?nsid]
              [?nsid :namespace/vars ?vid]
              [?vid :var/name ?vname]
              [?vid :var/public? ?vpub]
              [?vid :var/line ?vline]
              [(get-else $ ?vid :var/function "N/A") ?fid]
              [(get-else $ ?vid :var/multi "N/A") ?mid]]
            datascript-db
            ns-id)
       (map (fn [data]
              (let [var (zipmap [:project/id :namespace/id :var/id :var/name :var/public? :var/line :fn :multi] data)]
                (-> var
                    (assoc :var/type (cond
                                       (not= "N/A" (:fn var))    :function
                                       (not= "N/A" (:multi var)) :multimethod
                                       :else :var)
                           :entity/type :var)
                    (dissoc :fn :multi)))))))

(defn var-x-refs [datascript-db var-id]
  (->> (d/q '[:find ?pname ?vrnsn ?in-fn ?fsrcs ?fnvid
              :in $ ?vid
              :where
              [?nid :namespace/vars ?vid]
              [?vid :var/name ?vn]
              [?vid :var/refs ?vrid]
              [?vrid :var-ref/namespace ?vrnid]
              [?vrid :var-ref/in-function ?fnid]
              [?fnvid :var/function ?fnid]
              [?fnid :source/str ?fsrcs]
              [?fnvid :var/name ?in-fn]
              [?vrnid :namespace/name ?vrnsn]
              [?pid :project/name ?pname]
              [?pid :project/namespaces ?vrnid]
              [(get-else $ ?fid :file/name "N/A") ?fname]] ;; while we fix the file issue
            datascript-db
            var-id)
       (map #(zipmap [:project/name :namespace/name :var/name
                      :function/source-str :var/id] %))
       (remove #(= (:var/id %) var-id))))

(defn non-test-fns-ref-counts-in-project [datascript-db project-id]
  (->> (d/q '[:find ?pname ?nsname ?vname ?vid ?fspec (count ?vref)
              :in $ ?pid
              :where
              [?pid :project/name ?pname]
              [?pid :project/namespaces ?nsid]
              [?nsid :namespace/name ?nsname]
              [?nsid :namespace/vars ?vid]
              [?vid :var/name ?vname]
              [?vid :var/function ?fid]
              [(get-else $ ?fid :function/spec.alpha "N/A") ?fspec]
              [?vid :var/refs ?vref]
              [?vref :var-ref/namespace ?vrefns]
              [?pid :project/namespaces ?vrefns]
              [?vrefns :namespace/name ?vrefnsname]
              [(str ?vrefnsname) ?vrefnsnamestr]
              (not [(clojure.string/includes? ?vrefnsnamestr "test")])]
            datascript-db
            project-id)
       (map (fn [row]
              (let [vr (zipmap [:project/name :namespace/name :var/name :var/id :spec :count] row)]
                (assoc vr :variadic-count (-> (d/entity datascript-db (:var/id vr))
                                              :var/function
                                              :function/args
                                              count)))))))

(defn all-re-frame-feature [datascript-db feature-key]
  (->> (d/q '[:find ?subid ?subk ?nsn ?pn
              :in $ ?key ?nskey
              :where
              [?subid ?key ?subk]
              [?nsid ?nskey ?subid]
              [?nsid :namespace/name ?nsn]
              [?pid :project/namespaces ?nsid]
              [?pid :project/name ?pn]]
            datascript-db
            (case feature-key
              :re-frame-subs :re-frame.subs/key
              :re-frame-event :re-frame.event/key
              :re-frame-fx :re-frame.fx/key
              :re-frame-cofx :re-frame.cofx/key)
            (case feature-key
              :re-frame-subs :namespace/re-frame-subs
              :re-frame-event :namespace/re-frame-events
              :re-frame-fx :namespace/re-frame-fxs
              :re-frame-cofx :namespace/re-frame-cofxs))
       (map #(zipmap [:id :re-frame/key :namespace/name :project/name] %))
       (map #(assoc % :entity/type feature-key))))

(defn all-specs [datascript-db]
  (->> (d/q '[:find ?sid ?sk
              :in $
              :where
              [?sid :spec.alpha/key ?sk]]
            datascript-db)
       (map #(zipmap [:spec/id :spec.alpha/key] %))
       (map #(assoc % :entity/type :spec))))

(defn find-project-protocols [datascript-db project-id]
  (->> (d/q '[:find ?pname ?nsname ?vid ?vname
              :in $ ?pid
              :where
              [?pid :project/namespaces ?nsid]
              [?pid :project/name ?pname]
              [?nsid :namespace/vars ?vid]
              [?nsid :namespace/name ?nsname]
              [?vid :var/name ?vname]
              [?vid :var/protocol? true]]
            datascript-db
            project-id)
       (map #(zipmap [:project/name :namespace/name :var/id :var/name] %))))

(defn find-project-multimethods [datascript-db project-id]
  (->> (d/q '[:find ?pname ?nsname ?vid ?vname
              :in $ ?pid
              :where
              [?pid :project/namespaces ?nsid]
              [?pid :project/name ?pname]
              [?nsid :namespace/vars ?vid]
              [?nsid :namespace/name ?nsname]
              [?vid :var/name ?vname]
              [?vid :var/multi]]
            datascript-db
            project-id)
       (map #(zipmap [:project/name :namespace/name :var/id :var/name] %))))
