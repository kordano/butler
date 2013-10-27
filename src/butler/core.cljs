(ns butler.core
  (:require [cljs.nodejs :as nodejs]
            [butler.communicator :as com]))

(def http (nodejs/require "http"))

(defn greetHandler [req res]
  (doto res
    (.writeHead 200 {"Content-Type" "text/plain"})
    (.end (com/GET "http://localhost:5984/nitro-books"))))

(defn server [handler port url]
  (-> (.createServer http handler)
      (.listen port url)))

(defn -main [& mess]
  (server greetHandler 1337 "127.0.0.1")
  (println "Server running at http://127.0.0.1:1337/"))

(set! *main-cli-fn* -main)
