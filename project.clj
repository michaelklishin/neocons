(defproject clojurewerkz/neocons "3.3.0-SNAPSHOT"
  :description "Neocons is a feature rich idiomatic Clojure client for the Neo4J REST API. It also supports Bolt protocol."
  :url "http://clojureneo4j.info"
  :license {:name "Eclipse Public License"}
  :min-lein-version "2.5.1"
  :dependencies [[org.clojure/clojure  "1.9.0"]
                 [cheshire             "5.8.0"]
                 [clj-http             "3.7.0" :exclusions [org.clojure/clojure]]
                 [clojurewerkz/support "1.1.0" :exclusions [com.google.guava/guava]]
                 [org.neo4j.driver/neo4j-java-driver "1.5.0"]]

  :test-selectors {:default        (fn [m] (and (not (:time-consuming m))
                                                (not (:http-auth m))
                                                (not (:edge-features m))
                                                (not (:graphene m))
                                                (not (:spatial m))))
                   :travis         (fn [m] (and (not (:time-consuming m))
                                                (not (:http-auth m))
                                                (not (:edge-features m))
                                                (not (:spatial m))))
                   :time-consuming :time-consuming
                   :focus          :focus
                   :indexing       :indexing
                   :cypher         :cypher
                   :graphene       :graphene
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
  :profiles       {:1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}

                   :master {:dependencies [[org.clojure/clojure "1.10.0-master-SNAPSHOT"]]}
                   :dev {:plugins [[lein-codox "0.10.3"]]
                         :codox {:source-paths ["src/clojure"]}}}
  :codox {:src-dir-uri "https://github.com/michaelklishin/neocons/blob/master/"
          :src-linenum-anchor-prefix "L"}


  :aliases        {"all" ["with-profile" "dev:dev,1.8:dev,master"]}

  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :java-source-paths ["src/java"]
  :global-vars {*warn-on-reflection* true})
