(ns dv.handler
  (:require [compojure.core :refer [routes wrap-routes]]
            [dv.layout :refer [error-page]]
            [dv.routes.home :refer [home-routes]]
            [compojure.route :as route]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.defaults :refer :all]
            [dv.env :refer [defaults]]
            [mount.core :as mount]
            [dv.middleware :as middleware]))

(mount/defstate init-app
                :start ((or (:init defaults) identity))
                :stop  ((or (:stop defaults) identity)))

(def app-routes
  (routes
    (-> #'home-routes
;;        (wrap-routes middleware/wrap-csrf)
        wrap-json-response
        (wrap-cors :access-control-allow-origin [#"http://localhost:3000" #"http://.*"]
                   :access-control-allow-methods [:get :put :post :delete])
        (wrap-defaults api-defaults)
        (wrap-json-params)
        (wrap-routes middleware/wrap-formats))




    (route/not-found
      (:body
        (error-page {:status 404
                     :title "page not found"})))))


(defn app [] (middleware/wrap-base #'app-routes))
