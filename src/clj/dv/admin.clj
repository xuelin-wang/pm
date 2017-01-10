(ns dv.admin
  (:require
   [cheshire.core :as json]
   [clojure.java.jdbc :as j]
   [dv.db.common :as db]
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

(defn execute [query]
  (let [query-json (json/parse-string query)
        type (get query-json "type")
        query-str (get query-json "query")]
    (case type
      "sql_query" (execute-sql-query query-str)
      "sql_mutation" (execute-sql-mutation query-str)
      {:error (str "type must be sql_query, sql_mutation: " type)})))
