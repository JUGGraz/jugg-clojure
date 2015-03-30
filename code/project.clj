(defproject com.xaidat.workshop/workshopstats "1.0.0"
            :description "Do workshop prep stats"
            :dependencies [[org.clojure/clojure "1.7.0-alpha1"]
                           [clojure-csv/clojure-csv "2.0.1"]
                           [org.clojure/data.json "0.2.5"]
                           [org.clojure/tools.cli "0.3.1"]
                           [cheshire "5.4.0"]]
            :aot [com.xaidat.workshopstats]
            :main com.xaidat.workshopstats
            :source-paths ["src/main/clojure"]
            :test-paths ["src/test/clojure"])
