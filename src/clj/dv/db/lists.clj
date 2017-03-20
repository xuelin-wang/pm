(ns dv.db.lists
  (:require [to-jdbc-uri.core :refer [to-jdbc-uri]]
            [clojure.spec :as s]
            [dv.db.strs :refer [get-strs get-strs-by-owner-name strs-to-tuples]]
            )
  )

(defn get-list-list [db]
  (let
    [tuples (strs-to-tuples (get-strs-by-owner-name db "" "auth" "list_list") [:name :value])]
    (into {} (map (fn [tuple] [(:name tuple) (:value tuple)]) (vals tuples)))))

(defn get-list [db owner-id list-name]
  {:pre [(s/valid? string? owner-id) (s/valid? string? list-name)]}
  (let [
        list-id (get (get-list-list db) list-name)
        list-strs (get-strs db owner-id list-id)]
    (strs-to-tuples list-strs [:name :value])))