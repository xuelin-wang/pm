(ns dv.auth
  (:require
   [dv.db.common :refer [db-conn]]
   [dv.db.auth :refer [add-auth check-auth-registration get-nonce has-auth? valid-auth?]]
   [dv.mail]
   [clojure.spec :as s]))

(defn is-admin? [auth-name]
  (= auth-name "xlpwman@gmail.com"))

(defn nonce [auth-name]
  (let [db (db-conn)
        nonce (get-nonce db auth-name)]
    nonce))

(defn login [auth-name password]
  (let [db (db-conn)
        login? (valid-auth? db auth-name password)]
    {:login? login? :is-admin? (and login? (is-admin? auth-name)) :auth-name auth-name}))

(defn logout [auth-name]
  (let [db (db-conn)]
    (println (str "auditing logout: " auth-name))))

(defn register [auth-name nonce password base-url]
  (let [db (db-conn)
        confirm? (or (not (is-admin? auth-name)) (has-auth? db))

        {:keys [auth-id confirm]} (add-auth db auth-name nonce password (if confirm? 0 1))]
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
        db (db-conn)
        check-result (check-auth-registration db auth-id confirm)]
    check-result))
