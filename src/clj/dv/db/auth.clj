(ns dv.db.auth
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource))
  (:require
            [clojure.spec :as s]
            [clojure.java.jdbc :as j]
            [dv.db.strs :refer [get-strs-by-owner-name get-strs-id save-strs]]
            [dv.crypt :refer [aes-decrypt aes-encrypt new-aes]]
            [dv.utils :refer [new-uuid]])
  )

(defn- get-password-strs-id [db] (get-strs-id db "auth" "password"))
(defn- get-nonce-strs-id [db] (get-strs-id db "auth" "nonce"))

(defn has-auth? [db]
  (let [sql "select count(*) as count from auth"
        results (j/query db [sql])]
    (pos? (:count (first results)))))

(defn exists-auth? [db auth-name]
  {:pre [(s/valid? string? auth-name)]}
  (let [curr-auths (j/query db ["select * from auth where verified = 1 and auth_name = ?" auth-name])]
    (seq curr-auths)))

(defn add-auth [db auth-name nonce password verified]
  {:pre [(s/valid? string? auth-name) (s/valid? string? password)]}
  (if (exists-auth? db auth-name)
    nil
    (let [
          uuid (new-uuid)
          curr-millis (System/currentTimeMillis)
          confirm (aes-encrypt (new-aes uuid) (str curr-millis))
          nonce-strs-id (get-nonce-strs-id db)
          pw-strs-id (get-password-strs-id db)]
      (j/insert! db :auth {:auth_id uuid :verified verified :auth_name auth-name})
      (save-strs db uuid nonce-strs-id [nonce])
      (save-strs db uuid pw-strs-id [password])
      {:auth-id uuid :confirm confirm})))

(defn check-auth-registration [db auth-id confirm]
  {:pre [(s/valid? string? auth-id) (s/valid? string? confirm)]}
  (let [
        curr-millis (System/currentTimeMillis)
        confirm-millis-str (aes-decrypt (new-aes auth-id) confirm)
        confirm-millis (Long/parseLong confirm-millis-str)
        delta-millis (- curr-millis confirm-millis)
        millis-checked? (and (pos? delta-millis) (< delta-millis 7200000))]
    (if millis-checked?
      (do
        (j/execute! db ["update auth set verified = 1 where auth_id = ? and verified = 0" auth-id])
        {:auth-id auth-id})
      nil)))


(defn- get-auths [db verified auth-name]
  {:pre (s/valid? string? auth-name)}
  (let [sql "select auth_id from auth where verified = ? and auth_name = ?"
        results (j/query db [sql verified auth-name])]
    results))

(defn valid-auth? [db auth-name password]
  {:pre [(s/valid? string? auth-name) (s/valid? string? password)]}
  (let [auth-id (:auth_id (first (get-auths db 1 auth-name)))]
    (if (nil? auth-id) false
                       (let [
                             strs (get-strs-by-owner-name db auth-id "auth" "password")
                             found-pw (:str (first strs))]
                         (= password found-pw)))))

(defn get-nonce [db auth-name]
  {:pre [(s/valid? string? auth-name)]}
  (let [auth-id (:auth_id (first (get-auths db 1 auth-name)))
        strs (get-strs-by-owner-name db auth-id "auth" "nonce")
        nonce (:str (first strs))]
    nonce))
