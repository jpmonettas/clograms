#!/bin/bash

clj -e "(require '[garden-watcher.core :as gw]) (require '[com.stuartsierra.component :as component]) (component/start (gw/new-garden-watcher '[clograms.styles.main clograms.styles.components]))"
