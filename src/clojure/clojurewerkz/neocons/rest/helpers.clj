;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.helpers
  (:import  [java.net URI URL]))


;;
;; API
;;

(defn extract-id
  [^String location]
  (let [url (URL. location)]
    (Long/valueOf ^String (first (re-seq #"\d+$" (.getPath url))))))

(defn maybe-append
  [^String s ^String prefix]
  (.toLowerCase (if (.endsWith (.toLowerCase s) (.toLowerCase prefix))
                  s
                  (str s prefix))))

(definline check-not-nil!
  [ref ^String message]
  `(when (nil? ~ref)
     (throw (IllegalArgumentException. ~message))))
