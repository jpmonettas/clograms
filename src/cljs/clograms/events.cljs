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
            [expound.alpha :as expound]))

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

(defn db-loaded [db ds-db]
  (let [datascript-db (cljs.reader/read-string ds-db)
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

(defn build-project-node [entity]
  {:entity entity
   :diagram.node/type :clograms/project-node})

(defn build-namespace-node [entity]
  {:entity entity
   :diagram.node/type :clograms/namespace-node})

(defn build-var-node [entity]
  {:entity (-> entity
               (update :function/source
                       (fn [src]
                         (-> src
                             (str/replace "clojure.core/" "")
                             (str/replace "cljs.core/" "")
                             (zp/zprint-file-str {})))))
   :diagram.node/type :clograms/var-node})

(defn add-entity-to-diagram [db e {:keys [link-to-selected? client-x client-y] :as opts}]
  {:dispatch [::rg/add-node (-> (case (:type e)
                                  :project (build-project-node e)
                                  :namespace (build-namespace-node e)
                                  :var (build-var-node e))
                                (assoc :client-x client-x
                                       :client-y client-y))
              [{:diagram.port/type nil} {:diagram.port/type nil}]]}
  #_{:clograms.diagrams/add-node (cond-> {:entity e
                                          :x (or x 500)
                                          :y (or y 500)
                                          :on-node-selected [::select-node]
                                          :on-node-unselected [::unselect-node]
                                          }

                                   link-to-selected? (assoc :link-to {:id (get-in db [:diagram :selected-node :storm.entity/id])
                                                                      :port  (case (:type e)
                                                                               :call-to :out
                                                                               :called-by :in)}))
     }  )

(defn remove-entity-from-diagram [db id]
  {:dispatch [::rg/remove-node id]})

(defn node-selected [db node]
  (let [entity (:entity node)]
    (case (:type entity)
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
      (db/select-project p)
      (assoc-in [:projects-browser :level] (project-browser-level-key->idx :namespaces))))

(defn side-bar-browser-select-namespace [db ns]
  (-> db
      (db/select-namespace ns)
      (assoc-in [:projects-browser :level] (project-browser-level-key->idx :vars))))

(defn unselect-node [db]
  (assoc-in db [:diagram :selected-node] nil))

(re-frame/reg-event-fx ::initialize-db [inter-check] (fn [_ _] (initial-db)))
(re-frame/reg-event-fx ::reload-db [inter-check] (fn [cofxs [_]] (reload-db)))
(re-frame/reg-event-db ::db-loaded [inter-check] (fn [db [_ new-db]] (db-loaded db new-db)))
(re-frame/reg-event-fx ::add-entity-to-diagram [inter-check] (fn [{:keys [db]} [_ e opts]] (add-entity-to-diagram db e opts)))
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

  (re-frame/dispatch [::add-entity-to-diagram
                      {:type :function
                       :namespace/name "cljs.core"
                       :var/name "map2"}
                      {}])

  (re-frame/dispatch [::add-entity-to-diagram
                      {:type :namespace
                       :namespace/name "cljs.core"
                       :namespace/public-vars []
                       :namespace/private-vars []}
                      {}])
  )
