(ns dv.routes.home
  (:require [dv.layout :as layout]
            [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.util.http-response :as response]
            [dv.admin :as admin]
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

  (GET "/admin" [query :as request]
       (println "admin GET query: " query)
       (let [results (try (admin/execute query) (catch Throwable e {:error (.getMessage e) :data {:query query}}))]
         (response/ok results)))

  (POST "/admin" [query :as request]
        (println "admin POST query: " query)
        (let [results (try (admin/execute query) (catch Throwable e {:error (.getMessage e) :data {:query query}}))]
          (response/ok results)))

  (GET "/docs" []
       (-> (response/ok (-> "docs/docs.md" io/resource slurp))
           (response/header "Content-Type" "text/plain; charset=utf-8"))))
