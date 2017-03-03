(ns llama.core
  (:import [org.apache.camel CamelContext Processor]
           org.apache.camel.builder.RouteBuilder))

(defn fn->processor
  "Create a [[org.apache.camel.Processor Processor]] from
`proc-fn`."
  [proc-fn]
  (proxy [Processor] []
    (process [xchg]
      (proc-fn xchg))))

(defmacro defprocessor
  "Def `name` a Camel [Processor](http://camel.apache.org/processor.html)
that runs `myfn` on the [Exchange](http://camel.apache.org/exchange.html) in `x`.
Equivalent to `(def name (fn->processor (fn [xchg] ...)))`.
```
;; upper-case, reverse
(defprocessor revUpper x
  (->> (.getBody (.getIn x))
       clojure.string/upper-case
       clojure.string/reverse
       (.setBody xchg))
```
"
  [name xchg myfn]
  `(def ~name
     (proxy [Processor] []
       (process [x#]
         (let [~xchg x#]
           ~myfn)))))

(defn- ensure-fn-or-processor
  "Barks if `p` is not fn or Processor."
  [p]
  (cond
    (instance? clojure.lang.IFn p) (fn->processor p)
    (instance? Processor p) p
    :else (throw (IllegalArgumentException.
                  (format "Arg %s not fn or Processor" p)))))

(defmacro builder [& body]
  `(proxy [RouteBuilder] []
     (configure []
       ~@body)))

(defmacro from [endpoint & rest]
  `(builder
    (let [~'this (.from ~'this ~endpoint)]
      ~@rest)))

(defmacro to [endpoint]
  `(.to ~'this ~endpoint))

(defmacro process [p]
  `(let [p# (ensure-fn-or-processor ~p)]
     (.process ~'this p#)))

(defmacro defcontext [name & rest]
  `(let [ctx# (DefaultCamelContext.)]
     (.addRoutes ctx# ~@rest)
     (def ~name ctx#)))
  
            
