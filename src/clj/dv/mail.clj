(ns dv.mail
  (:import
   (dv.util Gmail))
  (:require
   [dv.db.common :as db]
   [clojure.string]
   [dv.crypt]
   [clojure.spec :as s]))

(defn send-mail [tos ccs subject body mime-type]
  {:pre [(s/valid? string? tos) (s/valid? (s/nilable string?) ccs)
         (s/valid? (s/nilable string?) subject)
         (s/valid? (s/nilable string?) body)
         (s/valid? (s/nilable string?) mime-type)]}
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
