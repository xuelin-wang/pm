(ns dv.auth
  (:require
   [dv.db.common :as db]
   [clojure.spec :as s]))

(defn is-admin? [auth-name]
  (= auth-name "xlpwman@gmail.com"))

(defn login [auth-name password]
  (let [db (db/db-conn)
        login? (db/valid-auth? db auth-name password)]
    {:login? login? :is-admin? (and login? (is-admin? auth-name))}))

(defn logout [auth-name]
  (let [db (db/db-conn)]
    (println (str "auditing logout: " auth-name))))

(defn register [auth-name password]
  (let [db (db/db-conn)]
    (db/add-auth db auth-name password)))

(defn get-emailer [] ())
