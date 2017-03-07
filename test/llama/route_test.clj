(ns llama.route-test
  (:require [clojure
             [string :as str]
             [test :refer [deftest is testing]]]
            [llama
             [core :refer :all]
             [route :refer :all]
             [testing :refer [expect-bodies mock mock-satisfied?]]])
  (:import [org.apache.camel.impl DefaultCamelContext DefaultExchange DefaultMessage]))

(deftest processor-test
  (testing "processor composes well"
    (let [ctx (context)
          xchg (exchange ctx)
          msg (message "hello")]
      (let [p (fn [x]
                (->> (body (in x))
                     str/upper-case
                     str/reverse
                     (set-body (in x))))]
        (.setIn xchg msg)
        (.process (fn->processor p) xchg)
        (is (= "OLLEH" (body (in xchg))))))))

(deftest routing
  (testing "routing works"
    (let [ctx (context)
          route (route (from "direct:bip") 
                       (to "mock:foo"))]
      (.addRoutes ctx route)
      (start ctx)
      (let [endpoint (mock ctx "mock:foo")]
        (expect-bodies endpoint "hello")
        (send-body ctx "direct:bip" "hello")
        (is (mock-satisfied? endpoint)))
      (stop ctx))))

(deftest filtering
  (testing "filtering works"
    (let [ctx (DefaultCamelContext.)
          route (route (from "direct:bip")
                       (guard (fn [x] (not= "hi" (body (in x)))))
                       (to "mock:filter"))]
      (.addRoutes ctx route)
      (start ctx)
      (let [endpoint (mock ctx "mock:filter")]
        (expect-bodies endpoint "hello")
        (send-body ctx "direct:bip" "hello")
        (send-body ctx "direct:bip" "hi")
        (is (mock-satisfied? endpoint)))
      (stop ctx))))
