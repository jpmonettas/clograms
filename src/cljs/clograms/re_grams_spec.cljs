(ns clograms.re-grams-spec
  (:require [clojure.spec.alpha :as s]
            [clograms.re-grams :as rg]))

(s/def :diagram.object/type #{:node :link :diagram})
(s/def ::rg/id string?)
(s/def :diagram.node/type (s/nilable keyword?))
(s/def :diagram.port/type (s/nilable keyword?))

(s/def ::client-x (s/nilable number?)) ;; TODO: fix nilable
(s/def ::client-y (s/nilable number?)) ;; TODO: fix nilable

(s/def ::w number?)
(s/def ::h number?)
(s/def ::x number?)
(s/def ::y number?)

(s/def ::port (s/keys :req [::rg/id
                            :diagram.port/type]
                      :req-un [::w
                               ::h
                               ::x
                               ::y]))

(s/def ::node (s/keys :req [::rg/id
                            :diagram.node/type]
                      :req-un [::client-x
                               ::client-y
                               ::w
                               ::h
                               ::x
                               ::y]
                      :opt-un [::ports]))

(s/def ::from-port (s/tuple ::rg/id ::rg/id))
(s/def ::to-port (s/tuple ::rg/id ::rg/id))
(s/def ::tmp-link-from (s/tuple ::rg/id ::rg/id))

(s/def ::link (s/keys :req [::rg/id]
                      :req-un [::from-port
                               ::to-port]))

(s/def ::coord (s/tuple number? number?))

(s/def ::ports (s/map-of string? ::port))
(s/def ::nodes (s/map-of string? ::node))
(s/def ::links (s/map-of string? ::link))

(s/def ::scale (and number? pos?))
(s/def ::translate ::coord)

(s/def ::cli-origin ::coord)
(s/def ::cli-current ::coord)
(s/def ::start-pos ::coord)
(s/def ::grab-object (s/keys :req [:diagram.object/type]
                             :opt [::rg/id]
                             :req-un [::start-pos]
                             :opt-un [::tmp-link-from]))

(s/def ::grab (s/nilable
               (s/keys :req-un [::cli-origin
                                ::cli-current
                                ::grab-object])))

(s/def ::selected-node-id ::rg/id)

(s/def ::rg/diagram (s/keys :req-un [::nodes
                                     ::links
                                     ::scale
                                     ::translate]
                            :opt-un [::grab
                                     ::selected-node-id]))
