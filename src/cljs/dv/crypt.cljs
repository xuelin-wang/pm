(ns dv.crypt
  (:require [goog.crypt]
            [goog.crypt.Aes]
            [goog.crypt.Sha256]))


(defn byte-array-to-hex [bs]
  (goog.crypt/byteArrayToHex (clj->js bs)))

(defn hex-to-byte-array [hex-str]
  (js->clj (goog.crypt/hexToByteArray hex-str)))

(defn byte-array-to-str [bs]
  (goog.crypt/byteArrayToString (clj->js bs)))

(defn str-to-byte-array [ss]
  (js->clj (goog.crypt/stringToByteArray ss)))

(defn js-to-hash256 [ss]
  (let [sha256 (goog.crypt.Sha256.)
        bs (goog.crypt/stringToByteArray ss)
        _ (.update sha256 bs)]
    (.digest sha256)))

(defn to-hash256-str [ss]
  (goog.crypt/byteArrayToString (js-to-hash256 ss)))


(defn padding-js-bytes
  [bs target-len padding-byte]
  (let [mod-len (mod (count bs) target-len)]
    (if (zero? mod-len) bs
      (.concat bs (clj->js (repeat (- target-len mod-len) padding-byte))))))

(defn new-aes [key-str]
  (let [
        hash-bytes (js-to-hash256 key-str)]
    (goog.crypt.Aes. hash-bytes)))

(defn aes-encrypt-js-bytes [aes js-bytes]
  (.encrypt aes js-bytes))

(defn aes-encrypt-str [aes ss]
  (let [js-bytes (goog.crypt/stringToByteArray ss)
        padded-bytes (padding-js-bytes js-bytes 16 0)]
    (aes-encrypt-js-bytes aes padded-bytes)))

(defn aes-decrypt-js-bytes [aes js-bytes]
  (.decrypt aes js-bytes))
