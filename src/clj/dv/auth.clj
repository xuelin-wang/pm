(ns dv.auth
  (:require
   [dv.db.common :as db]
   [dv.mail]
   [clojure.spec :as s]))

(defn is-admin? [auth-name]
  (= auth-name "xlpwman@gmail.com"))

(defn nonce [auth-name]
  (let [db (db/db-conn)
        nonce (db/nonce db auth-name)]
    nonce))

(defn login [auth-name password]
  (let [db (db/db-conn)
        login? (db/valid-auth? db auth-name password)]
    {:login? login? :is-admin? (and login? (is-admin? auth-name)) :auth-name auth-name}))

(defn logout [auth-name]
  (let [db (db/db-conn)]
    (println (str "auditing logout: " auth-name))))

(defn register [auth-name nonce password base-url]
  (let [db (db/db-conn)
        confirm? (or (not (is-admin? auth-name)) (db/has-auth? db))

        {:keys [auth-id confirm]} (db/add-auth db auth-name nonce password (if confirm? 0 1))]
    (when confirm?
      (let [confirm-link (str base-url "/auth_confirm_registration?auth-id=" auth-id "&confirm=" confirm)
            register-mail-body (str "Please confirm your password manager account registration by clicking the following link: \n"
                                    confirm-link "\n Regards\nXlpm team\n")]
        (dv.mail/send-mail auth-name nil "Please confirm password manager registration"
                           register-mail-body
                           "text/plain")))
    {:auth-id auth-id}))

(defn confirm-registration [auth-id confirm]
  (let [
        db (db/db-conn)
        check-result (db/check-auth-registration db auth-id confirm)]
    check-result))
