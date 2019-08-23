#!/bin/bash

clj -e "(require '[garden-watcher.core :as gw]) (gw/start-garden-watcher! '[clograms.styles.main])"
