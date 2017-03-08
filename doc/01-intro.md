# Introduction 

Llama is a lightweight Clojure implementation of Apache Camel, a framework for integrating systems
together. It currently provides a Clojure translation of the routing DSL and associated helper
methods to work with elements of Camel.

## Status of this work

Thanks to the `..` macro, any expression of the form (route (x foo) (y) (bar
z)) is directly translated to the method chain of applying first `x` then `y`
and then `z`, meaning, it is equivalent to the Java call
`x(foo).y().bar(z)`. Since this happens to match exactly how the routing DSL
works in Java, Llama effectively has full support for the DSL already, but see
the next section.

### Clojure conversions 

Most methods like `process` in the Java DSL accept anynymous classes based on
abstract classes. Llama tries to provide as many macros to make the DSL be
more Clojure-like, e.g., [[process]] automatically converts its argument to a
`Processor` if it the argument is a function. Similarly, [[from]] and [[to]]
can be called with multiple args and this translated into a call with an
`Array` argument of endpoints.

So, everything that is in the Java API already works, but their usage may be
cumbersome, since Camel was written before Java 8 lambdas. Here is a list of
the methods that are implemented by a Clojure macro, and their modification:

* [[from]] -- accepts varargs
* [[to]] -- ditto 
* [[process]] -- accepts a function
* [[filter]] -- accepts a function

The rest of the work is ongoing. The additional benefit of having a macro
behind is to provide a suitable doc on this page. For example, the
`aggregator` pattern is not yet implemented, and its parameter is either an
`Expression` or a `AggregationStrategy`, so you'd need to `reify` an instance
of those classes to get them to work.

Contributions of macros for the routing DSL are welcome!

### Other features

Besides the routing DSL, planned features are to have Consumers and Producers like
in [Akka Camel](http://doc.akka.io/docs/akka/current/scala/camel.html)
using [core.async](https://github.com/clojure/core.async) channels, so that you could 
do 

```clj
(def ch (chan))
(defcontext ctx)

(consume-to ctx "activemq:foo:bar" ch)
;; now ch gets messages from activemq
```
