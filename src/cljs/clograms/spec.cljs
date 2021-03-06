(ns clograms.spec
  (:require [clojure.spec.alpha :as s]
            [clograms.re-grams.re-grams-spec :as rg-spec]
            [clograms.re-grams.re-grams :as rg]))

;;;;;;;;;;;; More general specs

(s/def :project/id number?)
(s/def :project/name symbol?)
(s/def :namespace/id number?)
(s/def :namespace/name symbol?)
(s/def :datascript/db any?)
;;;;;;;;;;;;;;;;;;;;;;;;;;

(s/def ::x number?)
(s/def ::y number?)

(s/def ::query string?)

(s/def ::side-bar (s/keys :req-un [::query]))

(s/def :var/reference (s/keys :req [:project/name
                                    :namespace/name
                                    :var/name
                                    :var/id]))

(s/def :bottom-bar.references/vars (s/coll-of :var/reference))
(s/def :bottom-bar.references/node-id ::rg/id)
(s/def :bottom-bar/references (s/keys :req-un [:bottom-bar.references/vars]
                                      :opt-un [:bottom-bar.references/node-id]))
(s/def :bottom-bar/title string?)
(s/def :bottom-bar/collapsed? boolean?)

(s/def ::bottom-bar (s/keys :opt-un [:bottom-bar/references
                                     :bottom-bar/title
                                     :bottom-bar/collapsed?
                                     ]))

(s/def ::level #{0 1 2})

(s/def ::selected-project (s/nilable :project/id))

(s/def ::selected-namespace (s/nilable :namespace/id))

(s/def ::projects-browser (s/keys :req-un [::level
                                           ::selected-project
                                           ::selected-namespace]))
(s/def ::selected-color string?)

(s/def ::label string?)
(s/def ::dispatch vector?)

(s/def ::menu-entry (s/keys :req-un [::label ::dispatch]))

(s/def ::menu (s/coll-of ::menu-entry))

(s/def ::ctx-menu (s/nilable (s/keys :req-un [::x ::y ::menu])))

(s/def :main-project/id :project/id)

(s/def ::loading? boolean?)

(s/def :namespace/colors (s/map-of :namespace/name string?))
(s/def :project/colors (s/map-of :project/name string?))

(s/def :node/comments (s/map-of ::rg/id string?))

(s/def :entity/type #{:var :namespace :project})

(s/def :accordion/active-item keyword?)

(s/def :components.general/accordion (s/keys :opt-un [:accordion/active-item]))

(s/def :components.general.text-edit-modal/on-text-set-event vector?)

(s/def :components.general/text-edit-modal (s/keys :req-un [:components.general.text-edit-modal/on-text-set-event]))
(s/def :clograms.ui.components/general (s/keys :opt-un [:components.general/accordion
                                                        :components.general/text-edit-modal]))

(s/def :tcp/port int?)
(s/def ::diagram-file string?)
(s/def ::folder string?)
(s/def ::platform #{:cljs :clj})

(s/def :clograms/config (s/keys :req-un [:tcp/port
                                         ::diagram-file
                                         ::folder
                                         ::platform]))
(s/def ::db (s/keys :req [::rg/diagram]
                    :opt [:datascript/db
                          :main-project/id
                          :namespace/colors
                          :project/colors
                          :node/comments
                          :clograms.ui.components/general]
                    :req-un [::side-bar
                             ::bottom-bar
                             ::projects-browser
                             ::selected-color
                             ::loading?]
                    :opt-un [::ctx-menu
                             :clograms/config]))
