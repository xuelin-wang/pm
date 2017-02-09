(ns dv.handlers
  (:require [dv.db]
            [dv.crypt]
            [ajax.core :as ajax]
            [day8.re-frame.http-fx]
            [goog.crypt]
            [re-frame.core :refer [dispatch reg-event-db reg-event-fx]]))

(reg-event-db
  :initialize-db
  (fn [_ _]
    dv.db/default-db))

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

(defn url-encode [ss] (js/encodeURIComponent ss))
(defn url-encode-map [mm]
  (let [nvs (into [] mm)
        encoded-nvs (map #(into [] (map url-encode %))
                         nvs)]
    (into {} (into [] encoded-nvs))))

(defn clj->json [ds] (.stringify js/JSON (clj->js ds)))
(defn clj->url-encoded-json [ds] (url-encode (clj->json ds)))

(reg-event-fx
 :admin-execute-script
 []
 (fn [{:keys [db]} [_]]
   (let
     [admin (:admin db)
      script-type (or (:script-type admin) "sql_query")
      params {:script-type script-type :script (:script admin)}]
     {:http-xhrio {:method          :post
                   :uri             "/admin"
                   :format           (ajax/url-request-format)
                   :params          {:p (clj->url-encoded-json params)}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-admin-response]
                   :on-failure      [:process-admin-response]}
      :db  (assoc-in db [:admin :loading?] true)})))

(reg-event-fx
 :process-register-response
 []
 (fn [{:keys [db]} [_ response]]
   (let [registered? (:data response)
         new-db (-> db
                    (assoc-in [:pm :auth :regestring?] false)
                    (assoc-in [:pm :auth :register :error]
                              (if registered? "" "There exists an account for the email address already"))
                    (assoc-in [:pm :auth :register :msg]
                              (if registered? "Please check your email and click the confirm link"
                                "")))]
     {:db new-db})))



(defn hash-auth [auth]
  {:auth-name (:auth-name auth) :password
   (-> (:password auth)
       (dv.crypt/str-to-byte-array true)
       (dv.crypt/byte-array-to-hash256 true)
       (dv.crypt/byte-array-to-hash256 true)
       (dv.crypt/byte-array-to-hex true))})


(defn encrypt [vals key]
  (let [aes (dv.crypt/new-aes key)]
    (map #(dv.crypt/byte-array-to-hex (dv.crypt/aes-encrypt-str aes %) true) vals)))

(defn decrypt [vals key]
  (let [aes (dv.crypt/new-aes key)]
    (map
     #(dv.crypt/byte-array-to-str (dv.crypt/aes-decrypt-bytes aes (dv.crypt/hex-to-byte-array % true) true) true)
     vals)))

(reg-event-fx
 :auth-register
 []
 (fn [{:keys [db]} [_]]
   (let
     [auth (get-in db [:pm :auth])]
     {:http-xhrio {:method          :get
                   :uri             "/auth_register"
                   :params          (hash-auth auth)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-register-response]
                   :on-failure      [:process-register-response]}
      :db  (assoc-in db [:auth :loading?] true)})))


(reg-event-fx
 :process-login-response
 []
 (fn [{:keys [db]} [_ response]]
   (let [data (:data response)
         login? (:login? data)
         db1 (assoc-in db [:pm :auth :login :error]
                       (if login? "" "Email address or password doesn't match"))
         db2 (assoc-in db [:pm :auth] (merge (get-in db1 [:pm :auth]) data))]

     (if login?
       {:db db2
         :dispatch [:pm-get-list nil]}
       {:db db2}))))

(reg-event-fx
 :auth-login
 []
 (fn [{:keys [db]} [_]]
   (let
     [auth (get-in db [:pm :auth])]
     {:http-xhrio {:method          :get
                   :uri             "/auth_login"
                   :params          (hash-auth auth)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-login-response]
                   :on-failure      [:process-login-response]}})))

(reg-event-db
 :dummy
 []
 (fn [db [_]] db))

(reg-event-fx
 :auth-logout
 []
 (fn [{:keys [db]} [_]]
   (let
     [auth (get-in db [:pm :auth])]
     {:http-xhrio {:method          :get
                   :uri             "/auth_logout"
                   :params          (select-keys auth [:auth-name])
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:dummy]
                   :on-failure      [:dummy]}
      :db dv.db/default-db})))

(defn decrypt-list [id-to-list key]
              (let [entries (into [] id-to-list)
                    decoded-entries (map
                                     (fn [key {:keys [name value]}]
                                       (let [[decoded-name decoded-value] (decrypt [name value] key)]
                                         (key {:name decoded-name :value decoded-value}))
                                       [key {:name :value}])
                                     entries)]
                (into {} decoded-entries)))

(reg-event-db
 :process-pm-list-response
 []
 (fn [db [_ response]]
   (-> db
       (assoc-in [:pm :data :loading?] false)
       (assoc-in [:pm :data :list] (decrypt-list (:data response) (get-in db [:pm :auth :password]))))))

(reg-event-fx
 :pm-get-list
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
   (let [new-item-id (:id response)
         new-row (get-in db [:pm :data :new-row])
         new-item (assoc new-row :id new-item-id)
         pm-list (get-in db [:pm :data :list] {})]

     (assoc-in db [:pm :data :list] (merge pm-list {new-item-id new-item})))))

(reg-event-fx
 :pm-add-item
 []
 (fn [{:keys [db]} [_ list-name]]
   (let
     [new-row (get-in db [:pm :data :new-row])
      [encoded-name encoded-val] (encrypt [(:name new-row) (:value new-row)] (get-in db [:pm :auth :password]))
      encoded-new-row {:name encoded-name :value encoded-val}]
     {:http-xhrio {:method          :get
                   :uri             "/pm_add_item"
                   :params          (assoc encoded-new-row :list-name list-name :auth-name (get-in db [:pm :auth :auth-name]))
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:process-pm-add-item-response]
                   :on-failure      [nil]}
      :db  db})))
