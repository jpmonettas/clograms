(ns clograms.re-grams.re-grams-spec
  (:require [clojure.spec.alpha :as s]
            [clograms.re-grams.re-grams :as rg]))

(s/def :diagram.object/type #{:node :link :diagram :node-resizer})
(s/def ::rg/id string?)
(s/def :diagram.node/type (s/nilable keyword?))
(s/def :diagram.link/type #{:clograms/straight-line})

(s/def ::client-x number?)
(s/def ::client-y number?)

(s/def ::w number?)
(s/def ::h number?)
(s/def ::x number?)
(s/def ::y number?)

(s/def :diagram.port/id (s/and int? #(<= 0 % 7)))
(s/def ::port (s/keys :req []
                      :req-un [::w
                               ::h
                               ::x
                               ::y
                               :diagram.port/id]))

(s/def ::label string?)

(s/def ::extra-data map?)

(s/def ::node (s/keys :req [::rg/id
                            :diagram.node/type]
                      :req-un [::w
                               ::h
                               ::x
                               ::y]
                      :opt-un [::ports
                               ::extra-data
                               ::client-x
                               ::client-y]))

(s/def ::from-port (s/tuple ::rg/id :diagram.port/id))
(s/def ::to-port (s/tuple ::rg/id :diagram.port/id))
(s/def ::tmp-link-from (s/tuple ::rg/id :diagram.port/id))

(s/def ::arrow-start? boolean?)
(s/def ::arrow-end? boolean?)

(s/def ::link (s/keys :req [::rg/id
                            :diagram.link/type]
                      :req-un [::from-port
                               ::to-port]
                      :opt-un [::arrow-start?
                               ::arrow-end?
                               ::label]))

(s/def ::coord (s/tuple number? number?))

(s/def ::ports (s/map-of :diagram.port/id ::port))
(s/def ::nodes (s/map-of string? ::node))
(s/def ::links (s/map-of string? ::link))

(s/def ::scale (and number? pos?))
(s/def ::translate ::coord)

(s/def ::cli-origin ::coord)
(s/def ::cli-current ::coord)
(s/def ::start-pos ::coord)
(s/def ::start-size (s/tuple number? number?))
(s/def ::prop-resize? boolean?)
(s/def ::grab-object (s/keys :req [:diagram.object/type]
                             :opt [::rg/id]
                             :opt-un [::tmp-link-from
                                      ::prop-resize?]))

(s/def ::grab (s/nilable
               (s/keys :req-un [::cli-origin
                                ::cli-current
                                ::grab-object])))

(s/def ::selected-nodes-ids (s/coll-of ::rg/id :kind set?))

(s/def ::link-config (s/keys :req [:diagram.link/type]
                             :req-un [::arrow-start?
                                      ::arrow-end?]))

(s/def :main-tool/tool #{:selection :drag})

(s/def ::main-tool-config (s/keys :req-un [:main-tool/tool]))
(s/def ::rg/diagram (s/keys :req-un [::nodes
                                     ::links
                                     ::scale
                                     ::translate
                                     ::link-config
                                     ::main-tool-config
                                     ::selected-nodes-ids]
                            :opt-un [::grab]))
