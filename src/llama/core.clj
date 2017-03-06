d. See [[message]] and [[request-body]].

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
