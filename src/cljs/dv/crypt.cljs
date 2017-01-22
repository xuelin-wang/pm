(ns dv.crypt

  (:require [goog.crypt]
    ))

(defn new-aes [key]
  ())

(defn aes-encrypt [aes txt]
  (.encrypt aes txt))

(defn aes-decrypt [aes txt]
  (.decrypt aes txt))

(defn to-hash [str bytes-count]
  (let [sha256 (js/goog.crypt.Sha256.)])
  ())