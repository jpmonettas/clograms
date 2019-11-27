(ns clograms.db
  (:require [clograms.re-grams.re-grams :as rg]
            [datascript.core :as d]
            [cljs.tools.reader :as tools-reader]
            [clojure.string :as str]
            [cognitect.transit :as transit]))

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
  (assoc-in db [:bottom-bar :references] {:node-id node-id
                                          :vars references}))

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

(defn node-comment [db node-id]
  (get-in db [:node/comments node-id]))

(defn set-node-comment [db node-id comment]
  (assoc-in db [:node/comments node-id] comment))

(defn remove-node-comment [db node-id]
  (update db :node/comments dissoc node-id))

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
      (rg/set-link-type :clograms/line-link)
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

(defn deserialize-datoms [datoms]
  (doall (keep (fn [[eid a v t add?]]
                 (when-let [deserialized-val (if (#{:function/source-form
                                                    :multimethod/source-form
                                                    :spec.alpha/source-form
                                                    :fspec.alpha/source-form} a)
                                               (deserialize-source v)
                                               v)]
                   [(if add? :db/add :db/retract) eid a deserialized-val]))
               datoms)))

(defn deserialize-datascript-db [ds-db-str]
  (try
    (let [json-reader (transit/reader :json)
          {:keys [schema datoms]} (time (transit/read json-reader ds-db-str))
          datoms' (time (deserialize-datoms datoms))
          conn (d/create-conn schema)]
      (time (d/transact! conn datoms'))
      (d/db conn))
    (catch js/Error e
      (js/console.error "Couldn't read db" (ex-data e) e))))

(defn add-datoms [datascript-db datoms]
  (let [tx-data (deserialize-datoms datoms)]
    (d/db-with datascript-db tx-data)))

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
     :function/source-form (:function/source-form func)
     :function/source-str (:function/source-str func)
     :fspec.alpha/source-form (:fspec.alpha/source-form (:function/spec.alpha func))
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
                           (select-keys multi-method
                                        [:multimethod/dispatch-val
                                         :multimethod/source-form
                                         :multimethod/source-str]))
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
                 (:var/multi var)    :multimethod)
     :namespace/name (:namespace/name ns)
     :project/name (:project/name proy)}))

(defn project-entity [datascript-db project-id]
  (let [proj (d/entity datascript-db project-id)]
    {:project/id project-id
     :project/name (:project/name proj)
     :project/version (or (:project/version proj) "UNKNOWN")}))

(defn namespace-entity [datascript-db namespace-id]
  (let [ns (d/entity datascript-db namespace-id)]
    {:namespace/id namespace-id
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)
     :namespace/docstring (:namespace/docstring ns)}))

(defn re-frame-subs-entity [datascript-db id]
  (let [sub (d/entity datascript-db id)
        ns (:namespace/_re-frame-subs sub)]
    {:id id
     :re-frame.subs/key (:re-frame.subs/key sub)
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)}))

(defn re-frame-event-entity [datascript-db id]
  (let [e (d/entity datascript-db id)
        ns (:namespace/_re-frame-events e)]
    {:id id
     :re-frame.event/key (:re-frame.event/key e)
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)}))

(defn re-frame-fx-entity [datascript-db id]
  (let [e (d/entity datascript-db id)
        ns (:namespace/_re-frame-fxs e)]
    {:id id
     :re-frame.fx/key (:re-frame.fx/key e)
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)}))

(defn re-frame-cofx-entity [datascript-db id]
  (let [e (d/entity datascript-db id)
        ns (:namespace/_re-frame-cofxs e)]
    {:id id
     :re-frame.cofx/key (:re-frame.cofx/key e)
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)}))

(defn spec-entity [datascript-db spec-id]
  (let [e (d/entity datascript-db spec-id)
        ns (:namespace/_specs-alpha e)]
    {:spec/id spec-id
     :spec.alpha/key (:spec.alpha/key e)
     :spec.alpha/source-form (:spec.alpha/source-form e)
     :project/name (:project/name (:project/_namespaces ns))
     :namespace/name (:namespace/name ns)}))

#_(defn all-entities [datascript-db]
  (->> (d/q '[:find ?pname ?nsname ?vname ?vid ?fsrcf ?fsrcs
              :in $
              :where
              [?vid :var/name ?vname]
              [?nsid :namespace/vars ?vid]
              [?vid :var/function ?fid]
              [?fid :function/source-form ?fsrcf]
              [?fid :function/source-str ?fsrcs]
              [?nsid :namespace/name ?nsname]
              [?pid :project/name ?pname]
              [?pid :project/namespaces ?nsid]]
            datascript-db)
       (map #(zipmap [:project/name :namespace/name :var/name :var/id :function/source-form :function/source-str] %))))

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
                                       (not= "N/A" (:multi var)) :multimethod)
                           :entity/type :var)
                    (dissoc :fn :multi)))))))

(defn var-x-refs [datascript-db var-id]
  (prn "Finding var references for " var-id)
  (->> (d/q '[:find ?pname ?vrnsn ?in-fn ?fsrcf ?fsrcs ?fnvid
              :in $ ?vid
              :where
              [?nid :namespace/vars ?vid]
              [?vid :var/name ?vn]
              [?vid :var/refs ?vrid]
              [?vrid :var-ref/namespace ?vrnid]
              [?vrid :var-ref/in-function ?fnid]
              [?fnvid :var/function ?fnid]
              [?fnid :function/source-form ?fsrcf]
              [?fnid :function/source-str ?fsrcs]
              [?fnvid :var/name ?in-fn]
              [?vrnid :namespace/name ?vrnsn]
              [?pid :project/name ?pname]
              [?pid :project/namespaces ?vrnid]
              [(get-else $ ?fid :file/name "N/A") ?fname]] ;; while we fix the file issue
            datascript-db
            var-id)
       (map #(zipmap [:project/name :namespace/name :var/name :function/source-form
                      :function/source-str :var/id] %))
       (remove #(= (:var/id %) var-id))))

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
