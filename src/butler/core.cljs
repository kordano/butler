(ns butler.core
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :as async :refer [chan <! >! timeout close! put!]]
            [cljs.nodejs :as nodejs])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))

#_(do (ns butler.clojure.repl)
      (require '[cljs.repl.node :as node])
      (node/run-node-repl))


(def http (nodejs/require "http"))

(defn request [options]
  (let [res-c (chan)]
    (-> (.request http (clj->js options)
                  (fn [res]
                    (let [res-a (atom {:status (-> res .-statusCode)})]
                      (.setEncoding res "utf8")
                      (.on res "data" #(swap! res-a update-in [:data] str %))
                      (.on res "end" #(go (>! res-c @res-a))))))
        (.on "error" #(go (>! res-c {:error (-> % .-message)})))
        (.end))
    res-c))

(defn greetHandler [req res]
  (.writeHead res 200 {"Content-Type" "text/plain"})
  (println "HEADERS: " (js->clj (.-headers req) {:keywordize-keys true}))
  (println "METHOD: " (.-method req))
  (println "URL: " (.-url req))
  (go (.end res (str (<! (request {:hostname "localhost"
                                   :port 5984
                                   :path "/nitro-books"}))))))

(defn server [handler port url]
  (-> (.createServer http handler)
      (.listen port url)))

(defn -main [& args]
  (server greetHandler 1337 "127.0.0.1")
  (println "Server running at http://127.0.0.1:1337/"))

(set! *main-cli-fn* -main)
