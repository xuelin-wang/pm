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
  :admin
  (fn [db _]
    (get-in db [:admin])))

(reg-sub
  :pm-data
  (fn [db _]
    (get-in db [:pm :data])))

(reg-sub
  :pm-auth
  (fn [db _]
    (get-in db [:pm :auth])))

(reg-sub
  :init
  (fn [db _]
    (:init db)))
