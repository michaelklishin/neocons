(defproject clojurewerkz/neocons "1.1.0-beta3-SNAPSHOT"
  :description "Neocons is a feature rich idiomatic Clojure client for the Neo4J REST API"
  :url "http://clojureneo4j.info"
  :license {:name "Eclipse Public License"}
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure  "1.4.0"]
                 [cheshire             "4.0.3"]
                 [clj-http             "0.6.3"]
                 [clojurewerkz/support "0.12.0"]
                 [clojurewerkz/urly    "2.0.0-alpha4"]]
  :test-selectors {:default        (fn [m] (and (not (:time-consuming m))
                                                (not (:http-auth m))
                                                (not (:edge-features m))
                                                (not (:spatial m))))
                   :time-consuming :time-consuming
                   :focus          :focus
                   :indexing       :indexing
                   :cypher         :cypher
                   :http-auth      :http-auth
                   :spatial        :spatial
                   ;; as in, bleeding edge Neo4J Server
                   :edge-features  :edge-features
                   ;; assorted examples (extra integration tests)
                   :examples       :examples
                   :batching       :batching
                   :traversal      :traversal
                   :uri-encoding   (fn [m] (or (:examples m)
                                               (:indexing m)))
                   :all            (constantly true)}
  :source-paths ["src/clojure"]
  :profiles       {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
                   :1.5 {:dependencies [[org.clojure/clojure "1.5.0-RC4"]]}
                   :dev {:plugins [[codox "0.6.1"]]
                         :codox {:sources ["src/clojure"]
                                 :output-dir "doc/api"}}}
  :aliases        {"all" ["with-profile" "dev:dev,1.3:dev,1.5"]}
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :java-source-paths ["src/java"]
  :warn-on-reflection true)
