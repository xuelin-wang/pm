(ns dv.events
  (:require  [re-frame.core :refer [reg-event-db reg-event-fx inject-cofx path trim-v
                                    after debug]]))

(reg-event-db
 :auth-set-name
 []
 (fn [db [id email]]
   (println email)
   (update-in db [:pm :auth :auth-name] (constantly email))))

(reg-event-db
 :auth-set-password
 []
 (fn [db [id password]]
   (println password)
   (update-in db [:pm :auth :password] (constantly password))))

(reg-event-db
 :auth-login
 []
 (fn [db _]
   (update-in db [:pm :auth :login] (constantly true))))
