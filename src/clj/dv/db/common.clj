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
                     (str insert-strs-meta-sql-0 " ('" (dv.utils/new-uuid) "', 'auth', 'nonce', 1, 0, 'nonce')")
                     (str insert-strs-meta-sql-0 " ('" (dv.utils/new-uuid) "', 'auth', 'password', 1, 0, 'password')")
                     (str insert-strs-meta-sql-0 " ('" (dv.utils/new-uuid) "', 'auth', 'encryption', 10, 0, 'encryption')")
                     (str insert-strs-meta-sql-0 " ('" list-list-strs-id "', 'auth', 'list_list', 1000, 1, 'list list')")
                     (str insert-strs-sql-0 " ('', '" list-list-strs-id "', 0, 'default' )")
                     (str insert-strs-sql-0 " ('', '" list-list-strs-id "', 1, '" (dv.utils/new-uuid) "' )")
                     (str insert-strs-sql-0 " ('', '" list-list-strs-id "', 2, 'admin' )")
                     (str insert-strs-sql-0 " ('', '" list-list-strs-id "', 3, '" (dv.utils/new-uuid) "' )")]))))))

(defn migrate []
  (let [db (db-conn)]
    ;;    (drop-schemas db)
    (when-not (has-table? db "auth") (create-schema-auth db))
    (when-not (has-table? db "enc") (create-schema-enc db))
    (when-not (has-table? db "strs_meta") (create-schema-strs-meta db))
    (when-not (has-table? db "strs") (create-schema-strs db))
    (init-db db)))
