(ns llama.core-test
  (:import org.apache.camel.impl.DefaultMessage
           org.apache.camel.impl.DefaultCamelContext
           org.apache.camel.impl.DefaultExchange)
  (:require [clojure
             [string :as str]
             [test :refer :all]]
            [llama.core :refer :all]))

(deftest processor-test
  (testing "processor composes well"
    (let [ctx (DefaultCamelContext.)
          xchg (DefaultExchange. ctx)
          msg (doto (DefaultMessage.)
                (.setBody "hello"))]
      (defprocessor processor x
        (let [body (.getBody (.getIn x))]
          (->> body
               str/upper-case
               str/reverse
               (.setBody (.getIn x)))))
      (.setIn xchg msg)
      (.process processor xchg)
      (is (= "OLLEH" (.getBody (.getIn xchg)))))))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))
