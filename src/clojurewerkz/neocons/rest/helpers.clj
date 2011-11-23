(ns clojurewerkz.neocons.rest.helpers
  (:import  [java.net URI URL]))


;;
;; API
;;

(defn extract-id
  [^String location]
  (let [url (URL. location)]
    (Long/valueOf ^String (first (re-seq #"\d+$" (.getPath url))))))
