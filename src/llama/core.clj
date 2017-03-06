(ns llama.core
  "Core utilities for working with Camel endpoints, messages, exchanges, etc."
  (:import [org.apache.camel Exchange Message]
           [org.apache.camel.impl DefaultCamelContext DefaultMessage]))

(defn string->message
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

(defn- ensure-message
  [body id headers]
  (if-not (instance? Message body)
    (string->message (str body) :id id :headers headers)
    body))

(defn out-capable?
  "If the Exchange in `x` is an `InOut` exchange or not."
  [^Exchange x]
  (.. x (getPattern) (isOutCapable)))

(defn reply
  "Reply successfully to an exchange with `body`. `body` can be a String message
  or a [Message](http://camel.apache.org/message.html). If `body` is string it is
  converted into to a `CamelMessage` and the optional `headers` are inserted as
  the message headers and `:id` as the message ID. If `body` is already a
  Message its headers will be unaltered. See [[message]] and [[request-body]].

  **Note**. Reply works only on
  an [InOut](http://camel.apache.org/request-reply.html) Exchange. Does nothing
  if exchange is `InOnly`.

  ```
  (route 
     (from \"vm:lol\")
     (process (fn [x] (reply x \"haha\")))
  ```
  "
  [exchange body & {:keys [headers id] :or {headers (hash-map)
                                            id nil}}]
  (when (out-capable? exchange)
    (let [out (.getOut exchange)
          m (ensure-message body id headers)]
      (.setOut exchange m))))

(defn send-body
  "Synchronously send to `endpoint`.

  Sends `body` (String) to `endpoint`. Optionally add headers in `headers`."
  [ctx endpoint body & {:keys [headers]
                        :or {headers {}}}]
  (let [producer (.createProducerTemplate ctx)]
    (.sendBodyAndHeaders producer endpoint body headers)))

;; TODO: add timeout etc.
(defn request-body
  "Synchronously send to `endpoint` and expect a reply. Returns the reply.

  Send `body` (String) to `endpoint` using the `InOut` pattern. Optionally add
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
  "Get the `In` part of an `InOnly` or `InOut` exchange."
  [^Exchange x]
  (.getIn x))

(defn out
  "Get the `Out` part of an `InOut` exchange. Returns nil if not `InOut`."
  [^Exchange x]
  (when (out-capable? x)
    (.getOut x)))

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
         (fn [x] (->> (x-in x)                   
                      body                       
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

(defn set-body
  "Set the body of `msg` to body."
  [^Message msg body]
  (.setBody msg body))
