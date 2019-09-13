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
            [clojure.string :as str]))

;; (d/pull @db-conn '[:project/name {:project/dependency 6}] 2)
;; (d/pull @db-conn '[:project/name :project/dependency] 1)


(re-frame/reg-event-fx
 ::initialize-db
 (fn [_ _]
   {:db db/default-db
    :dispatch [::reload-db]}))

(re-frame/reg-event-fx
 ::reload-db
 (fn [cofxs [_]]
   {:http-xhrio {:method          :get
                 :uri             "http://localhost:3000/db"
                 :timeout         8000 ;; optional see API docs
                 :response-format (ajax/raw-response-format) ;; IMPORTANT!: You must provide this.
                 :on-success      [::db-loaded]
                 :on-failure      [:bad-http-result]}}))

(re-frame/reg-event-db
 ::db-loaded
 (fn [db [_ new-db]]
   (let [datascript-db (cljs.reader/read-string new-db)
         main-project-id (d/q '[:find ?pid .
                                :in $ ?pn
                                :where [?pid :project/name ?pn]]
                              datascript-db
                              'clindex/main-project)]
     (js/console.log "Have " (count datascript-db) "datoms. Main project id " main-project-id)
     (assoc db
            :datascript/db datascript-db
            :main-project/id main-project-id))))

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

(re-frame/reg-event-fx
 ::add-entity-to-diagram
 (fn [{:keys [db]} [_ e {:keys [link-to-selected? client-x client-y] :as opts}]]
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
      }))

(re-frame/reg-event-fx
 ::remove-entity-from-diagram
 (fn [{:keys [db]} [_ id]]
   {:dispatch [::rg/remove-node id]}))


(comment
  (re-frame/dispatch [::set-node-properties (first (keys (:nodes (:diagram @re-frame.db/app-db)))) "red"])
  )


(re-frame/reg-event-fx
 ::rg/node-selected
 (fn [{:keys [db]} [_ node]]
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
       {}))))

(re-frame/reg-event-db
 ::show-context-menu
 (fn [db [_ ctx-menu]]
   (db/set-ctx-menu db ctx-menu)))

(re-frame/reg-event-db
 ::select-color
 (fn [db [_ color]]
   (db/select-color db color)))

(re-frame/reg-event-db
 ::hide-context-menu
 (fn [db [_]]
   (db/set-ctx-menu db nil)))

(re-frame/reg-event-db
 ::select-side-bar-tab
 (fn [db [_ tab]]
   (assoc-in db [:side-bar :selected-side-bar-tab] tab)))

(re-frame/reg-event-db
 ::side-bar-browser-back
 (fn [db _]
   (update-in db [:projects-browser :level] #(max (dec %) 0))))

(re-frame/reg-event-db
 ::side-bar-browser-select-project
 (fn [db [_ p]]
   (-> db
       (db/select-project p)
       (assoc-in [:projects-browser :level] (project-browser-level-key->idx :namespaces)))))

(re-frame/reg-event-db
 ::side-bar-browser-select-namespace
 (fn [db [_ ns]]
   (-> db
       (db/select-namespace ns)
       (assoc-in [:projects-browser :level] (project-browser-level-key->idx :vars)))))

(re-frame/reg-event-db
 ::unselect-node
 (fn [db [_ e]]
   (assoc-in db [:diagram :selected-node] nil)))


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
