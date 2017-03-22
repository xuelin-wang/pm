(ns dv.pm
  (:require
   [dv.db.common :refer [db-conn]]
   [dv.db.lists :refer [get-list get-list-list]]
   [dv.db.strs :refer [append-strs update-strs]]
   [clojure.string]
   [dv.crypt]
   [clojure.spec :as s]))

(defn- get-list-name [type list-name0]
  (if (clojure.string/blank? list-name0) "default" list-name0))

(defn update-list-item [auth-name list-name0 id name value]
  (let [db (db-conn)
        list-name (get-list-name "auth" list-name0)
        list-list (get-list-list db)
        list-id (get list-list list-name)]
    (update-strs db auth-name list-id id [name value])))

(defn add-item-to-list [auth-name list-name0 name value]
  (let [db (db-conn)
        list-name (get-list-name "auth" list-name0)
        list-list (get-list-list db)
        list-id (get list-list list-name)]
    (append-strs db auth-name list-id [name value])))

(defn get-pm-list [auth-name list-name0]
  (let [db (db-conn)
        list-name (get-list-name "auth" list-name0)]
    (get-list db auth-name list-name)))

(defn exportcsv-pm-list [auth-name list-name0]
  (let [db (db-conn)
        list-name (get-list-name "auth" list-name0)
        tuples (get-list db auth-name list-name)
        ]
    (str tuples)))
