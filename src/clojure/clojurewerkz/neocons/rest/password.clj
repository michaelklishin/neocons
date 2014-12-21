(ns clojurewerkz.neocons.rest.password
  (:require [cheshire.core             :as json]
            [clojure.string            :as string]
            [clojure.tools.cli         :refer [parse-opts]]
            [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.support.http.statuses :refer [success?]])
  (:gen-class))


(defn get-token
  [uri username password]
  (let [{:keys [status body]}  (nr/POST (nr/map->Connection {:options {} :http-auth {}})
                                        (str uri "authentication")
                                        :body (json/encode {:username username :password password}))]
    (when (success? status)
      (get (json/decode body) "authorization_token"))))


(defn change-password
  [uri username old-password new-password]
  (let [{:keys [status body]}  (nr/POST (nr/map->Connection {:options {} :http-auth {}})
                                        (str uri "user/" username "/password")
                                        :body (json/encode {:password old-password
                                                            :new_password new-password}))]
    (when (success? status)
      (println (get (json/decode body) "authorization_token")))))

(def cli-options
  [["-d" "--uri URI" "Neo4j URI"
    :default "http://localhost:7474/"
    :flag false]
   ["-u" "--user USER" "User Name"
    :default "neo4j"
    :flag false]
   ["-p" "--password PASSWORD" "Password"
    :default "neo4j"
    :flag false]
   ["-n" "--new-password NEWPASSWORD" "New Password (optional)"
    :default nil
    :flag false]
   ["-h" "--help"]])


(defn usage [options-summary]
  (->> ["This program authenticates against a Neo4j database server given a username
         and a password with an optional new password.
         \nIt returns the Authentication token for that user."
        ""
        "Usage: program-name [options]"
        ""
        "Options:"
        options-summary
        ""]
       (string/join \newline)))


(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))


(defn exit [status msg]
  (println msg)
  (System/exit status))


(defn -main [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    #_(println options)
    (cond
      (:help options) (exit 0 (usage summary))
      errors (exit 1 (error-msg errors)))
    (println (get-token (:uri options)
                        (:user options)
                        (:password options)))))
