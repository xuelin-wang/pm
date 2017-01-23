(ns dv.crypt
  (:require [goog.crypt]
            [goog.crypt.Aes]
            [goog.crypt.Sha256]
    ))

(defn byte-array-to-hex [bytes]
  (goog.crypt/byteArrayToHex (clj->js bytes)))

(defn hex-to-byte-array [hex-str]
  (js->clj (goog.crypt/hexToByteArray hex-str)))

(defn byte-array-to-str [bytes]
  (goog.crypt/byteArrayToString (clj->js bytes)))

(defn str-to-byte-array [str]
  (js->clj (goog.crypt/stringToByteArray str)))

(defn to-hash256 [str]
  (let [sha256 (goog.crypt.Sha256.)
        bytes (goog.crypt/stringToByteArray str)
        hash-bytes (.update sha256 bytes)]
       (js->clj hash-bytes)
    )
  )

(defn new-aes [key-str]
  (let [sha256 (goog.crypt.Sha256.)
        bytes (goog.crypt/stringToByteArray key-str)
        hash-bytes (.update sha256 bytes)]
    (goog.crypt.Aes. hash-bytes)
    )
 )

(defn aes-encrypt [aes txt]
  (.encrypt aes txt))

(defn aes-decrypt [aes txt]
  (.decrypt aes txt))

