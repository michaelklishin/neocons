(ns clojurewerkz.neocons.rest.statuses)


;;
;; API
;;

(defn success?
  [^long status]
  (<= 200 status 299))

(defn missing?
  [^long status]
  (= status 404))

(defn conflict?
  [^long status]
  (= status 409))


(defn redirect?
  [^long status]
  (<= 300 status 399))

(defn error?
  [^long status]
  (<= 400 status 499))

(defn server-error?
  [^long status]
  (<= 500 status 599))
