(ns dv.auth
  (:require
   [dv.db.common :as db]
   [clojure.spec :as s]))

(defn login [auth-name password]
  (let [db (db/db-conn)]
    (db/valid-auth? db auth-name password)))

(defn logout [auth-name]
  (let [db (db/db-conn)]
    (println (str "auditing logout: " auth-name))))

(defn register [auth-name password]
  (let [db (db/db-conn)]
    (db/add-auth db auth-name password)))
