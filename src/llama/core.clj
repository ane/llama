(ns llama.core
  (:require [rx.lang.clojure.interop :as rxi]
            [rx.lang.clojure.core :as rx])
  (:import org.apache.camel.impl.DefaultCamelContext
           org.apache.camel.rx.ReactiveCamel
           org.slf4j.LoggerFactory
           rx.subjects.PublishSubject))

(defn mk-context []
  (DefaultCamelContext.))

(defn make-consumer
  ([ctx uri]
   (let [rx (ReactiveCamel. ctx)]
     (.toObservable rx uri)))
  ([ctx uri clazz]
   (let [rx (ReactiveCamel. ctx)]
     (.toObservable rx uri clazz))))

(defn make-producer [ctx uri]
  (let [rx (ReactiveCamel. ctx)
        ps (PublishSubject/create)]
    (.sendTo rx (.asObservable ps) uri)
    ps))

(defn foo
  "I don't do a whole lot."
  [x]
  (println x "Hello, World!"))

(defn run [& args]
  (let [logger (LoggerFactory/getLogger "bla")
        ctx (mk-context)
        obs (make-consumer ctx "file:pluts" java.lang.String)]
    (.info logger "well yeah")
    (rx/do println obs)
    ctx)) 
