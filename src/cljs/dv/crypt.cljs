(ns dv.crypt
  (:require [goog.crypt]
            [goog.crypt.Aes]
            [goog.crypt.Sha256]))


(defn byte-array-to-hex [bs js?]
  (goog.crypt/byteArrayToHex (if js? bs (clj->js bs))))

(defn hex-to-byte-array [hex-str js?]
  (let [bs (goog.crypt/hexToByteArray hex-str)]
    (if js? bs (js->clj bs))))

(defn byte-array-to-str [bs js?]
  (goog.crypt/byteArrayToString (if js? bs (clj->js bs))))

(defn str-to-byte-array [ss js?]
  (let [bs (goog.crypt/stringToByteArray ss)]
    (if js? bs (js->clj bs))))

(defn byte-array-to-hash256 [bs js?]
  (let [sha256 (goog.crypt.Sha256.)
        bs (if js? bs (clj->js bs))
        _ (.update sha256 bs)
        hash-bytes (.digest sha256)]
    (if js? hash-bytes (clj->js hash-bytes))))

(defn- padding-js-bytes
  [bs target-len padding-byte]
  (let [mod-len (mod (count bs) target-len)]
    (if (zero? mod-len) bs
      (.concat bs (clj->js (repeat (- target-len mod-len) padding-byte))))))

(defn new-aes [key-str]
  (let [
        hash-bytes (byte-array-to-hash256 (str-to-byte-array key-str false))]
    (goog.crypt.Aes. hash-bytes)))

(defn aes-encrypt-bytes [aes bytes js?]
  (.encrypt aes (if js? bytes (clj->js bytes))))

(defn aes-encrypt-str [aes ss]
  (let [js-bytes (str-to-byte-array ss true)
        padded-bytes (padding-js-bytes js-bytes 16 0)]
    (aes-encrypt-bytes aes padded-bytes true)))

(defn aes-decrypt-bytes [aes bytes js?]
  (.decrypt aes (if js? bytes (clj->js bytes))))
