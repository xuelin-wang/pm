(ns dv.utils
  (:import
           (java.util UUID))
  (:require
            [clojure.spec :as s]))

(defn new-uuid []
  (let [uuid-obj (UUID/randomUUID)
        uuid-str (.toString uuid-obj)
        str0 (.substring uuid-str 0 8)
        str1 (.substring uuid-str 9 13)
        str2 (.substring uuid-str 14 18)
        str3 (.substring uuid-str 19 23)
        str4 (.substring uuid-str 24)]
    (str str0 str1 str2 str3 str4)))
