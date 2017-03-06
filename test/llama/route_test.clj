(ns llama.route-test
  (:require [clojure
             [string :as str]
             [test :refer [deftest is testing]]]
            [llama
             [route :refer :all]
             [testing :refer [expect-bodies mock mock-satisfied?]]])
  (:import [org.apache.camel.impl DefaultCamelContext DefaultExchange DefaultMessage]))

(deftest processor-test
  (testing "processor composes well"
    (let [ctx (DefaultCamelContext.)
          xchg (DefaultExchange. ctx)
          msg (doto (DefaultMessage.)
                (.setBody "hello"))]
      (let [p (fn [x]
                (->> (.. x getIn getBody)
                     str/upper-case
                     str/reverse
                     (.setBody (.getIn x))))]
        (.setIn xchg msg)
        (.process (fn->processor p) xchg)
        (is (= "OLLEH" (.getBody (.getIn xchg))))))))

(deftest routing
  (testing "routing works"
    (let [ctx (DefaultCamelContext.)
          producer (.createProducerTemplate ctx)
          route (route (from "direct:bip") 
                       (to "mock:foo"))]
      (.addRoutes ctx route)
      (start ctx)
      (let [endpoint (mock ctx "mock:foo")]
        (expect-bodies endpoint "hello")
        (.sendBody producer "direct:bip" "hello")
        (is (mock-satisfied? endpoint)))
      (stop ctx))))
