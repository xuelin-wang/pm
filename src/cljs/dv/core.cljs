(ns dv.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [dv.ajax :refer [load-interceptors!]]
            [dv.events]
            [dv.handlers]
            [dv.subscriptions])
  (:import goog.History))

(defn nav-link [uri title page collapsed?]
  (let [selected-page (rf/subscribe [:page])]
    [:li.nav-item
     {:class (when (= page @selected-page) "active")}
     [:a.nav-link
      {:href uri
       :on-click #(reset! collapsed? true)} title]]))

(defn navbar []
  (r/with-let [collapsed? (r/atom true)]
    [:nav.navbar.navbar-dark.bg-primary
     [:button.navbar-toggler.hidden-sm-up
      {:on-click #(swap! collapsed? not)} "☰"]
     [:div.collapse.navbar-toggleable-xs
      (when-not @collapsed? {:class "in"})
      [:a.navbar-brand {:href "#/"} "dv"]
      [:ul.nav.navbar-nav
       [nav-link "#/" "Home" :home collapsed?]
       [nav-link "#/pm" "My stuff" :pm collapsed?]
       [nav-link "#/about" "About" :about collapsed?]]]]))

(defn about-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "this is the story of pm... work in progress"]]])

(defn- text-input [event-id input-type init-val]
  (let [val (r/atom init-val)
        stop #(reset! val init-val)
        save #(rf/dispatch [event-id @val])]
    [:input {:type input-type
             :auto-focus true
             :default-value init-val
             :on-blur save
             :on-change #(reset! val (-> % .-target .-value))
             :on-key-down #(case (.-which %)
                             13 (save)
                             27 (stop)
                             nil)}]))

(defn pm-login [pm-auth]
  [:div.container
   [:div.row [:div.col-md-2 "Email address: "]
    [:div.col-md-4 [text-input :auth-set-name "text" (:auth-name pm-auth)]]]
   [:div.row [:div.col-md-2 "Password: "]
    [:div.col-md-4
     [text-input :auth-set-password "password" (:password pm-auth)]]]
   [:div.row
    [:div.col-md-2]
    [:div.col-md-2
     [:button {:on-click #(rf/dispatch [:auth-login])
               :type "button"} "Login"]]]])

(defn pm-data []
  [:div.container
   [:div.row
    [:div.col-md-12
     "this is my stuff... work in progress"]]])

(defn pm-page []
  (let [pm-auth @(rf/subscribe [:pm-auth])]
    (if (:login pm-auth) [pm-data] [pm-login pm-auth])))

(defn home-page []
  [:div.container
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(def pages
  {:home #'home-page
   :pm #'pm-page
   :about #'about-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:set-active-page :home]))

(secretary/defroute "/pm" []
  (rf/dispatch [:set-active-page :pm]))

(secretary/defroute "/about" []
  (rf/dispatch [:set-active-page :about]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [event]
        (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn fetch-docs! []
  (GET "/docs" {:handler #(rf/dispatch [:set-docs %])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
