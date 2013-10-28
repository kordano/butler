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

(defn routing [handler & options]
  (fn [req-chan]
    (go (let [resp (<! (handler req-chan))]
          (cond (re-find #"^/get" (:url resp))
                (merge resp (<! (request {:hostname "localhost"
                                          :port 5984
                                          :path "/nitro-books/168bd6ab987b7161ffd61449ee000d2a"})))

                :else
                (assoc resp
                  :status 404
                  :data "Not found."))))))

(defn ring [handler req res]
  (let [ring-req (-> {:headers (js->clj (.-headers req) {:keywordize-keys true})}
                     (assoc :method (.-method req))
                     (assoc :url (.-url req)))
        req-chan (go ring-req)]
    (go (let [ring-res (<! (handler req-chan))]
          (println "RES-REQ: " (str ring-res))
          (.writeHead res
                      (or (:status ring-res) 500)
                      {"Content-Type" (or (:content-type ring-res) "text/plain")})
          (.end res (str ring-res))))))

(def app (-> identity
             (routing)))

(defn server [handler port url]
  (-> (.createServer http handler)
      (.listen port url)))

(defn -main [& args]
  (server (partial ring app) 1337 "127.0.0.1")
  (println "Server running at http://127.0.0.1:1337/"))

(set! *main-cli-fn* -main)
