(ns user
  (:require [mount.core :as mount]
            [dv.figwheel :refer [start-fw stop-fw cljs]]
            ))

(defn start []
  (mount/start-without (eval (str "(do (require '[dv.core]) #'dv.core/http-server)"))
                       (eval (str "(do (require '[dv.core]) #'dv.core/repl-server)"))
                       ))

(defn stop []
  (mount/stop-except
    (eval (str "(do (require '[dv.core]) #'dv.core/http-server )"))
    (eval (str "(do (require '[dv.core]) #'dv.core/repl-server )"))
    ))

(defn restart []
  (stop)
  (start))


