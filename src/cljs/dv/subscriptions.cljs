(ns dv.subscriptions
  (:require [re-frame.core :refer [reg-sub]]))

(reg-sub
  :page
  (fn [db _]
    (:page db)))

(reg-sub
  :docs
  (fn [db _]
    (:docs db)))

(reg-sub
  :pm
  (fn [db _]
    (:pm db)))

(reg-sub
  :pm-auth
  (fn [db _]
    (:auth (:pm db))))
