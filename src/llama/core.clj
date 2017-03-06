(ns llama.core
  "Core utilities for working with Camel endpoints, messages, exchanges, etc."
  (:import org.apache.camel.impl.DefaultMessage
           org.apache.camel.Message))

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
  (when (.isOutCapable (.getPattern exchange))
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

(defn request-body
  "Synchronously send to `endpoint` and expect a reply. Returns the reply.

  Send `body` (String) to `endpoint` using the `InOut` pattern. Optionally add
  headers in `headers`.

```
(route (from \"direct:foo\")
       (process (fn [x] (reply x \"hello\"))))
  
;; start the route, add to context, etc.

(request-body ctx \"direct:foo\" \"hi\")
;; => \"hello\"
```
"
  [ctx endpoint body & {:keys [headers]
                        :or {headers {}}}]
  (let [producer (.createProducerTemplate ctx)]
    (.requestBodyAndHeaders producer endpoint body headers)))
