(ns dv.handlers
  (:require [dv.db :as db]
            [re-frame.core :refer [dispatch reg-event-db]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
  :set-active-page
  (fn [db [_ page]]
    (assoc db :page page)))

(reg-event-db
  :set-docs
  (fn [db [_ docs]]
    (assoc db :docs docs)))

(reg-event-db
  :set-pm
  (fn [db [_ pm]]
    (assoc db :pm pm)))

(reg-event-db
 :auth-set-name
 []
 (fn [db [id email]]
   (update-in db [:pm :auth :auth-name] (constantly email))))

(reg-event-db
 :auth-set-password
 []
 (fn [db [id password]]
   (update-in db [:pm :auth :password] (constantly password))))

(reg-event-db
 :pm-set-filter
 []
 (fn [db [id filter-str]]
   (update-in db [:pm :data :filter] #(if (clojure.string/blank? filter-str) nil (.trim filter-str)))))

(reg-event-db
 :auth-login
 []
 (fn [db [id email password]]
   (let [db-login (update-in db [:pm :auth :login] (constantly true))
         lists (map (fn [ii] {:name (str "name " ii) :value (str "value " ii)}) (range 100))]
       (assoc-in db-login [:pm :data :lists] lists))))





(reg-event-db
 :auth-register
 []
 (fn [db [id email password]]
   (update-in db [:pm :auth :login] (constantly true))))

(reg-event-db
 :auth-set-registering
 []
 (fn [db [id registering]]
   (update-in db [:pm :auth :registering] (constantly registering))))
