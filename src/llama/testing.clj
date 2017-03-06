(ns llama.testing
  "Testing utilities for Camel Mock endpoints."
  (:import org.apache.camel.component.mock.MockEndpoint))

(defn mock
  "Use in a test. Get a mock endpoint in `endpoint` as an `MockEndpoint` in context `ctx`.

```
(let [ctx (DefaultCamelContext.)
      mock-endpoint (mock ctx \"mock:bar\")]
  (expect-bodies mock-endpoint \"HI!\")
  (send-body ctx mock-endpoint \"HI!\")
  (is (mock-satisfied? mock-endpoint)))
```
"
  [ctx endpoint]
  (.getEndpoint ctx endpoint (class (MockEndpoint.))))

(defn expect-bodies
  "Testing. Create an expectation on endpoint to have received 
the bodies in `messages`. See [[mock]]."
  [endpoint & messages]
  (.expectedBodiesReceived endpoint messages))


(defn mock-satisfied?
  "Runs the mock assertion, returns `true` if the mock endpoint is satisfied,
false if an `AssertionError` is thrown. See [[mock]]."
  [mock]
  (try
    (do
      (.assertIsSatisfied mock)
      true)
    (catch java.lang.AssertionError e
      false)))

(defn mock-not-satisfied?
  "Runs the mock assertion, returns `true` if the mock endpoint is satisfied,
false if an `AssertionError` is thrown. See [[mock]]."
  [mock]
  (try
    (do
      (.assertIsNotSatisfied mock)
      true)
    (catch java.lang.AssertionError e
      false)))
