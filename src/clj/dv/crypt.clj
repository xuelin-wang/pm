(ns dv.crypt
  (:import [dv.enc AES CryptUtil])
  (:require
   [clojure.spec :as s]
   [dv.utils]))

(defn get-enc-key [auth-name] ())

(defn get-admin-enc-key [] ())

(defn new-aes [key]
    (AES. key)
  )

(defn aes-encrypt [aes txt]
  (.encrypt aes txt))

(defn aes-decrypt [aes txt]
  (.decrypt aes txt))

(defn to-hash [str bytes-count]
    (CryptUtil/toHash str bytes-count)
  )