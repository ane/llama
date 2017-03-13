(ns llama.core
  "Core utilities for working with Camel endpoints, messages, exchanges, etc."
  (:refer-clojure :exclude [get-in] :as core)
  (:import [org.apache.camel Exchange ExchangePattern Message]
           [org.apache.camel.impl DefaultCamelContext DefaultExchange DefaultMessage]))

(defn message
  "Create a Message, with `body` String as the body. Optionally with
  `:headers` (map) as the headers, and `:id` as the [message
  id](http://camel.apache.org/correlation-identifier.html)."
  [body & {:keys [id headers]
           :or {id nil
                headers (hash-map)}}]
  (let [msg (DefaultMessage.)]
    (doto msg
      (.setBody body)
      (.setHeaders (java.util.HashMap. headers))
      (.setMessageId id))))

(defn exchange
  "Create an exchange on context `on`. Can set `in` as the `In` message. If third arg is truthy,
  set the exchange pattern as `InOut`. See [[message]] and [[reply]].

  `on` can be either a context, another exchange or an endpoint.
  ```
  (body (in (exchange ctx (message \"hi\"))))
  ;; => \"hi\"
  
  (out-capable? (exchange ctx (message \"hi\") :out))
  ;; => true
  ```
  "
  ([on] (DefaultExchange. on))
  ([on in] (let [xchg (exchange on)]
             (.setIn xchg in)
             xchg))
  ([on in out] (let [xchg (exchange on in)]
                 (when out
                   (.setPattern xchg ExchangePattern/InOut))
                 xchg)))

(defn- ensure-message
  [body id headers]
  (if-not (instance? Message body)
    (message (str body) :id id :headers headers)
    body))

(defn out-capable?
  "If the Exchange in `x` is an `InOut` exchange or not."
  [^Exchange x]
  (.. x (getPattern) (isOutCapable)))

(defn reply!
  "Reply successfully to an exchange with `body`. `body` can be a String message
  or a [Message](http://camel.apache.org/message.html). If `body` is string it
  is converted into to a `Message` and the optional `headers` are inserted as
  the message headers and `:id` as the message ID. If `body` is already a
  Message its headers and id will be unaltered. See [[message]]
  and [[request-body]].

  **Note**. Reply works only on
  an [InOut](http://camel.apache.org/request-reply.html) Exchange. Does nothing
  if exchange is `InOnly`.

  ```
  (route 
     (from \"vm:lol\")
     (process (fn [x] (reply x \"haha\")))
  
  (request-body ctx \"vm:lol\" \"hi\")
  ;; => \"haha\"
  ```
  "
  [^Exchange exchange body & {:keys [headers id] :or {headers (hash-map)
                                            id nil}}]
  (when (out-capable? exchange)
    (let [out (.getOut exchange)
          m (ensure-message body id headers)]
      (.setOut exchange m))))

(defn fail!
  "Fail the exchange because of `reason`. `reason` can be a string or
  exception/throwable."
  [^Exchange exchange reason]
  (.setException exchange reason))

(defn failed?
  "Has the exchange failed (its exception is non-nil) or any of its messages
  faulted? See also [[fault?]]."
  [^Exchange exchange]
  (.isFailed exchange))

(defn fault!
  "Fault the message."
  [^Message msg]
  (.setFault msg true))

(defn fault?
  "Has the message faulted?"
  [^Message msg]
  (.isFault msg))

(defn send-body
  "Synchronously send `body` as message to `endpoint`.

  Sends `body` (String) to `endpoint`. Optionally add headers in `headers`."
  [ctx endpoint body & {:keys [headers]
                        :or {headers {}}}]
  (let [producer (.createProducerTemplate ctx)]
    (.sendBodyAndHeaders producer endpoint body headers)))

;; TODO: add timeout etc.
(defn request-body
  "Synchronously send to `endpoint` and expect a reply, returns the reply.

  Send `body` (String) to `endpoint` using the `InOut` pattern. Optionally send
  headers in `headers`.

  ```
  (route (from \"direct:foo\")
       (process (fn [x] (reply x \"Is it African or European?\"))))
  
  ;; start the route, add to context, etc.

  (request-body ctx \"direct:foo\" \"How much weight can an unladen swallow carry?\")
  ;; => \"Is it African or European?\"
  ```
  "
  [ctx endpoint body & {:keys [headers]
                        :or {headers {}}}]
  (let [producer (.createProducerTemplate ctx)]
    (.requestBodyAndHeaders producer endpoint body headers)))

(defn in
  "With 1 arg, get the body of the **In** part of an `InOut` exchange `x`. With second arg `h`, get the header `h`
  from `x`."
  ([^Exchange x] (.. x getIn getBody))
  ([^Exchange x ^String h] (.. x getIn (getHeader h))))

(defn in!
  "Set the **body** of the In message to `body`. Compare with [[set-in!]]."
  [exchange body]
  (set-body (get-in exchange) body))

(defn out!
  [exchange body]
  (when (out-capable?)
    (set-body (get-out exchange) body)))

(defn get-in
  [^Exchange x]
  (.getIn x))

(defn get-out
  [^Exchange x]
  (when (out-capable? x)
    (.getOut x)))

(defn out
  "With 1 arg, get the body of the **Out** part of an `InOut` exchange `x`. With second arg `h`, get the header `h`
  from `x`."
  ([^Exchange x]
   (when (out-capable? x)
     (.. x getOut getBody)))
  ([^Exchange x ^String h]
   (when (out-capable? x)
     (.. x getOut (getHeader h)))))

(defn body
  "Get the body of `msg`. 
  
  See [[in]] and [[out]] for getting the Message out of an Exchange.

  *Type conversions*. If `clazz` is provided use a [type
  converter](http://camel.apache.org/type-converter.html) to cast it to
  `clazz`. Throws
  [`NoTypeConversionAvailableException`](http://camel.apache.org/maven/current/camel-core/apidocs/org/apache/camel/NoTypeConversionAvailableException.html)
  if conversion fails because the type converter isn't in the registry. Throws `TypeConversionException` if the conversion *itself fails*.

  Uses the context of the exchange of msg. If `msg` has no exchange and context then the `DefaultCamelcontext` is used.

  ```
  ;; read from a direct exchange, get the in part of the exchange,
  ;; get the body, serialize to json using Cheshire, get :foo from the dict 
  ;; and print
  (route (from \"direct:hello\")                   
         (process                                  
           (fn [x] (->> (in x)                   
                        cheshire.core/parse-string 
                        :foo                       
                        println))))                
  ```
  "
  ([^Message msg] (.getBody msg))
  ([^Message msg
    ^java.lang.Class clazz]
   (let [ctx (if-some [ctx (.getContext (.getExchange msg))]
               (ctx)
               (DefaultCamelContext.))
         b (body msg)]
     (.mandatoryConvertTo (.getConverter ctx) b clazz))))

(defn headers
  "Get the headers of `msg` as a map."
  [^Message msg]
  (.getHeaders msg))

(defn id
  "Get the message id of `msg`."
  [^Message msg]
  (.getMessageId msg))

(defn set-body!
  "Set the body of `msg` to body."
  [^Message msg body]
  (.setBody msg body))

(defn set-in!
  "Set the **In** part of an exchange to `m`. If you want to set the body directly use [[in!]]."
  [xchg m]
  (.setIn xchg m))

(defn set-out!
  "Set the **Out** part of an exchange to `m`. No-op on **InOnly** exchanges. If you want to set the body directly use [[out!]]."
  [xchg m]
  (when (out-capable? xchg)
    (.setOut xchg m)))
