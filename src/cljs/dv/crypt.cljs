(ns dv.crypt
  (:require [goog.crypt]
            [goog.crypt.Aes]
            [goog.crypt.Sha256]))

(def block-size 16)

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

(defn padding-js-bytes
  [bs target-len padding-byte]
  (let [mod-len (mod (count bs) target-len)
        padding-len (if (zero? mod-len) 0 (- target-len mod-len))]
    (.concat bs (clj->js (repeat (- target-len mod-len) padding-byte)))))

(defn new-aes [key-str]
  (let [
        hash-bytes (byte-array-to-hash256 (str-to-byte-array key-str true) true)]
    (goog.crypt.Aes. hash-bytes)))

(defn aes-encrypt-bytes [aes bytes js?]
  (let [encrypted (.encrypt aes (if js? bytes (clj->js bytes)))]
    (if js? encrypted (js->clj encrypted))))

(defn aes-encrypt-str [aes ss js?]
  (let [js-bytes (str-to-byte-array ss true)
        padding-byte 32
        padded-bytes (padding-js-bytes js-bytes block-size padding-byte)
        partitioned (partition block-size (js->clj padded-bytes))
        encoded-bytes (mapcat identity (map #(aes-encrypt-bytes aes % false) partitioned))]
    (if js? (clj->js encoded-bytes) encoded-bytes)))

(defn aes-decrypt-bytes [aes bytes js?]
  (let [
        cbytes (if js? (js->clj bytes) bytes)
        partitioned (partition block-size cbytes)
        decoded-bytes (mapcat identity (map #(.decrypt aes (clj->js %)) partitioned))]
    (if js? (clj->js decoded-bytes) decoded-bytes)))
