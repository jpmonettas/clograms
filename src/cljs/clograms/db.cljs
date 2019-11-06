(ns clograms.db
  (:require [clograms.re-grams.re-grams :as rg]
            [datascript.core :as d]
            [cljs.tools.reader :as tools-reader]))

(def project-browser-transitions
  [:projects :namespaces :vars])

(def project-browser-level-idx->key (->> project-browser-transitions
                                         (map-indexed #(vector %1 %2))
                                         (into {})))
(def project-browser-level-key->idx (->> project-browser-transitions
                                         (map-indexed #(vector %2 %1))
                                         (into {})))

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

(defn select-side-bar-tab [db tab]
  (assoc-in db [:side-bar :selected-side-bar-tab] tab))

(defn update-side-bar-browser-level [db f]
 (update-in db [:projects-browser :level] f))

(defn unselect-node [db]
  (assoc-in db [:diagram :selected-node] nil))

(defn loading? [db]
  (:loading? db))

(def selectable-colors #{"#984545"
                         "#68ba41"
                         "#c5973c"
                         "#afbc4f"})

(def default-db
  (merge
   {:side-bar {:selected-side-bar-tab :projects-browser}
    :projects-browser {:level 0
                       :selected-project nil
                       :selected-namespace nil}
    :ctx-menu nil
    :selected-color (first selectable-colors)
    :loading? true}
   (rg/initial-db)))

;;;;;;;;;;;;;;;;;;;
;; Datascript DB ;;
;;;;;;;;;;;;;;;;;;;

(defn deserialize-datoms [datoms]
  (keep (fn [[eid a v t add?]]
          (try
            (let [deserialized-val (if (or (= a :function/source-form)
                                           (= a :multimethod/source-form))
                                     (tools-reader/read-string v)
                                     v)]
              [(if add? :db/add :db/retract) eid a deserialized-val])
            (catch js/Error e
              (js/console.warn (str "[Skipping] Couldn't parse source-form for function " eid " probably because of reg exp non compatible with JS")))))
        datoms))

(defn deserialize-datascript-db [ds-db-str]
  (try
    (let [{:keys [schema datoms]} (cljs.reader/read-string ds-db-str)
          datoms' (deserialize-datoms datoms)
          conn (d/create-conn schema)]
      (d/transact! conn datoms')
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

(defn var-entity [datascript-db var-id]
  (let [var (d/entity datascript-db var-id)
        func (:var/function var)
        ns (:namespace/_vars var)
        proy (:project/_namespaces ns)]
    {:var/name (:var/name var)
     :var/docstring (:var/docstring var)
     :function/source-form (:function/source-form func)
     :function/source-str (:function/source-str func)
     :function/args (:function/args func)
     :namespace/name (:namespace/name ns)
     :project/name (:project/name proy)}))

(defn all-entities [datascript-db]
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

(defn project-entity [datascript-db project-id]
  (d/entity datascript-db project-id))

(defn namespace-entity [datascript-db project-id]
  (d/entity datascript-db project-id))

(defn all-projects [datascript-db]
  (->> (d/q '[:find ?pid ?pname
              :in $
              :where
              [?pid :project/name ?pname]]
            datascript-db)
       (map #(zipmap [:project/id :project/name] %))))

(defn all-namespaces [datascript-db project-id]
  (->> (d/q '[:find ?pid ?nsid ?nsname
              :in $ ?pid
              :where
              [?pid :project/namespaces ?nsid]
              [?nsid :namespace/name ?nsname]]
            datascript-db
            project-id)
       (map #(zipmap [:project/id :namespace/id :namespace/name] %))))

(defn all-vars [datascript-db ns-id]
  (->> (d/q '[:find ?pid ?nsid ?vid ?vname ?vpub ?vline ?fid ?fsrcf ?fsrcs
              :in $ ?nsid
              :where
              [?pid :project/namespaces ?nsid]
              [?nsid :namespace/vars ?vid]
              [?vid :var/name ?vname]
              [?vid :var/public? ?vpub]
              [?vid :var/line ?vline]
              [?vid :var/function ?fid]
              [?fid :function/source-form ?fsrcf]
              [?fid :function/source-str ?fsrcs]
              #_[?vid :function/_var ?fid]
              #_[(get-else $ ?fid :function/var nil) ?x]
              #_[(get-else $ ?fid :file/name "N/A") ?fname]]
            datascript-db
            ns-id)
       (map #(zipmap [:project/id :namespace/id :var/id :var/name :var/public?
                      :var/line :function/id :function/source-form :function/source-str] %))))

(defn var-x-refs [datascript-db var-id]
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
                      :function/source-str :var/id] %))))
