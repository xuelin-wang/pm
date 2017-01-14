(ns dv.pm
  (:require
   [dv.db.common :as db]
   [clojure.string]
   [clojure.spec :as s]))

(defn- get-list-name [type list-name0]
  (if (clojure.string/blank? list-name0) "default_list" list-name0))

(defn add-item-to-list [auth-name list-name0 name value]
  (let [db (db/db-conn)
        list-name (get-list-name "auth" list-name0)
        list-id (db/get-strs-id db "auth" list-name)]
    (db/append-strs db auth-name list-id [name value])))

(defn get-list [auth-name list-name0]
  (let [db (db/db-conn)
        list-name (get-list-name "auth" list-name0)]
    (db/get-list db auth-name list-name)))
