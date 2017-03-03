(defproject llama "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.apache.camel/camel-core "2.18.2"]
                 [org.apache.camel/camel-rabbitmq "2.18.2"]
                 [ch.qos.logback/logback-classic "1.1.1"]]
  :plugins [[lein-codox "0.9.4"]]
  :codox {:source-paths ["src"]
          :doc-paths ["doc"]
          :output-path "docs"
          :namespaces [llama.core]
          :source-uri "http://github.com/ane/llama/blob/master/{filepath}#L{line}"
          :metadata {:doc/format :markdown}})
