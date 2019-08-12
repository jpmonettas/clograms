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
