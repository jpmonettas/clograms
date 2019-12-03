(ns clograms.events
  (:require [re-frame.core :as re-frame]
            [clograms.db :as db]
            [clograms.db.components :as components-db]
            [reagent.dom :as rdom]
            [day8.re-frame.http-fx]
            [datascript.core :as d]
            [clograms.db :refer [project-browser-level-key->idx]]
            [clograms.re-grams.re-grams :as rg]
            [zprint.core :as zp]
            [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [clograms.spec :as clograms-spec]
            [expound.alpha :as expound]
            [clograms.models :as models]
            [clograms.external :as external]
            [clograms.diagram.selection :as selection]
            [clograms.diagram.entities :as entities]
            [clograms.diagram.tools :as tools]
            [clograms.browser :as browser]
            [clograms.menues :as menues]))


(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  #_(when-not (s/valid? a-spec db)
    (throw (js/Error. (str "spec check failed: " (expound/expound-str a-spec db))))))

;; now we create an interceptor using `after`
(def inter-check (re-frame/after (partial check-and-throw ::clograms-spec/db)))

(defn initialize-db-and-load []
  {:db db/default-db
   :dispatch-n [[::reload-db]
                [::load-diagram]]})

(re-frame/reg-event-fx ::initialize-db [inter-check] (fn [_ _] (initialize-db-and-load)))
(re-frame/reg-event-fx ::reload-db [inter-check] (fn [cofxs [_]] (external/reload-datascript-db)))
(re-frame/reg-event-db ::new-datoms [] (fn [db [_ new-datoms]] (external/new-datascript-db-datoms db new-datoms)))
(re-frame/reg-event-db ::db-loaded [inter-check] (fn [db [_ new-db]] (external/db-loaded db new-db)))
(re-frame/reg-event-fx ::add-entity-to-diagram [inter-check] (fn [{:keys [db]} [_ et id opts]] (entities/add-entity-to-diagram db et id opts)))
(re-frame/reg-event-fx ::remove-entity-from-diagram [inter-check] (fn [{:keys [db]} [_ id]] (entities/remove-entity-from-diagram db id)))
(re-frame/reg-event-fx ::rg/node-selected [inter-check] (fn [{:keys [db]} [_ node]] (selection/node-selected db node)))
(re-frame/reg-event-db ::set-node-comment [inter-check] (fn [db [_ node-id comment]]
                                                          (rg/set-node-extra-data
                                                           db node-id
                                                           (assoc (rg/node-extra-data db node-id) :comment comment))))
(re-frame/reg-event-db ::set-node-label [inter-check] (fn [db [_ node-id label]]
                                                        (rg/set-node-extra-data
                                                         db node-id
                                                         (assoc (rg/node-extra-data db node-id) :label label))))

(re-frame/reg-event-db ::remove-node-comment [inter-check] (fn [db [_ node-id comment]]
                                                             (rg/set-node-extra-data
                                                              db node-id
                                                              (dissoc (rg/node-extra-data db node-id) :comment))))


(re-frame/reg-event-db ::toggle-collapse-node [inter-check] (fn [db [_ node-id]]
                                                              (rg/set-node-extra-data db node-id
                                                                                      (update (rg/node-extra-data db node-id) :collapsed? not))))

(re-frame/reg-event-db ::show-context-menu [inter-check] (fn [db [_ ctx-menu]] (menues/show-context-menu db ctx-menu)))
(re-frame/reg-event-db ::hide-context-menu [inter-check] (fn [db [_]] (menues/hide-context-menu db)))
(re-frame/reg-event-db ::select-color [inter-check] (fn [db [_ color]] (tools/select-color db color)))
(re-frame/reg-event-db ::set-namespace-color [inter-check] (fn [db [_ ns-name]] (tools/set-namespace-color db ns-name)))
(re-frame/reg-event-db ::set-project-color [inter-check] (fn [db [_ project-name]] (tools/set-project-color db project-name)))
(re-frame/reg-event-db ::find-var-references [inter-check] (fn [db [_ var-id node-id]] (tools/find-var-references db var-id node-id)))
(re-frame/reg-event-db ::side-bar-browser-back [inter-check] (fn [db _] (browser/side-bar-browser-back db)))
(re-frame/reg-event-db ::side-bar-browser-select-project [inter-check] (fn [db [_ p]]
                                                                         (-> db
                                                                             (browser/side-bar-browser-select-project p)
                                                                             (db/set-side-bar-search ""))))
(re-frame/reg-event-db ::side-bar-browser-select-namespace [inter-check] (fn [db [_ ns]]
                                                                           (-> db
                                                                               (browser/side-bar-browser-select-namespace ns)
                                                                               (db/set-side-bar-search ""))))
(re-frame/reg-event-db ::side-bar-set-search [inter-check] (fn [db [_ query]] (db/set-side-bar-search db query)))
(re-frame/reg-event-db ::toggle-bottom-bar-collapse [inter-check] (fn [db _] (db/toggle-bottom-bar-collapse db)))
(re-frame/reg-event-fx ::load-diagram [inter-check] (fn [_ _] (external/load-diagram)))
(re-frame/reg-event-db ::diagram-loaded [inter-check] (fn [db [_ diagram]] (external/diagram-loaded db diagram)))
(re-frame/reg-event-fx ::save-diagram [] (fn [cofxs _] (external/save-diagram (select-keys (:db cofxs)
                                                                                           [::rg/diagram
                                                                                            :project/colors
                                                                                            :namespace/colors
                                                                                            :node/comments]))))

(re-frame/reg-event-db :accordion/activate-item [inter-check] (fn [db [_ accordion-id item-id]] (components-db/accordion-activate-item db accordion-id item-id)))

(re-frame/reg-event-db :text-edit-modal/create [inter-check] (fn [db [_ event]] (components-db/text-edit-modal-create db event)))

(re-frame/reg-event-fx :text-edit-modal/set [inter-check]
                       (fn [{:keys [db]} [_ event]]
                         {:db (components-db/text-edit-modal-kill db)
                          :dispatch event}))




(comment

  ;; we can update datascript-db like this and everything will react accordingly
  (re-frame/reg-event-db
   ::update-datascript
   (fn [db _]
     (update db :datascript/db (fn [ds-db]
                                 (d/db-with ds-db [[:db/add 341897743 :project/name "Something else"]])))))

  (re-frame/dispatch [::update-datascript])


  )
