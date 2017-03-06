(ns llama.route
  "A Clojure DSL for [Camel Routes](http://camel.apache.org/routes.html).
  
### The DSL

The DSL tries to be a close approximation of the [fluent DSL](http://camel.apache.org/java-dsl.html). The main
elements of this namespace are [[route]] and [[defcontext]]. The following building blocks for routes exist:

* [[from]] - read from endpoints
* [[process] - process exchanges between endpoints
* [[to]] - send to endpoints
 
[[route]] instantiates a [[RouteBuilder]], which is what you add to a
  *context*. These can be added to a context via [[defcontext]].

(def myroute
  (route (from \"activemq:hello\")
         (process (fn [x] 

### Running routes


"
  (:import [org.apache.camel CamelContext Message Processor]
           org.apache.camel.builder.RouteBuilder
           [org.apache.camel.impl DefaultCamelContext DefaultMessage]))

(defn ^:no-doc fn->processor
  "Create a [Processor](http://camel.apache.org/processor.html) from `proc-fn`,
  a fn accepting one argument,
  an [Exchange](http://camel.apache.org/exchange.html). See [[process]]."
  [proc-fn]
  (proxy [Processor] []
    (process [xchg]
      (proc-fn xchg))))

(defn ^:no-doc ensure-fn-or-processor
  "Barks if `p` is not fn or Processor."
  [p]
  (cond
    (instance? clojure.lang.IFn p) (fn->processor p)
    (instance? Processor p) p
    :else (throw (IllegalArgumentException.
                  (format "Arg %s not fn or Processor" p)))))


(defmacro defcontext
  "Defines `name` to DefaultCamelContext, adding
  the [RouteBuilder](https://static.javadoc.io/org.apache.camel/camel-core/2.18.2/org/apache/camel/builder/RouteBuilder.html)
  in `routes`. Note, the routes won't start unless the context isn't already
  started. Sets `nameStragety` field of `ctx` to `name`. **Remember**, nothing will start unless you call
  `(start name)`. See [[start]] and [[stop]].

```
(defcontext foobar
  (route (from \"activemq:queue:hi\")
         (process (fn [xchg] (println xchg)))
         (to \"file:blah\")))

(start ctx) ; pump a message to activemq on queue hi, will be printed to *out*
            ; note: this does NOT block! use a loop if you want your program to
            ; run
(stop ctx) ; shut down
```
"
  [name & routes]
  `(let [ctx# (DefaultCamelContext.)]
    (.setName ctx# (str '~name))
    (doseq [route# (list ~@routes)]
      (.addRoutes ctx# route#))
    (def ~name ctx#)))

(defmacro route
  "Build a Camel [Route](http://camel.apache.org/routes.html), using [[from]], [[to]], [[process]] etc.

  Binds `this` to a `RouteBuilder`, using the definitions in `routes`. The first
  element in `route` should be a call to [[from]], followed by
  either [[process]] or [[to]]. Note, adding a route with just a from, i.e., an
  empty `rest`, results an error when you start the context.

```
(route 
  (from \"activemq:queue:hi\")
  (process (fn [x] (println x)))
  (to \"rabbitmq:blah\"))
```"
  [& routes]
  `(proxy [RouteBuilder] []
     (configure []
       (let [~'this ~(first routes)]
         (do
           ~@(drop 1 routes))))))

(defmacro from
  "Read from `endpoint`, can be a [Camel URI](http://camel.apache.org/uris.html)
  or an [Endpoint](http://camel.apache.org/endpoint.html). Use inside
  a [[route]] definition. Can only be called once in a [[route]]. 

  You need to have the Camel component in the classpath. For example,
  `rabbitmq://` requires the RabbitMQ component, available in `camel-rabbitmq`,
  `activemq:...` requires ActiveMQ and so on.

  Make the `RouteBuilder` defined in [[route]] to read from `endpoint`. Binds
  `this` to a `RouteDefinition` so that calls to [[to]] and [[process]] will
  work. You can call
  the [methods](https://static.javadoc.io/org.apache.camel/camel-core/2.18.2/org/apache/camel/model/RouteDefinition.html)
  of `this` to alter its behaviour. See [[route]].

```
(route (from \"vm:foo\")
       (process (fn [x] (println x))))
```
"
  [endpoint]
  `(.from ~'this ~endpoint))

(defmacro to
  "Send data to `endpoint`, can be a [Camel
  URI](http://camel.apache.org/uris.html) or
  an [Endpoint](http://camel.apache.org/endpoint.html). Use inside a [[route]]
  definition. Can be chained multiple times at any point after [[from]].

For components see the doc in [[from]].

  Adds `endpoints`, Endpoints or String URI, sending exchanges to those
  endpoints. Must be after a [[from]]. See [[route]].

```
(route (from \"activemq:hello\")
       (to \"file:blaa\")
       (to \"kafka:topic:bar\"))
```
"
  [endpoint]
  `(.to ~'this ~endpoint))


(defmacro process
  "Add `p` as a processing step to this `RouteDefinition`. Useful for
  transforming a message to be sent elsewhere with [[to]], or to replying
  with [[reply]]. Must be invoked after a call to [[from]]. `p` can be a one
  argument fn accepting an fn accepting one argument or a
  Processor. See [[fn->processor]]. Keep in mind, altering the exchange will
  affect subsequent inputs [[to]] or [[process]] calls.  

```
(def upper-reverse [x]
  (->> (.getBody (.getIn x)) ;; or (.. x (.getIn) (.getBody))
       clojure.string/upper-case
       clojure.string/reverse
       (.setBody xchg)))

;; creates a Jetty REST server
(route (from \"jetty://localhost:33221/hello\")
       (process upper-reverse)
       (to \"log:hello\")
```
"
  [p]
  `(let [p# (ensure-fn-or-processor ~p)]
     (.process ~'this p#)))

(defn start
  "Starts `ctx`, does not block."
  [^CamelContext ctx]
  (.start ctx))

(defn stop
  "Stops `ctx`, shutting down all routes that go along with it."
  [^CamelContext ctx]
  (.stop ctx))
