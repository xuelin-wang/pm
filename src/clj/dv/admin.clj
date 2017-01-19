(ns dv.admin
  (:require
   [cheshire.core :as json]
   [clojure.java.jdbc :as j]
   [dv.db.common :as db]
   [dv.commonutils]
   [clojure.spec :as s]))

(defn- execute-sql-query [query]
  (let [db (db/db-conn)
        results (j/query db [query] {:as-arrays? true})]
    {:data results}))

(defn- execute-sql-mutation [query]
  (let
    [db (db/db-conn)
     results (j/execute! db [query])]
    {:data results}))

(defn- execute-clj [script]
  (let
    [
     results (eval (read-string script))]
    {:data results}))

(defn execute [script-type script]
  (let []
    (case script-type
      "sql_query" (execute-sql-query script)
      "sql_mutation" (execute-sql-mutation script)
      "clojure" (execute-clj script)
      {:error (str "type must be in " dv.commonutils/admin-script-types ": " script-type)})))
