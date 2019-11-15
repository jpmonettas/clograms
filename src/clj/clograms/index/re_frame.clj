(ns clograms.index.re-frame
  (:require [clindex.forms-facts :as forms-facts]
            [clindex.utils :as clindex-utils]))

(def extra-schema
  {:re-frame.event/key        {:db/cardinality :db.cardinality/one}
   :namespace/re-frame-events {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref :db/isComponent true}

   :re-frame.subs/key         {:db/cardinality :db.cardinality/one}
   :namespace/re-frame-subs   {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref :db/isComponent true}

   :re-frame.fx/key           {:db/cardinality :db.cardinality/one}
   :namespace/re-frame-fxs    {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref :db/isComponent true}

   :re-frame.cofx/key         {:db/cardinality :db.cardinality/one}
   :namespace/re-frame-cofxs  {:db/cardinality :db.cardinality/many :db/valueType :db.type/ref :db/isComponent true}
   })


(defn events-facts [ctx [_ ev-key :as form]]
  (let [ev-id (clindex-utils/stable-id :re-frame :event ev-key)]
    {:facts [[:db/add ev-id :re-frame.event/key ev-key]
             [:db/add (clindex-utils/namespace-id (:namespace/name ctx)) :namespace/re-frame-events ev-id]]
     :ctx ctx}))

(defmethod forms-facts/form-facts 're-frame.core/reg-event-db
  [all-ns-map ctx form]
  (events-facts ctx form))

(defmethod forms-facts/form-facts 're-frame.core/reg-event-fx
  [all-ns-map ctx form]
  (events-facts ctx form))

(defmethod forms-facts/form-facts 're-frame.core/reg-sub
  [all-ns-map ctx [_ subs-key :as form]]
  (let [subs-id (clindex-utils/stable-id :re-frame :subs subs-key)]
    {:facts [[:db/add subs-id :re-frame.subs/key subs-key]
             [:db/add (clindex-utils/namespace-id (:namespace/name ctx)) :namespace/re-frame-subs subs-id]]
     :ctx ctx}))

(defmethod forms-facts/form-facts 're-frame.core/reg-fx
  [all-ns-map ctx [_ fx-key :as form]]
  (let [fx-id (clindex-utils/stable-id :re-frame :fx fx-key)]
    {:facts [[:db/add fx-id :re-frame.fx/key fx-key]
             [:db/add (clindex-utils/namespace-id (:namespace/name ctx)) :namespace/re-frame-fxs fx-id]]
     :ctx ctx}))

(defmethod forms-facts/form-facts 're-frame.core/reg-cofx
  [all-ns-map ctx [_ cofx-key :as form]]
  (let [cofx-id (clindex-utils/stable-id :re-frame :cofx cofx-key)]
    {:facts [[:db/add cofx-id :re-frame.cofx/key cofx-key]
             [:db/add (clindex-utils/namespace-id (:namespace/name ctx)) :namespace/re-frame-cofxs cofx-id]]
     :ctx ctx}))
