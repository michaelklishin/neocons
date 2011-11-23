(defproject com.novemberain/neocons "1.0.0-SNAPSHOT"
  :description "Neocons is an experimental idiomatic Clojure client for the Neo4J REST API"
  :license { :name "Eclipse Public License" }
  :repositories {
                 "clojure-releases" "http://build.clojure.org/releases"
                 "sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}
                             }
                 }
  :dependencies [[org.clojure/clojure   "1.3.0"]
                 [org.clojure/data.json "0.1.2"]
                 ;; to use checkouts/momentum. MK.
                 [org.jboss.netty/netty "3.2.4.Final"]
                 [clj-http              "0.2.4"]]
  :dev-resources-path "test/resources"
  :warn-on-reflection true
  :test-selectors   {:default        (fn [v] (not (:time-consuming v)))
                     :time-consuming (fn [v] (:time-consuming v))
                     :focus          (fn [v] (:focus v))
                     :all            (fn [_] true)})
