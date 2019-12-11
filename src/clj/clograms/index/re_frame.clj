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

   :source/form               {:db/cardinality :db.cardinality/one}
   :source/str                {:db/cardinality :db.cardinality/one}
   })


(defn events-facts [ctx [_ ev-key :as form]]
  (let [ev-id (clindex-utils/stable-id :re-frame :event ev-key)
        form-str (:form-str (meta form))]
    {:facts [[:db/add ev-id :re-frame.event/key ev-key]
             [:db/add (clindex-utils/namespace-id (:namespace/name ctx)) :namespace/re-frame-events ev-id]
             [:db/add ev-id :source/form (vary-meta form dissoc :form-str)]
             [:db/add ev-id :source/str form-str]]
     :ctx ctx}))

(defmethod forms-facts/form-facts 're-frame.core/reg-event-db
  [all-ns-map ctx form]
  (events-facts ctx form))

(defmethod forms-facts/form-facts 're-frame.core/reg-event-fx
  [all-ns-map ctx form]
  (events-facts ctx form))

(defn subscription-form-facts [all-ns-map ctx [_ subs-key :as form]]
  (let [subs-id (clindex-utils/stable-id :re-frame :subs subs-key)
        form-str (:form-str (meta form))]
    {:facts [[:db/add subs-id :re-frame.subs/key subs-key]
             [:db/add (clindex-utils/namespace-id (:namespace/name ctx)) :namespace/re-frame-subs subs-id]
             [:db/add subs-id :source/form (vary-meta form dissoc :form-str)]
             [:db/add subs-id :source/str form-str]]
     :ctx ctx}))

(defmethod forms-facts/form-facts 're-frame.core/reg-sub
  [all-ns-map ctx form]
  (subscription-form-facts all-ns-map ctx form))

(defmethod forms-facts/form-facts 're-frame.core/reg-sub-raw
  [all-ns-map ctx form]
  (subscription-form-facts all-ns-map ctx form))

(defmethod forms-facts/form-facts 're-frame.core/reg-fx
  [all-ns-map ctx [_ fx-key :as form]]
  (let [fx-id (clindex-utils/stable-id :re-frame :fx fx-key)
        form-str (:form-str (meta form))]
    {:facts [[:db/add fx-id :re-frame.fx/key fx-key]
             [:db/add (clindex-utils/namespace-id (:namespace/name ctx)) :namespace/re-frame-fxs fx-id]
             [:db/add fx-id :source/form (vary-meta form dissoc :form-str)]
             [:db/add fx-id :source/str form-str]]
     :ctx ctx}))

(defmethod forms-facts/form-facts 're-frame.core/reg-cofx
  [all-ns-map ctx [_ cofx-key :as form]]
  (let [cofx-id (clindex-utils/stable-id :re-frame :cofx cofx-key)
        form-str (:form-str (meta form))]
    {:facts [[:db/add cofx-id :re-frame.cofx/key cofx-key]
             [:db/add (clindex-utils/namespace-id (:namespace/name ctx)) :namespace/re-frame-cofxs cofx-id]
             [:db/add cofx-id :source/form (vary-meta form dissoc :form-str)]
             [:db/add cofx-id :source/str form-str]]
     :ctx ctx}))
