(ns clograms.db)

(def project-browser-transitions
  [:projects :namespaces :vars])

(def default-db
  {:side-bar {:selected-side-bar-tab :projects-browser}
   :projects-browser {:level 0}})
