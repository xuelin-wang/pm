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
         lists (map (fn [ii] {:id (str "id_" ii) :name (str "name " ii) :value (str "value " ii)}) (range 100))]
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

(reg-event-db
 :pm-set-editing-id
 []
 (fn [db [id editable item-id]]
   (update-in db [:pm :data :editing-id] #(if editable item-id nil))))

(reg-event-db
 :pm-update-item
 []
 (fn [db [id name-or-value item-id val]]
   (let [lists-path [:pm :data :lists]
         pm-lists (get-in db lists-path)
         new-pm-lists
           (map
            (fn [item]
              (if (= item-id (:id item)) (assoc item name-or-value val) item)) pm-lists)]
     (update-in db lists-path (constantly new-pm-lists)))))
