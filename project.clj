(defproject llama "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Apache License, version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.apache.camel/camel-core "2.18.2"]]
  :plugins [[lein-codox "0.10.3"]]
  :profiles {:dev {:dependencies [[org.apache.camel/camel-test "2.18.2"]
                                  [org.apache.camel/camel-rabbitmq "2.18.2"]
                                  [ch.qos.logback/logback-classic "1.1.1"]]}}
  :codox {:source-paths ["src"]
          :doc-paths ["doc"]
          :output-path "docs"
          :namespaces [llama.route llama.core llama.testing]
          :source-uri "http://github.com/ane/llama/blob/master/{filepath}#L{line}"
          :metadata {:doc/format :markdown}})
