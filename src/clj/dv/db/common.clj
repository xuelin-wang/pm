(ns dv.db.common
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource))
  (:require [clojure.java.jdbc :as j]))

(defn has-db-url? [] (some? (System/getenv "DATABASE_URL")))

(def db-spec
  {
   :url (System/getenv "DATABASE_URL")
   :classname "org.postgresql.Driver"})

(def local-db-spec
  {
     :classname "org.postgresql.Driver"
     :url "postgresql://localhost:5432/pm"
     :user "pm"
     :password "pm_pw"})

(defn db-pool
  []
  (let [cpds0 (doto (ComboPooledDataSource.)
                    (.setDriverClass (:classname db-spec))
                    (.setMaxIdleTimeExcessConnections (* 5 60))
               ;; expire connections after 20 minutes of inactivity:
                    (.setMaxIdleTime (* 20 60)))
        cpds (if (has-db-url?)
               (doto cpds0
                     (.setJdbcUrl (str "jdbc:" (:url db-spec))))
               (doto cpds0
                     (.setJdbcUrl (str "jdbc:" (:url local-db-spec)))
                     (.setUser (:user local-db-spec))
                     (.setPassword (:password local-db-spec))))]
    {:datasource cpds}))

(defn- create-schema-strs-meta [db]
  (j/db-do-commands db
    (j/create-table-ddl
     :strs_meta
     [
      [:strs_id "int" "NOT NULL"]
      [:owner_type "varchar(50)" "NOT NULL"]
      [:name "varchar(50)" "NOT NULL"]
      [:cardinality "int" "NOT NULL"]
      [:optional "int" "NOT NULL"]
      [:notes "varchar(50)"]
      [:constraint " PK_strs_meta PRIMARY KEY(strs_id)"]
      [:constraint " UC_strs_meta UNIQUE (owner_type, name)"]])))

(defn- create-schema-strs [db]
  (j/db-do-commands db
    (j/create-table-ddl
     :strs [
            [:owner_id "int" "NOT NULL"]
            [:strs_id "int" "NOT NULL"]
            [:str_id "int" "NOT NULL"]
            [:str "varchar(4096)"]
            [:constraint " PK_strs PRIMARY KEY(owner_id, strs_id, str_id)"]])))

(defn- create-schema-enc [db]
  (j/db-do-commands db
    (j/create-table-ddl
     :enc [ [:enc_id "int" "NOT NULL"]
           [:notes "varchar(50)" "NOT NULL"]
           [:constraint " PK_enc PRIMARY KEY(enc_id)"]])))

(defn- create-schema-auth [db]
  (j/db-do-commands db
      (j/create-table-ddl
       :auth [
              [:auth_id "int" "NOT NULL"]
              [:auth_name "varchar(64)" "NOT NULL"]
              [:constraint " PK_auth PRIMARY KEY(auth_id) "]])))

(defn- create-schema-auth-enc [db]
  (j/db-do-commands db
    (j/create-table-ddl
     :auth_enc
     [
           [:auth_id "varchar(64)" "NOT NULL"]
           [:begin_ymd "int" "NOT NULL"]
           [:enc_id "int" "NOT NULL"]
           [:constraint " PK_auth_enc PRIMARY KEY(auth_id, begin_ymd)"]])))

(defn has-table? [db table-name]
  (let [sql (str "select count(*) as count from information_schema.tables where table_name = '" table-name "'")
        results (j/query db [sql])]
    (pos? (:count (first results)))))

(defn db-inited? [db]
  (let [sql (str "select count(*) as count from strs_meta where strs_id = 1")
        results (j/query db [sql])]
    (pos? (:count (first results)))))

(defn init-db [db]
  (let [inited? (db-inited? db)]
    (when-not inited?
      (doall (map (fn [sql] (j/execute! db sql))
                  ["insert into enc (enc_id, notes) values (1, 'test')"
                   "insert into strs_meta (strs_id, owner_type, name, cardinality, optional, notes) values (1, 'auth', 'password', 1, 0, 'password')"])))))


(defn migrate []
  (let [db (db-pool)]
    (when-not (has-table? db "auth") (create-schema-auth db))
    (when-not (has-table? db "enc") (create-schema-enc db))
    (when-not (has-table? db "auth_enc") (create-schema-auth-enc db))
    (when-not (has-table? db "strs_meta") (create-schema-strs-meta db))
    (when-not (has-table? db "strs") (create-schema-strs db))
    (init-db db)))
