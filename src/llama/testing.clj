(ns llama.testing
  "Testing utilities for Camel Mock endpoints."
  (:import org.apache.camel.component.mock.MockEndpoint))

(defn mock
  "Get a mock endpoint in `endpoint` as an `MockEndpoint` in context `ctx`."
  [ctx endpoint]
  (.getEndpoint ctx endpoint (class (MockEndpoint.))))

(defn expect-bodies
  "Testing. Create an expectation on endpoint to have received 
the bodies in `messages`."
  [endpoint & messages]
  (.expectedBodiesReceived endpoint messages))


(defn mock-satisfied?
  "Runs the mock assertion, returns `true` if the mock endpoint is satisfied,
false if an `AssertionError` is thrown."
  [mock]
  (try
    (do
      (.assertIsSatisfied mock)
      true)
    (catch java.lang.AssertionError e
      false)))

(defn mock-not-satisfied?
  "Runs the mock assertion, returns `true` if the mock endpoint is satisfied,
false if an `AssertionError` is thrown."
  [mock]
  (try
    (do
      (.assertIsNotSatisfied mock)
      true)
    (catch java.lang.AssertionError e
      false)))
