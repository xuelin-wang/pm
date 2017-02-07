(ns dv.db.common
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource))
  (:require [to-jdbc-uri.core :refer [to-jdbc-uri]]
            [clojure.spec :as s]
            [clojure.java.jdbc :as j]
            [dv.crypt]
            [dv.utils]))

(defn- has-db-url? [] (some? (System/getenv "DATABASE_URL")))

(def db-spec
  {
   :url (System/getenv "DATABASE_URL")})

(def local-db-spec
  {
     :url "postgresql://localhost:5432/pm"
     :user "pm"
     :password "pm_pw"})

(defn- db-pool
  []
  (let [cpds0 (doto (ComboPooledDataSource.)
                    (.setDriverClass "org.postgresql.Driver")
                    (.setMaxIdleTimeExcessConnections (* 5 60))
               ;; expire connections after 20 minutes of inactivity:
                    (.setMaxIdleTime (* 20 60)))
        cpds (if (has-db-url?)
               (doto cpds0
                     (.setJdbcUrl (to-jdbc-uri (:url db-spec))))
               (doto cpds0
                     (.setJdbcUrl (to-jdbc-uri (:url local-db-spec)))
                     (.setUser (:user local-db-spec))
                     (.setPassword (:password local-db-spec))))]
    {:datasource cpds}))

(def pooled-db (delay (db-pool)))
(defn db-conn [] @pooled-db)

(defn- create-schema-strs-meta [db]
  (j/db-do-commands db
    (j/create-table-ddl
     :strs_meta
     [
      [:strs_id "char(32)" "NOT NULL"]
      [:owner_type "varchar(50)" "NOT NULL"]
      [:strs_name "varchar(50)" "NOT NULL"]
      [:cardinality "int" "NOT NULL"]
      [:optional "int" "NOT NULL"]
      [:notes "varchar(50)"]
      [:constraint " PK_strs_meta PRIMARY KEY(strs_id)"]
      [:constraint " UC_strs_meta UNIQUE (owner_type, strs_name)"]])))

(defn- create-schema-strs [db]
  (j/db-do-commands db
    (j/create-table-ddl
     :strs [
            [:owner_id "char(32)" "NOT NULL"]
            [:strs_id "char(32)" "NOT NULL"]
            [:str_id "int" "NOT NULL"]
            [:str "varchar(4096)"]
            [:constraint " PK_strs PRIMARY KEY(owner_id, strs_id, str_id)"]])))

(defn- create-schema-enc [db]
  (j/db-do-commands db
    (j/create-table-ddl
     :enc [ [:enc_id "int" "NOT NULL"]
           [:enc_name "varchar(50)" "NOT NULL"]
           [:notes "varchar(256)" "NOT NULL"]
           [:constraint " UC_enc UNIQUE(enc_name) "]
           [:constraint " PK_enc PRIMARY KEY(enc_id)"]])))

(defn- create-schema-auth [db]
  (j/db-do-commands db
      (j/create-table-ddl
       :auth [
              [:auth_id "char(32)" "NOT NULL"]
              [:auth_name "varchar(64)" "NOT NULL"]
              [:verified "int" "NOT NULL"]
              [:constraint " PK_auth PRIMARY KEY(auth_id) "]])))

(defn- has-table? [db table-name]
  (let [sql (str "select count(*) as count from information_schema.tables where table_name = '" table-name "'")
        results (j/query db [sql])]
    (pos? (:count (first results)))))

(defn has-auth? [db]
  (let [sql "select count(*) as count from auth"
        results (j/query db [sql])]
    (pos? (:count (first results)))))

(defn- drop-schema [db table-name]
  (j/db-do-commands db (j/drop-table-ddl table-name)))

(defn- drop-schemas [db]
  (doseq [table-name ["auth" "enc" "strs_meta" "strs"]]
    (if (has-table? db table-name) (drop-schema db table-name))))

