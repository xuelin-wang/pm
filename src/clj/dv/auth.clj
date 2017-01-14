(ns dv.auth
  (:require
   [dv.db.common :as db]
   [clojure.spec :as s]))

(defn login [name password]
  (let [db (db/db-conn)]
    (db/valid-auth? db name password)))


(defn logout [name]
  (let [db (db/db-conn)]
    (println (str "auditing logout: " name))))

(defn register [name password]
  (let [db (db/db-conn)]
    (db/add-auth db name password)))
