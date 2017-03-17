(ns llama.core-test
  (:refer-clojure :exclude [filter] :as core)
  (:require [clojure.test :refer :all]
            [llama
             [core :refer :all]
             [route :refer :all]])
  (:import org.apache.camel.ExchangePattern
           [org.apache.camel.impl DefaultCamelContext DefaultExchange]))

(deftest replying
  (testing "with Message: reply works as expected"
    (let [ctx (context)
          xchg (exchange ctx (message "bla") :out)]
      (reply! xchg "hi there" :id "313" :headers {"foo" "bar"})
      (let [msg (exchange-out xchg)]
        (is (= (body msg) "hi there"))
        (is (= (id msg) "313"))
        (is (= (.get (headers msg) "foo") "bar"))))))

(deftest requesting
  (testing "requesting a reply works"
    (let [ctx (context)
          routes (route (from "direct:foo")
                        (process (fn [x] (reply! x "haha"))))]
      (add-routes ctx routes)
      (start ctx)
      (is (= "haha" (request-body ctx "direct:foo" "hehe")))
      (stop ctx))))

(deftest basic
  (testing "creating messages and exchanges works"
    (let [ctx (context)
          msg (message "testing")
          xchg (exchange ctx msg)
          xchg2 (exchange ctx msg :out)]
      (is (= "testing" (in xchg)))
      (is (out-capable? xchg2)))))