(defn- db-inited? [db]
  (let [sql "select count(*) as count from strs_meta where strs_name = 'password'"
        results (j/query db [sql])]
    (pos? (:count (first results)))))

(defn- init-db [db]
  (let [inited? (db-inited? db)]
    (when-not inited?
      (let [
            list-list-strs-id  (dv.utils/new-uuid)
            insert-strs-meta-sql-0 "insert into strs_meta (strs_id, owner_type, strs_name, cardinality, optional, notes) values "
            insert-strs-sql-0 "insert into strs (owner_id, strs_id, str_id, str) values "]
        (doall (map (fn [sql] (j/execute! db [sql]))
                    ["insert into enc (enc_id, enc_name, notes) values (1, 'test', 'test')"
                     (str insert-strs-meta-sql-0 " ('" (dv.utils/new-uuid) "', 'auth', 'admin-enc', 1, 0, 'admin-enc')")
                     (str insert-strs-meta-sql-0 " ('" (dv.utils/new-uuid) "', 'auth', 'password', 1, 0, 'password')")
                     (str insert-strs-meta-sql-0 " ('" (dv.utils/new-uuid) "', 'auth', 'encryption', 10, 0, 'encryption')")
                     (str insert-strs-meta-sql-0 " ('" list-list-strs-id "', 'auth', 'list_list', 1000, 1, 'list list')")
                     (str insert-strs-sql-0 " ('', '" list-list-strs-id "', 0, 'default' )")
                     (str insert-strs-sql-0 " ('', '" list-list-strs-id "', 1, '" (dv.utils/new-uuid) "' )")
                     (str insert-strs-sql-0 " ('', '" list-list-strs-id "', 2, 'admin' )")
                     (str insert-strs-sql-0 " ('', '" list-list-strs-id "', 3, '" (dv.utils/new-uuid) "' )")]))))))


(defn migrate []
  (let [db (db-conn)]
;    (drop-schemas db)
    (when-not (has-table? db "auth") (create-schema-auth db))
    (when-not (has-table? db "enc") (create-schema-enc db))
    (when-not (has-table? db "strs_meta") (create-schema-strs-meta db))
    (when-not (has-table? db "strs") (create-schema-strs db))
    (init-db db)))

(defn- get-strs-metas [db owner-type strs-name]
  (let [sql-0 "select strs_id, owner_type, strs_name, cardinality, optional, notes from strs_meta "
        sql-1 (if (not-any? some? [owner-type strs-name]) "" " where ")
        sql-2 (if (some? owner-type) " owner_type = ? " "")
        sql-3 (if (every? some? [owner-type strs-name]) " and " "")
        sql-4 (if (some? strs-name) " strs_name = ? " "")
        sql (str sql-0 sql-1 sql-2 sql-3 sql-4)
        params (filter some? [owner-type strs-name])
        results (j/query db (into [] (concat [sql] params)))]
    results))

(defn- get-strs-meta [db owner-type strs-name]
  {:pre [(s/valid? string? owner-type) (s/valid? string? strs-name)]}
  (first (get-strs-metas db owner-type strs-name)))

(defn get-strs-id [db owner-type strs-name]
  (:strs_id (get-strs-meta db owner-type strs-name)))

(defn- get-password-strs-id [db] (get-strs-id db "auth" "password"))

(defn- get-strs [db owner-id strs-id]
  {:pre [(s/valid? string? owner-id) (s/valid? string? strs-id)]}
  (let [sql "select str_id, str from strs where owner_id = ? and strs_id = ? order by str_id"
        results (j/query db [sql owner-id strs-id])]
    results))

(defn- get-strs-by-owner-name [db owner-id owner-type strs-name]
  {:pre [(s/valid? string? owner-id) (s/valid? string? owner-type) (s/valid? string? strs-name)]}
  (let [
        strs-id (get-strs-id db owner-type strs-name)
        sql "select str_id, str from strs where owner_id = ? and strs_id = ? order by str_id"
        results (j/query db [sql owner-id strs-id])]
    results))

