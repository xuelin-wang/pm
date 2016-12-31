(ns user
  (:require [mount.core :as mount]
            [dv.figwheel :refer [start-fw stop-fw cljs]]
            dv.core))

(defn start []
  (mount/start-without #'dv.core/http-server
                       #'dv.core/repl-server))

(defn stop []
  (mount/stop-except #'dv.core/http-server
                     #'dv.core/repl-server))

(defn restart []
  (stop)
  (start))


