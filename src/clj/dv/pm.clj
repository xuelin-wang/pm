(ns dv.pm
  (:require
   [dv.db.common :as db]
   [clojure.string]
   [dv.crypt]
   [clojure.spec :as s]))

(defn- get-list-name [type list-name0]
  (if (clojure.string/blank? list-name0) "default" list-name0))

(defn update-list-item [auth-name list-name0 id name value]
  (let [db (db/db-conn)
        list-name (get-list-name "auth" list-name0)
        list-list (db/get-list-list db)
        list-id (get list-list list-name)]
    (db/update-strs db auth-name list-id id [name value])))

(defn add-item-to-list [auth-name list-name0 name value]
  (let [db (db/db-conn)
        list-name (get-list-name "auth" list-name0)
        list-list (db/get-list-list db)
        list-id (get list-list list-name)]
    (db/append-strs db auth-name list-id [name value])))

(defn get-list [auth-name list-name0]
  (let [db (db/db-conn)
        list-name (get-list-name "auth" list-name0)]
    (db/get-list db auth-name list-name)))
