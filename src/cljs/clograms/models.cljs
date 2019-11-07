(ns clograms.models
  (:require [clograms.db :as db]
            [datascript.core :as d]))

(defmulti build-node (fn [entity-type id] entity-type))

(defmethod build-node :project
  [_ proj-id]
  {:entity {:entity/type :project
            :project/id proj-id}
   :diagram.node/type :clograms/project-node})

(defmethod build-node :namespace
  [_ ns-id]
  {:entity {:entity/type :namespace
            :namespace/id ns-id}
   :diagram.node/type :clograms/namespace-node})

(defmethod build-node :function
  [_ var-id]
  {:entity {:entity/type :function
            :var/id var-id}
   :diagram.node/type :clograms/function-node})

(defmethod build-node :multimethod
  [_ var-id]
  {:entity {:entity/type :multimethod
            :var/id var-id}
   :diagram.node/type :clograms/multimethod-node})
