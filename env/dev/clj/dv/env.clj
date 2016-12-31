(ns dv.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [dv.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[dv started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[dv has shut down successfully]=-"))
   :middleware wrap-dev})
