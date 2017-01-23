(ns dv.crypt
  (:import [dv.enc AES CryptUtil]
           )
  (:require
   [clojure.spec :as s]
   [dv.utils]))

(defn get-enc-key [auth-name] ())

(defn get-admin-enc-key [] ())

(defn byte-array-to-hex [bytes]
  (CryptUtil/toHex bytes))

(defn hex-to-byte-array [hex-str]
  (CryptUtil/hexStringToBytes hex-str))

(defn byte-array-to-str [bytes]
  (String. bytes))

(defn str-to-byte-array [str]
  (.getBytes str))

(defn to-hash256 [str]
  (CryptUtil/toHash256 str)
  )

(defn new-aes [key]
    (AES. key)
  )

(defn aes-encrypt [aes txt]
  (.encrypt aes txt))

(defn aes-decrypt [aes txt]
  (.decrypt aes txt))
