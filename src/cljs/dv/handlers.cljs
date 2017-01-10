(ns dv.handlers
  (:require [dv.db :as db]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    db/default-db))

(reg-event-db
 :auth-login
 []
 (fn [db [_]]
   (let [db-login (update-in db [:pm :auth :login] (constantly true))
         db-admin-login (update-in db-login [:pm :auth :admin] (constantly true))
         lists
         (
          into {}
          (map
              (fn [ii] [(str "id_" ii) {:id (str "id_" ii) :name (str "name " ii) :value (str "value " ii)}])
              (range 100)))]
       (assoc-in db-admin-login [:pm :data :lists] lists))))

(reg-event-db
 :auth-register
 []
 (fn [db [_]]
   (update-in db [:pm :auth :login] (constantly true))))

(reg-event-db
 :update-value
 []
 (fn [db [_ value-path val]]
   (update-in db value-path (constantly val))))

(reg-event-db
 :process-admin-response
 []
 (fn [db [_ response]]
   (-> db
       (assoc-in [:admin :loading?] false)
       (assoc-in [:admin :results] response))))

(reg-event-db
 :process-admin-error
 []
 (fn [db [_ error]]
   (-> db
       (assoc-in [:admin :loading?] false)
       (assoc-in [:admin :results] error))))

(reg-event-fx
 :admin-execute-script
 []
 (fn [{:keys [db]} [_]]
   (let
     [script (get-in db [:admin :script])]
     {:http-xhrio {:method          :get
                   :uri             "/admin"
                   :params          {:query script}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-admin-response]
                   :on-failure      [:process-admin-error]}
      :db  (assoc-in db [:admin :loading?] true)})))
