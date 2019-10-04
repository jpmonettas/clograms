(ns clograms.events
  (:require [re-frame.core :as re-frame]
            [clograms.db :as db]
            [reagent.dom :as rdom]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [datascript.core :as d]
            [clograms.db :refer [project-browser-level-key->idx]]
            [clograms.re-grams :as rg]
            [zprint.core :as zp]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clograms.spec :as clograms-spec]
            [expound.alpha :as expound]
            [cljs.tools.reader :as tools-reader]))

;; (d/pull @db-conn '[:project/name {:project/dependency 6}] 2)
;; (d/pull @db-conn '[:project/name :project/dependency] 1)

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (js/Error. (str "spec check failed: " (expound/expound-str a-spec db))))))

;; now we create an interceptor using `after`
(def inter-check (re-frame/after (partial check-and-throw ::clograms-spec/db)))

(defn initial-db []
  {:db db/default-db
   :dispatch-n [[::reload-db]
                [::load-diagram]]})

(defn reload-db []
  {:http-xhrio {:method          :get
                :uri             "http://localhost:3000/db"
                :timeout         8000
                :response-format (ajax/raw-response-format)
                :on-success      [::db-loaded]
                :on-failure      [::reload-failed]}})

(defn load-diagram []
  {:http-xhrio {:method          :get
                :uri             "http://localhost:3000/diagram"
                :timeout         8000
                :response-format (ajax/raw-response-format)
                :on-success      [::diagram-loaded]
                :on-failure      [::diagram-load-failed]}})

(defn save-diagram [diagram]
  {:http-xhrio {:method          :post
                :uri             "http://localhost:3000/diagram"
                :timeout         8000
                :params (pr-str diagram)
                :format (ajax/text-request-format)
                :response-format (ajax/raw-response-format)
                :on-success      []
                :on-failure      [:save-failed]}})

(defn deserialize-datascript-db [ds-db-str]
  (try
    (let [{:keys [schema datoms]} (cljs.reader/read-string ds-db-str)
          datoms' (keep (fn [[eid a v]]
                          (try
                            (let [deserialized-val (if (= a :function/source-form)
                                                     (tools-reader/read-string v)
                                                     v)]
                              [:db/add eid a deserialized-val])
                            (catch js/Error e
                              (js/console.warn (str "[Skipping] Couldn't parse source-form for function " eid " probably because of reg exp non compatible with JS")))))
                        datoms)
          conn (d/create-conn schema)]
      (d/transact! conn datoms')
      (d/db conn))
    (catch js/Error e
      (js/console.error "Couldn't read db" (ex-data e) e))))

