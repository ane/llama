(ns llama.route-test
  (:refer-clojure :exclude [filter] :as core)
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
                     (set-body! (in x))))]
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
    (let [ctx (context)
          route (route (from "direct:bip")
                       (filter (fn [x] (not= "hi" (body (in x)))))
                       (to "mock:filter"))]
      (.addRoutes ctx route)
      (start ctx)
      (let [endpoint (mock ctx "mock:filter")]
        (expect-bodies endpoint "hello")
        (send-body ctx "direct:bip" "hello")
        (send-body ctx "direct:bip" "hi")
        (is (mock-satisfied? endpoint)))
      (stop ctx))))

(deftest aggregating
  (let [start-endpoint "direct:beep"
        mock-endpoint "mock:foo"
        aggrefn (fn [x1 x2]
                      (if x1
                        (do
                          (set-body!
                           (in x1) (str (body (in x1)) (body (in x2))))
                          x1)
                        x2))]
    (testing "aggregation works with completion size"
      (let [ctx (context)
            route (route (from start-endpoint)
                         (aggregate (header "foo") aggrefn)
                         (size 2)
                         (to mock-endpoint))]
        (add-routes ctx route)
        (start ctx)
        (let [endpoint (mock ctx mock-endpoint)
              uuid (java.util.UUID/randomUUID)]
          (expect-bodies endpoint "a quick brown fox jumped over the lazy dog")
          (send-body ctx start-endpoint "a quick brown fox" :headers {"foo" (str uuid)})
          (send-body ctx start-endpoint " jumped over the lazy dog"  :headers {"foo" (str uuid)})
          (is (mock-satisfied? endpoint)))
        (stop ctx)))))
