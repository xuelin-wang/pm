(ns dv.routes.home
  (:require [dv.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.util.http-response :as response]
            [dv.admin :as admin]
            [dv.auth :as auth]
            [dv.pm :as pm]
            [dv.gql.graphql :as graphql]
            [cheshire.core :as json]
            [clojure.java.io :as io]))

(defn get-base-url [request]
  (str (-> request :scheme name)
       "://"
       (get-in request [:headers "host"])))

(defn home-page [init]
  (layout/render "home.html" {:init init}))

(defn get-current-auth-name [request]
  (get-in request [:session :auth-name]))

(def permission-denied {:error "Permission denied"})

(defroutes home-routes
  (GET "/" []
       (home-page nil))
  (GET "/register_ok" []
       (home-page "{\"register\":1}"))
  (GET "/register_fail" []
       (home-page "{\"register\":0}"))
  (GET "/graphql" [schema query variables :as request]
       (println "GET query: " query)
       (response/ok
        (graphql/execute query variables)))
  (POST "/graphql" [schema query variables :as request]
        (println "POST query: " query)
        ;; (println "Post variables: " (json/parse-string variables))
        (response/ok
         (try
           (graphql/execute query (json/parse-string variables))
           (catch Throwable e
             (println e)))))

  (route/resources "/iql" {:root "public/iql/build"})

  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8"))))

(defroutes pm-get-routes

  (GET "/pm_get_list" [auth-name list-name :as request]
       (let [results
             (if (nil? (get-current-auth-name request))
               {:data permission-denied}
               {:data (pm/get-pm-list auth-name list-name)})]
         (response/ok results)))

  (GET "/pm_add_item" [auth-name list-name name value :as request]
       (if (nil? (get-current-auth-name request)) (response/ok {:data permission-denied})
         (let [item-id (pm/add-item-to-list auth-name list-name name value)
               results {:id item-id}]

           (response/ok {:id item-id}))))


  (GET "/pm_update_item" [auth-name list-name id name value :as request]
       (if (nil? (get-current-auth-name request)) (response/ok {:data permission-denied})
         (let [item-id (pm/update-list-item auth-name list-name id name value)
               results {:id item-id}]

           (response/ok {:id item-id}))))

  (GET "/auth_register" [auth-name nonce password :as request]
       (let [base-url (get-base-url request)
             results {:data (auth/register auth-name nonce password base-url)}]
         (let [resp (response/ok results)] resp)))

  (GET "/auth_confirm_registration" [auth-id confirm :as request]
       (let [
             results (auth/confirm-registration auth-id confirm)]
         {:status 302
          :headers {"Location" (if (nil? results) "/register_fail" "/register_ok")}
          :body ""}))

  (GET "/auth_login" [auth-name password :as request]
       (let [results {:data (auth/login auth-name password)}]
         (let [resp (response/ok results)] (assoc resp :session {:auth-name auth-name}))))


  (GET "/auth_nonce" [auth-name :as request]
   (let [nonce (auth/nonce auth-name)]
     (response/ok {:nonce nonce})))


  (GET "/auth_logout" [auth-name :as request]
       (let [results {:data (auth/logout auth-name)}]
         (let [resp (response/ok results)] (assoc resp :session {:auth-name nil})))))


(defroutes pm-post-routes

  (POST "/admin" [p :as request]
    (if (auth/is-admin? (get-current-auth-name request))
      (let [param (json/parse-string p)
            script-type (get param "script-type")
            script (get param "script")
            results {:data (admin/execute script-type script)}]
        (response/ok results))
      (response/ok {:data permission-denied}))))
