(ns clograms.utils
  (:require [clojure.string :as str]
            [clojure.zip :as zip]))

(defn replace-in-str [f s from len]
  (let [before (subs s 0 from)
        after (subs s (+ from len))
        being-replaced (subs s from (+ from len))
        replaced (f being-replaced)]
    (str before replaced after)))

(defn replace-in-str-line [f s l from len]
  (->> (str/split-lines s)
       (map-indexed (fn [i line]
                      (if (= i l)
                        (replace-in-str f line from len)
                        line)))
       (str/join "\n")))

(defn code-zipper
  "Returns a zipper for nested sequences, given a root sequence"
  [root]
  (zip/zipper coll?
              seq
              (fn [node children] (with-meta children (meta node)))
              root))

(defn move-zipper-to-next [zloc pred]
  (loop [z (zip/next zloc)]
    (if (or (zip/end? z)
            (pred (zip/node z)))
      z
      (recur (zip/next z)))))
