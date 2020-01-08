(ns clograms.external
  (:require [ajax.core :as ajax]
            [clograms.db :as db]
            [clograms.re-grams.re-grams :as rg]
            [goog.string :as gstring]))

(defn reload-config [port]
  {:http-xhrio {:method          :get
                :uri             (gstring/format "http://localhost:%s/config" port)
                :timeout         8000
                :response-format (ajax/raw-response-format)
                :on-success      [:clograms.events/config-loaded]
                :on-failure      [:clograms.events/reload-failed]}})

(defn reload-datascript-db [port]
  {:http-xhrio {:method          :get
                :uri             (gstring/format "http://localhost:%s/db" port)
                :timeout         8000
                :response-format (ajax/raw-response-format)
                :on-success      [:clograms.events/db-loaded]
                :on-failure      [:clograms.events/reload-failed]}})

(defn load-diagram [port]
  {:http-xhrio {:method          :get
                :uri             (gstring/format "http://localhost:%s/diagram" port)
                :timeout         8000
                :response-format (ajax/raw-response-format)
                :on-success      [:clograms.events/diagram-loaded]
                :on-failure      [:clograms.events/diagram-load-failed]}})

(defn save-diagram [diagram port]
  {:http-xhrio {:method          :post
                :uri             (gstring/format "http://localhost:%s/diagram" port)
                :timeout         8000
                :params (pr-str diagram)
                :format (ajax/text-request-format)
                :response-format (ajax/raw-response-format)
                :on-success      []
                :on-failure      [:clograms.events/save-failed]}})

(defn db-loaded [db ds-db-str]
  (let [datascript-db (db/deserialize-datascript-db ds-db-str)
        main-project-id (db/main-project-id datascript-db)]
    (js/console.log "Have " (count datascript-db) "datoms. Main project id " main-project-id)
    (assoc db
           :datascript/db datascript-db
           :main-project/id main-project-id
           :loading? false)))

(defn config-loaded [{:keys [db] :as cofxs} config-str]
  (let [config (cljs.reader/read-string config-str)]
   {:db (assoc db :config config)
    :start-websocket config}))

(defn diagram-loaded [db diagram-str]
  (let [diagram (-> (cljs.reader/read-string diagram-str)
                    (update ::rg/diagram (fn [d]
                                           ;; merge whatever diagram we loaded on top of initial diagram
                                           ;; so we can upgrade
                                           ;; It could happen that the stored diagram doesn't contain some new keys needed
                                           ;; by the new version
                                           (merge (::rg/diagram (rg/initial-db)) d))))]
    (-> db
        (merge diagram))))

(defn new-datascript-db-datoms [db datoms]
  (update db :datascript/db db/add-datoms datoms))
