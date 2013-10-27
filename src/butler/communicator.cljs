(ns butler.communicator
  (:require [goog.net.XhrIo :as xhr]
            [cljs.reader :refer [read-string]]
            [cljs.core.async :as async :refer [chan close! put!]])
  (:require-macros [cljs.core.async.macros :refer [go alt!]]))


(defn GET [url]
  (let [ch (chan 1)]
    (xhr/send url
              (fn [event]
                (put! ch (-> event .-target .getResponseText))
                (close! ch)))
    ch))


(defn POST [url payload]
  (let [ch (chan 1)]
    (xhr/send url
              (fn [event]
                (put! ch (-> event .-target .getResponseText))
                (close! ch))
              "POST"
              payload)
    ch))
