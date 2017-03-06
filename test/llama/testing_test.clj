(ns llama.testing-test
  (:require [clojure.test :refer [deftest is testing]]
            [llama
             [core :refer [send-body]]
             [testing :refer :all]])
  (:import org.apache.camel.impl.DefaultCamelContext))

(deftest mocks
  (let [ctx (DefaultCamelContext.)]
    (testing "mocks work"
      (let [endpoint (mock ctx "mock:foo")]
        (expect-bodies endpoint "hello" "HI!")
        (send-body ctx endpoint "hello")
        (send-body ctx endpoint "HI!")
        (is (mock-satisfied? endpoint))))))
