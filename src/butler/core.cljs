(ns butler.core
  (:require [cljs.reader :refer [read-string]]
            [cljs.core.async :as async :refer [chan <! >! timeout close! put!]]
            [cljs.nodejs :as nodejs])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))


(def http (nodejs/require "http"))

(defn- write-as-json [req {data :data}] ; only JSON here for CouchDB
  (if data (do (println "PUT-DATA: " data)
               (.write req (.stringify js/JSON (clj->js data)))))
  req)

(defn request [options]
  (let [res-c (chan)]
    (-> (.request http (clj->js options)
                  (fn [res]
                    (let [res-a (atom {:status (-> res .-statusCode)})]
                      (.setEncoding res "utf8")
                      (.on res "data" #(swap! res-a update-in [:data] str %))
                      (.on res "end" #(go (>! res-c @res-a))))))
        (.on "error" #(go (>! res-c {:error (-> % .-message)})))
        (write-as-json options)
        (.end))
    res-c))

(defn routing [handler [db]]
  (fn [req-chan]
    (go (let [{:keys [url method sent-data] :as resp} (<! (handler req-chan))]
          (cond (re-find #"^/get/" url)
                (merge resp (<! (request {:hostname "localhost"
                                          :port 5984
                                          :path (str "/" db "/"
                                                     (second (re-find #"^/get/([^/\?&]+)" url)))})))

                (and (re-find #"^/commit/" url) (= "PUT" method))
                (merge resp (<! (request {:hostname "localhost"
                                          :port 5984
                                          :method "PUT"
                                          :data {:data (str sent-data)}
                                          :path (str "/" db "/"
                                                     (second (re-find #"^/commit/([^/\?&]+)" url)))})))

                :else
                (assoc resp
                  :status 404
                  :data "Not found."))))))

(defn ring [handler req res]
  (let [ring-req (-> {:headers (js->clj (.-headers req) {:keywordize-keys true})}
                     (assoc :method (.-method req))
                     (assoc :url (.-url req)))
        req-chan (chan)
        req-a (atom ring-req)]
    (.setEncoding req "utf8")
    (.on req "data" #(swap! req-a update-in [:sent-data] str %))
    (.on req "end" #(go (>! req-chan @req-a)))
    (go (let [ring-res (<! (handler req-chan))]
          (println "RES-REQ: " (str ring-res))
          (.writeHead res
                      (or (:status ring-res) 500)
                      {"Content-Type" (or (:content-type ring-res) "text/plain")})
          (.end res (str ring-res))))))

(defn server [handler port url]
  (-> (.createServer http handler)
      (.listen port url)))

(defn -main [& args]
  (let [app (-> identity
                (routing args))]
    (server (partial ring app) 13023 "0.0.0.0")
    (println "Server running at http://0.0.0.0:13023/ for local couch db:" (first args))))

(set! *main-cli-fn* -main)