(defn append-strs [db owner-id strs-id strs]
  {:pre [(s/valid? string? owner-id) (s/valid? string? strs-id)]}
  (let [
        db-results (j/query db ["select max(str_id + 1) as next_id from strs where owner_id = ? and strs_id = ?" owner-id strs-id])
        next-str-id0 (:next_id (first db-results))
        next-str-id (if (nil? next-str-id0) 0 next-str-id0)]
    (doall
      (map-indexed
       (fn [idx item]
         (j/execute! db ["insert into strs (owner_id, strs_id, str_id, str) values (?, ?, ?, ?)",
                         owner-id, strs-id, (+ next-str-id idx), item]))
       strs))
    next-str-id))

(defn- strs-to-tuples [strs tuple-elements]
  {:pre [(s/valid? pos? (count tuple-elements)) (= 0 (mod (count strs) (count tuple-elements)))]}
  (let [elem-cnt (count tuple-elements)
        strs-groups (partition elem-cnt strs)
        k-tuples
        (map
          (fn [strs-group]
            (let [k (:str_id (first strs-group))
                  tuple (zipmap tuple-elements (map :str strs-group))]
              [k (assoc tuple :id k)]))
          strs-groups)]
    (into {} k-tuples)))

(defn get-list-list [db]
  (let
    [tuples (strs-to-tuples (get-strs-by-owner-name db "" "auth" "list_list") [:name :value]) _ (print (str "tuples:" tuples))]
    (into {} (map (fn [tuple] [(:name tuple) (:value tuple)]) (vals tuples)))))

(defn get-list [db owner-id list-name]
  {:pre [(s/valid? string? owner-id) (s/valid? string? list-name)]}
  (let [
        list-id (get (get-list-list db) list-name)
        list-strs (get-strs db owner-id list-id)]
    (strs-to-tuples list-strs [:name :value])))

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

(defn- save-strs [db owner-id strs-id strs]
  {:pre [(s/valid? string? owner-id) (s/valid? string? strs-id)]}
  (j/execute! db ["delete from strs where owner_id = ? and  strs_id = ?" owner-id strs-id])
  (if (seq strs)
    (doall (map-indexed
            (fn [idx item]
              (j/insert! db :strs {:owner_id owner-id :strs_id strs-id :str_id idx :str item}))
            strs))))

(defn exists-auth? [db auth-name]
  {:pre [(s/valid? string? auth-name)]}
  (let [curr-auths (j/query db ["select * from auth where verified = 1 and auth_name = ?" auth-name])]
    (seq curr-auths)))

(defn add-auth [db auth-name password verified]
  {:pre [(s/valid? string? auth-name) (s/valid? string? password)]}
  (if (exists-auth? db auth-name)
    nil
    (let [
          uuid (dv.utils/new-uuid)
          curr-millis (System/currentTimeMillis)
          confirm (dv.crypt/aes-encrypt (dv.crypt/new-aes uuid) (str curr-millis))
          pw-strs-id (get-password-strs-id db)]
      (j/insert! db :auth {:auth_id uuid :verified verified :auth_name auth-name})
      (save-strs db uuid pw-strs-id [password])
      {:auth-id uuid :confirm confirm})))

(defn check-auth-registration [db auth-id confirm]
  {:pre [(s/valid? string? auth-id) (s/valid? string? confirm)]}
  (let [
        curr-millis (System/currentTimeMillis)
        confirm-millis-str (dv.crypt/aes-decrypt (dv.crypt/new-aes auth-id) confirm)
        confirm-millis (Long/parseLong confirm-millis-str)
        delta-millis (- curr-millis confirm-millis)
        millis-checked? (and (pos? delta-millis) (< delta-millis 7200000))]
    (if millis-checked?
      (do
        (j/execute! db ["update auth set verified = 1 where auth_id = ? and verified = 0" auth-id])
        {:auth-id auth-id})
      nil)))
