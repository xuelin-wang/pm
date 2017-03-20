(ns dv.db.strs
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource))
  (:require [to-jdbc-uri.core :refer [to-jdbc-uri]]
            [clojure.spec :as s]
            [clojure.java.jdbc :as j]
            [dv.db.common]
            )
  )

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

(defn get-strs [db owner-id strs-id]
  {:pre [(s/valid? string? owner-id) (s/valid? string? strs-id)]}
  (let [sql "select str_id, str from strs where owner_id = ? and strs_id = ? order by str_id"
        results (j/query db [sql owner-id strs-id])]
    results))

(defn get-strs-by-owner-name [db owner-id owner-type strs-name]
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

(defn update-strs [db owner-id strs-id str-id strs]
  {:pre [(s/valid? string? owner-id) (s/valid? string? strs-id)]}
  (doall
    (map-indexed
      (fn [idx item]
        (j/execute! db ["update strs set str = ? where owner_id = ? and strs_id = ? and str_id = ?",
                        item, owner-id, strs-id, (+ (Integer/parseInt str-id) idx)]))
      strs)))

(defn strs-to-tuples [strs tuple-elements]
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

(defn save-strs [db owner-id strs-id strs]
  {:pre [(s/valid? string? owner-id) (s/valid? string? strs-id)]}
  (j/execute! db ["delete from strs where owner_id = ? and  strs_id = ?" owner-id strs-id])
  (if (seq strs)
    (doall (map-indexed
             (fn [idx item]
               (j/insert! db :strs {:owner_id owner-id :strs_id strs-id :str_id idx :str item}))
             strs))))
