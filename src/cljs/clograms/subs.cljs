(ns clograms.subs
  (:require [re-frame.core :as re-frame]
            [datascript.core :as d]))

(defn dependency-tree [db main-project-id]
  (d/pull db '[:project/name {:project/depends 6}] main-project-id))

(re-frame/reg-sub
 ::projecs-dependencies-edges
 (fn [{:keys [:datascript/db :main-project/id]} _]
   (when (and db id)
     (->> (dependency-tree db id)
          (tree-seq (comp not-empty :project/depends) :project/depends)
         (mapcat (fn [{:keys [:project/depends] :as p}]
                   (map (fn [d]
                          [p d])
                        depends)))
         (into #{})))))

(re-frame/reg-sub
 ::diagram
 (fn [{:keys [diagram]} _]
   diagram))


(re-frame/reg-sub
 ::all-entities
 (fn [{:keys [:datascript/db]} _]
   (->> (d/q '[:find ?pname ?nsname ?vname
               :in $
               :where
               [?vid :var/name ?vname]
               [?vid :var/namespace ?nsid]
               [?nsid :namespace/name ?nsname]
               [?pid :project/name ?pname]
               [?nsid :namespace/project ?pid]]
             db)
        (map #(zipmap [:project/name :namespace/name :var/name] %)))))

(re-frame/reg-sub
 ::selected-entity
 (fn [{:keys [selected-entity]} _]
   selected-entity))

(re-frame/reg-sub
 ::selected-entity-refs
 (fn [{:keys [:datascript/db :diagram]} _]
   (when-let [selected-entity (:selected-entity diagram)]
     (let [calls []
           called-by [{:project/name 'clojurescript
                       :namespace/name 'cljs.pprint
                       :var/name 'pprint-vector}
                      {:project/name 'clojurescript
                       :namespace/name 'cjs.pprint
                       :var/name 'pprint}]]
       {:calls calls
        :called-by called-by}))))
