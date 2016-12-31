(ns dv.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[dv started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[dv has shut down successfully]=-"))
   :middleware identity})