(defn db-loaded [db ds-db-str]
  (let [datascript-db (deserialize-datascript-db ds-db-str)
        main-project-id (d/q '[:find ?pid .
                               :in $ ?pn
                               :where [?pid :project/name ?pn]]
                             datascript-db
                             'clindex/main-project)]
    (js/console.log "Have " (count datascript-db) "datoms. Main project id " main-project-id)
    (assoc db
           :datascript/db datascript-db
           :main-project/id main-project-id
           :loading? false)))

(defn diagram-loaded [db diagram-str]
  (let [diagram (cljs.reader/read-string diagram-str)]
    (merge db diagram)))

(defmulti build-node (fn [entity-type id] entity-type))

(defmethod build-node :project
  [entity-type id]
  {:entity {:entity/type :project
            :project/id id}
   :diagram.node/type :clograms/project-node})

(defmethod build-node :namespace
  [entity-type id]
  {:entity {:entity/type :namespace
            :namespace/id id}
   :diagram.node/type :clograms/namespace-node})

(defn add-var-from-link [var-id]
  (re-frame/dispatch [::add-entity-to-diagram :var var-id {:link-to :last}]))

(defmethod build-node :var
  [entity-type id]
  {:entity {:entity/type :var
            :var/id id}
   :diagram.node/type :clograms/var-node})

(defn add-entity-to-diagram [db entity-type id {:keys [link-to client-x client-y] :as opts}]
  (println "Adding entity to diagram" entity-type id " link to " link-to)
  (let [new-node-id (rg/gen-random-id)
        port-in-id (rg/gen-random-id)
        port-out-id (rg/gen-random-id)
        selected-node (rg/selected-node db)]
    {:dispatch-n (cond-> [[::rg/add-node (-> (build-node entity-type id)
                                             (assoc :client-x client-x
                                                    :client-y client-y
                                                    ::rg/id new-node-id))
                           [{:diagram.port/type nil ::rg/id port-in-id} {:diagram.port/type nil ::rg/id port-out-id}]]]
                   (and link-to selected-node) (into [[::rg/add-link

                                                       (let [port-sel-fn ({:first first :last last} link-to)
                                                             port-id (-> (rg/node-ports selected-node) vals port-sel-fn ::rg/id)]
                                                         [(::rg/id selected-node) port-id])

                                                       [new-node-id (if (= :first link-to)
                                                                      port-out-id
                                                                      port-in-id)]]]))}))

(defn remove-entity-from-diagram [db id]
  {:dispatch [::rg/remove-node id]})

(defn node-selected [db node]
  (let [entity (:entity node)]
    (case (:entity/type entity)
      :var {:db db
            :dispatch [::select-side-bar-tab :selected-browser]}
      :project {:db db
                :dispatch-n [[::select-side-bar-tab :projects-browser]
                             [::side-bar-browser-select-project entity]]}
      :namespace {:db (db/select-project db entity)
                  :dispatch-n [[::select-side-bar-tab :projects-browser]
                               [::side-bar-browser-select-namespace entity]]}
      {})))


(defn show-context-menu [db ctx-menu]
  (db/set-ctx-menu db ctx-menu))

(defn select-color [db color]
  (db/select-color db color))

(defn set-namespace-color [db ns-name]
  (db/set-namespace-color db
                    ns-name
                    (db/selected-color db)))

(defn set-project-color [db project-name]
  (db/set-project-color db
                        project-name
                        (db/selected-color db)))

(defn hide-context-menu [db]
  (db/set-ctx-menu db nil))

(defn select-side-bar-tab [db tab]
  (assoc-in db [:side-bar :selected-side-bar-tab] tab))

(defn side-bar-browser-back [db]
  (update-in db [:projects-browser :level] #(max (dec %) 0)))

(defn side-bar-browser-select-project [db p]
  (-> db
      (db/select-project (:project/id p))
      (assoc-in [:projects-browser :level] (project-browser-level-key->idx :namespaces))))

(defn side-bar-browser-select-namespace [db ns]
  (-> db
      (db/select-namespace (:namespace/id ns))
      (assoc-in [:projects-browser :level] (project-browser-level-key->idx :vars))))

(defn unselect-node [db]
  (assoc-in db [:diagram :selected-node] nil))

(re-frame/reg-event-fx ::initialize-db [inter-check] (fn [_ _] (initial-db)))
(re-frame/reg-event-fx ::reload-db [inter-check] (fn [cofxs [_]] (reload-db)))
(re-frame/reg-event-db ::db-loaded [inter-check] (fn [db [_ new-db]] (db-loaded db new-db)))
(re-frame/reg-event-fx ::add-entity-to-diagram [inter-check] (fn [{:keys [db]} [_ et id opts]] (add-entity-to-diagram db et id opts)))
(re-frame/reg-event-fx ::remove-entity-from-diagram [inter-check] (fn [{:keys [db]} [_ id]] (remove-entity-from-diagram db id)))
(re-frame/reg-event-fx ::rg/node-selected [inter-check] (fn [{:keys [db]} [_ node]] (node-selected db node)))
(re-frame/reg-event-db ::show-context-menu [inter-check] (fn [db [_ ctx-menu]] (show-context-menu db ctx-menu)))
(re-frame/reg-event-db ::select-color [inter-check] (fn [db [_ color]] (select-color db color)))
(re-frame/reg-event-db ::set-namespace-color [inter-check] (fn [db [_ ns-name]] (set-namespace-color db ns-name)))
(re-frame/reg-event-db ::set-project-color [inter-check] (fn [db [_ project-name]] (set-project-color db project-name)))
(re-frame/reg-event-db ::hide-context-menu [inter-check] (fn [db [_]] (hide-context-menu db)))
(re-frame/reg-event-db ::select-side-bar-tab [inter-check] (fn [db [_ tab]] (select-side-bar-tab db tab)))
(re-frame/reg-event-db ::side-bar-browser-back [inter-check] (fn [db _] (side-bar-browser-back db)))
(re-frame/reg-event-db ::side-bar-browser-select-project [inter-check] (fn [db [_ p]] (side-bar-browser-select-project db p)))
(re-frame/reg-event-db ::side-bar-browser-select-namespace [inter-check] (fn [db [_ ns]] (side-bar-browser-select-namespace db ns)))
(re-frame/reg-event-db ::unselect-node [inter-check] (fn [db [_]] (unselect-node db)))
(re-frame/reg-event-fx ::load-diagram [inter-check] (fn [_ _] (load-diagram)))
(re-frame/reg-event-db ::diagram-loaded [inter-check] (fn [db [_ diagram]] (diagram-loaded db diagram)))
(re-frame/reg-event-fx ::save-diagram [] (fn [cofxs _] (save-diagram (select-keys (:db cofxs)
                                                                                  [::rg/diagram
                                                                                   :project/colors
                                                                                   :namespace/colors]))))



(comment

  ;; we can update datascript-db like this and everything will react accordingly
  (re-frame/reg-event-db
   ::update-datascript
   (fn [db _]
     (update db :datascript/db (fn [ds-db]
                                 (d/db-with ds-db [[:db/add 341897743 :project/name "Something else"]])))))

  (re-frame/dispatch [::update-datascript])


  )
