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

(defn home-page []
  (layout/render "home.html"))

(defroutes home-routes
  (GET "/" []
       (home-page))
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
       (let [results {:data (pm/get-list auth-name list-name)}]
         (response/ok results)))

  (GET "/pm_add_item" [auth-name list-name name value :as request]
       (let [item-id {:data (pm/add-item-to-list auth-name list-name name value)}]
         (response/ok {:id item-id})))

  (GET "/auth_register" [auth-name password :as request]
       (let [results {:data (auth/register auth-name password)}]
         (response/ok results)))

  (GET "/auth_login" [auth-name password :as request]
       (let [results {:data (auth/login auth-name password)}]
         (response/ok results)))

  (GET "/auth_logout" [auth-name :as request]
      (let [results {:data (auth/logout auth-name)}]
        (response/ok results))))

(defroutes pm-post-routes

  (POST "/admin" [p :as request]
        (println "p type:" (type p) " and params: " p)
        (println "vec in params: " (into [] (:params request)))
        (println ":p - " (get (:params request) ":p"))
        (println ":p2 - " (:p (:params request)))
        (println ":p3 - " (get (:params request) "p"))
        (println "admin POST form-params: " (:form-params request) "request: " request)
        (let [param (json/parse-string p)
              script-type (get param "script-type")
              script (get param "script")
              results {:data (admin/execute script-type script)}]
          (response/ok results))))
