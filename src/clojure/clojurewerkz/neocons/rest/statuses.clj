(ns clojurewerkz.neocons.rest.statuses)


;;
;; API
;;

(defn success?
  [^long status]
  (and (>= status 200)
       (<= status 299)))

(defn missing?
  [^long status]
  (and (= status 404)))

(defn conflict?
  [^long status]
  (and (= status 409)))


(defn redirect?
  [^long status]
  (and (>= status 300)
       (<= status 399)))

(defn error?
  [^long status]
  (and (>= status 400)
       (<= status 499)))

(defn server-error?
  [^long status]
  (and (>= status 500)
       (<= status 599)))
