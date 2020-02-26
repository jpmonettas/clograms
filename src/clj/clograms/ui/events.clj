(ns clograms.ui.events)

(def dispatch-event nil)

(defmulti handle-event :event/type)
