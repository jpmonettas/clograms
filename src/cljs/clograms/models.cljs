(ns clograms.models
  (:require [clograms.db :as db]
            [datascript.core :as d]))

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

(defmethod build-node :var
  [entity-type id]
  {:entity {:entity/type :var
            :var/id id}
   :diagram.node/type :clograms/var-node})

(defmulti enrich-entity (fn [_ entity] (:entity/type entity)))

(defmethod enrich-entity :project
  [ds-db {:keys [:project/id] :as e}]
  (assoc e :project/name (:project/name (d/entity ds-db id))))

(defmethod enrich-entity :namespace
  [ds-db {:keys [:namespace/id] :as e}]
  (let [namespace (d/entity ds-db id)]
    (assoc e
           :project/name (:project/name (:namespace/project namespace))
           :namespace/name (:namespace/name namespace))))

(defmethod enrich-entity :var
  [ds-db {:keys [:var/id] :as e}]
  (let [entity-extra (db/var-entity ds-db id)]
    (merge e entity-extra)))
