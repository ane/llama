(ns llama.route
  "A Clojure DSL for [Camel Routes](http://camel.apache.org/routes.html). Provides a framework from routing messages from endpoints to others.
  

  ### The DSL

  The DSL tries to be a close approximation of the [fluent
  DSL](http://camel.apache.org/java-dsl.html). The main elements of this
  namespace are [[route]] and [[defcontext]]. The following building blocks for
  routes exist:

  * [[from]] - read from endpoints
  * [[to]] - send to endpoints
  * [[process]] - process exchanges between endpoints
  * [[foreach]] - apply functions to messages of the exchanges
  * [[guard]] - filter exchanges with predicates
  
  [[route]] instantiates a [[RouteBuilder]], which is what you add to a
  *context*. These can be added to a context via [[defcontext]], or by calling
  the method `.addRoutes` on ctx directly.

  ```
  ;; read from (local) ActiveMQ queue hello, print the body of the incoming
  ;; message, write the exchange to a file in `dump/fromHello.txt`.
  (def myroute
    (route (from \"activemq:hello\")
           (process (fn [x] (println (body (in x)))))
           (to \"file:dump?fileName=fromhello.txt\"))
  ```

  ### Running routes

  After the routes have been added to the context, the context can be started
  with [[start]]. This is a non-blocking operation so if your program is simply
  doing something with Camel you need an infinite loop of sorts. It is a good
  idea to [[stop]] the context when your program starts. You could
  use [component](https://github.com/stuartsierra/component) for managing the
  lifecycle of your program.
  
  ```
  (defcontext ctx myroute)
  (-defn main [& args]
    (start ctx)
    ;; your app logic here
    (stop ctx))
  ```
  
  ### Where to start?

  It is a good idea to learn what [Apache Camel](http://camel.apache.org) is
  before trying to use Llama. [This StackOverflow thread](http://stackoverflow.com/questions/8845186/what-exactly-is-apache-camel)
  is a good place to start. To grok Llama, you need to understand the following:

  * [Endpoints](http://camel.apache.org/endpoint.html) -- sources and destinations of messages
  * [Exchanges](http://camel.apache.org/exchange.html) -- messagings between two components
  * [Routes](http://camel.apache.org/routes.html) -- how to wire exchanges between sources and destinations

  Once you have a basic understanding of those, you should be able to get going. Alternatively, dive in and 
  [read the tutorial](./02-tutorial.html).
  "
  (:import [org.apache.camel CamelContext Predicate Processor]
           org.apache.camel.builder.RouteBuilder
           org.apache.camel.impl.DefaultCamelContext)
  (require [llama.core :refer :all]))

(defn fn->processor
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
  (stop ctx)  ; shut down
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
       (.. ~'this ~@(map macroexpand-1 routes)))))

(defmacro from
  "Read from `endpoints`. 

  Must be the first expression inside a [[route]] block. Can only be called once
  in a [[route]] block.

  Each endpoint in `endpoints` can be a collection of either [Camel
  URIs](http://camel.apache.org/uris.html) or
  an [Endpoints](http://camel.apache.org/endpoint.html).

  You need to have the Camel component in the classpath. For example,
  `rabbitmq://` requires the RabbitMQ component, available in `camel-rabbitmq`,
  `activemq:...` requires ActiveMQ and so on.

  Makes the `RouteBuilder` defined in [[route]] to read from `endpoint`. Binds
  `this` to a `RouteDefinition` so that calls to [[to]] and [[process]] will
  work. You can call
  the [methods](https://static.javadoc.io/org.apache.camel/camel-core/2.18.2/org/apache/camel/model/RouteDefinition.html)
  of `this` to alter its behaviour. See [[route]].

  ```
  ;; will aggregate data from thee endpoints, printing the exchanges
  (route (from \"vm:foo\" \"direct:hello\" \"activemq:queue:bar\")
         (process (fn [x] (println x))))
  ```
  "
  [& endpoints]
  `(~'from (into-array (list ~@endpoints))))

(defmacro to
  "Send data to `endpoints`.
  
  Can be chained multiple times at any point after [[from]].

  Each endpoint in `endpoints` can be a collection of either [Camel
  URIs](http://camel.apache.org/uris.html) or
  an [Endpoints](http://camel.apache.org/endpoint.html).

  See the note about components in the docs for [[from]].

  Adds `endpoints`, Endpoints or String URI, to the `RouteDefinition`, sending
  exchanges to those endpoints. Must be after a [[from]]. See [[route]].

```
(route (from \"activemq:hello\")
       (to \"file:blaa\")
       (to \"kafka:topic:bar\"))
```
"
  [& endpoints]
  `(~'to (into-array (list ~@endpoints))))


(defmacro process
  "Add `p` as a processing step to this `RouteDefinition`. Useful for
  transforming a message to be sent elsewhere with [[to]], or to replying
  with [[reply]]. Must be invoked after a call to [[from]]. 

  `p` can be a one argument fn accepting an fn accepting one argument or a
  Processor. See [[fn->processor]]. Keep in mind, altering the exchange will
  affect subsequent inputs [[to]], [[guard]], [[process]] calls.

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
  `(~'process (ensure-fn-or-processor ~p)))

(defn fn->predicate
  "Turn `pred` into a Predicate. `pred` should be a 1 arg function."
  [pred]
  (cond (ifn? pred) (reify Predicate
                      (matches [this exchange] (pred exchange)))
        (instance? Predicate pred) pred
        :else (throw (IllegalArgumentException. "pred is not IFn or Predicate!"))))

(defmacro guard
  "Add `pred` as a filtering step. Operates on the exchange.

  A filter needs a downstream component, like [[to]] or [[process]]. It would be
  pretty useless otherwise, right? Without it, starting the associated context
  will blow up during start-up.

  `pred` should be an 1-arg function accepting an exchange and returning a boolean, or a Camel 
  [Predicate](http://camel.apache.org/predicate.html).
  
```
(route (from \"direct:foo\")
       (guard (fn [x] (starts-with? \"hello\" (body (in x)))))
       (process (fn [x] (println (str \"Made it:\" (body (in x))))))
       (to \"mock:faa\"))
```
  "
  [pred]
  `(~'filter (fn->predicate ~pred)))

(defmacro foreach 
  "Do something to the message(s) of the exchange. Like process, but operates on
  the in or out part of the exchange. `f1` should be fn of one argument. Apply
  `f1` to the **In** message. With second arg `f2` apply it to the **Out**
  message.

  ```
  (route (from \"direct:asdf\")
         (foreach (comp println body)))
  ```
  "
  ([f1] `(~'process (fn->processor (fn [x#] (~f1 (~in x#))))))
  ([f1 f2]
   `(~'process
     (fn->processor (fn [x#]
                      (do
                        (~f1 (~in x#))
                        (~f2 (~out x#))))))))

(defn start
  "Starts `ctx`, does not block."
  [^CamelContext ctx]
  (.start ctx))

(defn stop
  "Stops `ctx`, shutting down all routes that go along with it."
  [^CamelContext ctx]
  (.stop ctx))

(defn context
  "Create a CamelContext. Optionally pass a [JNDI
  Context](http://docs.oracle.com/javase/7/docs/api/javax/naming/Context.html?is-external=true)
  or [Registry](http://camel.apache.org/registry.html)."
  ([] (DefaultCamelContext.))
  ([ctx-or-reg] (DefaultCamelContext. ctx-or-reg)))

(defn add-routes
  "Add the routes in `routes` to `ctx`."
  [ctx routes]
  (.addRoutes ctx routes))

