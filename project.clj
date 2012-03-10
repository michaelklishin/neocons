(defproject clojurewerkz/neocons "1.0.0-SNAPSHOT"
  :license {:name "Eclipse Public License"}
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.2"]
                 [clj-http "0.3.3"]]
  :test-selectors {:default (fn [v] (not (:time-consuming v))),
                   :time-consuming (fn [v] (:time-consuming v)),
                   :focus (fn [v] (:focus v)),
                   :indexing (fn [v] (:indexing v)),
                   :cypher (fn [v] (:cypher v)),
                   :all (fn [_] true)}
  :source-paths ["src/clojure"]
  :profiles {:all
             {:dependencies
              [[org.clojure/data.json "0.1.2"] [clj-http "0.3.3"]]},
             :1.4
             {:dependencies [[org.clojure/clojure "1.4.0-beta4"]]}}
  :test-resources-path "/Users/antares/Development/ClojureWerkz/neocons.git/test/resources"
  :repositories {"clojure-releases"
                 "http://build.clojure.org/releases",
                 "sonatype"
                 {:url
                  "http://oss.sonatype.org/content/repositories/releases",
                  :snapshots false,
                  :releases {:checksum :fail, :update :always}}}
  :java-source-paths ["src/java"]
  :min-lein-version "2.0.0"
  :dev-resources-path "/Users/antares/Development/ClojureWerkz/neocons.git/test/resources"
  :warn-on-reflection true
  :description "Neocons is an experimental idiomatic Clojure client for the Neo4J REST API")
