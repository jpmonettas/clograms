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

(defmethod build-node :var
  [_ var-id]
  {:entity {:entity/type :var
            :var/id var-id}
   :diagram.node/type :clograms/var-node})

(defmethod build-node :re-frame-subs
  [_ id]
  {:entity {:entity/type :re-frame-subs
            :id id}
   :diagram.node/type :clograms/re-frame-subs-node})

(defmethod build-node :re-frame-event
  [_ id]
  {:entity {:entity/type :re-frame-event
            :id id}
   :diagram.node/type :clograms/re-frame-event-node})

(defmethod build-node :re-frame-fx
  [_ id]
  {:entity {:entity/type :re-frame-fx
            :id id}
   :diagram.node/type :clograms/re-frame-fx-node})

(defmethod build-node :re-frame-cofx
  [_ id]
  {:entity {:entity/type :re-frame-cofx
            :id id}
   :diagram.node/type :clograms/re-frame-cofx-node})

(defmethod build-node :spec
  [_ spec-id]
  {:entity {:entity/type :spec
            :spec/id spec-id}
   :diagram.node/type :clograms/spec-node})
