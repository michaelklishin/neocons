(ns clojurewerkz.neocons.rest.password
  (:require [cheshire.core             :as json]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.support.http.statuses :refer [success?]])
  (:gen-class))


(defn change-password
  [uri username old-password new-password]
  (let [{:keys [status body]}  (nr/POST (nr/map->Connection {:options {} :http-auth {:basic-auth [username old-password]}})
                                        (str uri "user/" username "/password")
                                        :body (json/encode {:password new-password}))]

    (when (success? status)
      (println "Password changed!"))))


(defn -main
  [& args]
  (let  [[uri user password new-password]  args]
    (when new-password
      (change-password uri
                       user
                       password
                       new-password))))
