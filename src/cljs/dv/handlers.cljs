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
 :update-value
 []
 (fn [db [_ value-path val]]
   (update-in db value-path (constantly val))))

(defn process-response [db [_ path response]]
  (-> db
      (assoc-in (conj path :loading?) false)
      (assoc-in (conj path :results) response)))

(reg-event-db
 :process-admin-response
 []
 (fn [db [_ response]] (process-response db [_ [:admin] response])))

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
                   :on-failure      [:process-admin-response]}
      :db  (assoc-in db [:admin :loading?] true)})))

(reg-event-db
 :process-register-response
 []
 (fn [db [_ response]]
   (let [db1 (process-response db [_ [:pm :auth :register] response])]
     (assoc-in db1 [:pm :auth :login] (not (:error response))))))

(reg-event-fx
 :auth-register
 []
 (fn [{:keys [db]} [_]]
   (let
     [auth (get-in db [:pm :auth])]
     {:http-xhrio {:method          :get
                   :uri             "/auth_register"
                   :params          {:name (:auth-name auth) :password (:password auth)}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-register-response]
                   :on-failure      [:process-register-response]}
      :db  (assoc-in db [:auth :loading?] true)})))


(reg-event-db
 :process-login-response
 []
 (fn [db [_ response]]
   (let [db1 (process-response db [_ [:pm :auth :sign-in] response])]
     (assoc-in db1 [:pm :auth :login] (:data response)))))

(reg-event-fx
 :auth-login
 []
 (fn [{:keys [db]} [_]]
   (let
     [auth (get-in db [:pm :auth])]
     {:http-xhrio {:method          :get
                   :uri             "/auth_login"
                   :params          {:name (:auth-name auth) :password (:password auth)}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-login-response]
                   :on-failure      [:process-login-response]}})))

(reg-event-fx
 :auth-logout
 []
 (fn [{:keys [db]} [_]]
   (let
     [auth (get-in db [:pm :auth])]
     {:http-xhrio {:method          :get
                   :uri             "/auth_logout"
                   :params          {:name (:auth-name auth)}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      nil
                   :on-failure      nil}
      :db  (assoc-in db [:pm :auth :login] false)})))

(reg-event-db
 :process-pm-list-response
 []
 (fn [db [_ response]]
   (let [db (process-response db [_ [:pm :data :lists] (:data response)])])))

(reg-event-fx
 :pm_get_list
 []
 (fn [{:keys [db]} [_ list-name]]
   (let
     [auth (get-in db [:pm :auth])]
     {:http-xhrio {:method          :get
                   :uri             "/pm_get_list"
                   :params          {:auth-name (:auth-name auth) :list-name list-name}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-pm-list-response]
                   :on-failure      [:process-pm-list-response]}
      :db  (assoc-in db [:pm :data :loading?] true)})))

(reg-event-db
 :process-pm-add-item-response
 []
 (fn [db [_ response]]
   (let [new-item-id (get-in response [:data :id])
         new-row (get-in db [:pm :data :new-row])
         new-item (assoc new-row :id new-item-id)
         lists (get-in db [:pm :data :lists] {})]
     (assoc-in db [:pm :data :lists] (merge lists {new-item-id new-item})))))

(reg-event-fx
 :pm-add-item
 []
 (fn [{:keys [db]} [_ list-name]]
   (let
     [new-row (get-in db [:pm :data :new-row])]
     {:http-xhrio {:method          :get
                   :uri             "/pm_add_item"
                   :params          (assoc new-row :list-name list-name :auth-name (get-in db [:pm :auth :auth-name]))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-pm-add-item-response]
                   :on-failure      [nil]}
      :db  db})))
