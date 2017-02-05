(ns dv.pm
  (:import
   (dv.util Gmail))
  (:require
   [dv.db.common :as db]
   [clojure.string]
   [dv.crypt]
   [clojure.spec :as s]))

(defn- get-list-name [type list-name0]
  (if (clojure.string/blank? list-name0) "default" list-name0))

(defn add-item-to-list [auth-name list-name0 name value]
  (let [db (db/db-conn)
        list-name (get-list-name "auth" list-name0)
        list-list (db/get-list-list db)
        _ (print (str "list-list" list-list))
        list-id (get list-list list-name)]
    (db/append-strs db auth-name list-id [name value])))

(defn get-list [auth-name list-name0]
  (let [db (db/db-conn)
        list-name (get-list-name "auth" list-name0)]
    (db/get-list db auth-name list-name)))

(defn send-mail [tos ccs subject body mime-type]
  (let [db (db/db-conn)
        admin-strs (db/get-list db "" "admin")
        admin-strs-vals (vals admin-strs)
        key (->> admin-strs-vals
                 (filter #(= (:name %) "key"))
                 first
                 :value)
        sender (->> admin-strs-vals
                    (filter #(= (:name %) "gmail_sender"))
                    first
                    :value)
        sender-pw (->> admin-strs-vals
                       (filter #(= (:name %) "gmail_sender_password"))
                       first
                       :value)
        aes (dv.crypt/new-aes key)
        dec-sender (dv.crypt/aes-decrypt aes sender)
        dec-sender-pw (dv.crypt/aes-decrypt aes sender-pw)]
    (Gmail/send dec-sender dec-sender-pw tos ccs subject body mime-type)))
