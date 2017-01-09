(ns dv.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [dv.ajax :refer [load-interceptors!]]
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
       [nav-link "#/admin" "Admin" :admin collapsed?]
       [nav-link "#/settings" "Settings" :settings collapsed?]]]]))

(defn settings-page []
  [:div.container
   [:div.row
    [:div.col-md-12
     "this is the settings of pm... work in progress"]]])

(defn- text-input [event-id event-params input-type init-val props]
  (let [val (r/atom init-val)
        stop #(reset! val init-val)
        save #(rf/dispatch (into [] (concat [event-id] event-params [@val])))]
    [:input
      (merge
       {
        :type input-type
        :default-value init-val
        :on-blur save
        :on-change #(reset! val (-> % .-target .-value))
        :on-key-down #(case (.-which %)
                        13 (save)
                        27 (stop)
                        nil)}
       props)]))


(defn- textarea-input [event-id event-params init-val props]
  (let [val (r/atom init-val)
        save #(rf/dispatch (into [] (concat [event-id] event-params [@val])))]
    [:textarea
       (merge
        {
         :default-value init-val
         :on-blur save
         :on-change #(reset! val (-> % .-target .-value))
         :on-key-up #(reset! val (-> % .-target .-value))}
        props)]))

(defn pm-login [pm-auth]
  [:div.container
   [:div.row [:div.col-md-2 "Email address: "]
    [:div.col-md-4 [text-input :update-value [[:pm :auth :auth-name]] "text" (:auth-name pm-auth) nil]]]
   [:div.row [:div.col-md-2 "Password: "]
    [:div.col-md-4
     [text-input :update-value [[:pm :auth :password]] "password" (:password pm-auth) nil]]]
   [:div.row
    [:div.col-md-2]
    [:div.col-md-2
     [:button {:on-click #(rf/dispatch [:auth-login]) :type "button" } "Login"]]]

   [:div.row
    [:div.col-md-4 "Don't have an account? please "
     [:a.pointer {:on-click #(rf/dispatch [:update-value [:pm :auth :registering] true])} "register"]]]])

(defn pm-register [pm-auth]
  [:div.container
   [:div.row [:div.col-md-2 "Email address: "]
    [:div.col-md-4 [text-input :update-value [[:pm :auth :auth-name]] "text" (:auth-name pm-auth) nil]]]
   [:div.row [:div.col-md-2 "Password: "]
    [:div.col-md-4
     [text-input :update-value [[:pm :auth :password]] "password" (:password pm-auth) nil]]]
   [:div.row
    [:div.col-md-2]
    [:div.col-md-2
     [:button {:on-click #(rf/dispatch [:auth-register]) :type "button" } "Register"]]]

   [:div.row
    [:div.col-md-4 "Please click the link as instructed in the confirmation email after registration"]]
   [:div.row
    [:div.col-md-4 "Already have an account? please "
     [:a.pointer {:on-click #(rf/dispatch [:update-value [:pm :auth :registering] false])} "login"]]]])

(defn pm-editable-row [item]
  [:div.row
   [:div.col-md-4
    [text-input :update-value [[:pm :data :lists (:id item) :name]] "text" (:name item) nil]]
   [:div.col-md-8
    [text-input :update-value [[:pm :data :lists (:id item) :value]] "text" (:value item) nil]]])

(defn pm-readonly-row [item]
  [:div.row
   [:div.col-md-4
    [:span {:on-click #(rf/dispatch [:update-value [:pm :data :editing-id] (:id item)])} (:name item)]]
   [:div.col-md-8
    [:span {:on-click #(rf/dispatch [:update-value [:pm :data :editing-id] (:id item)])} (:value item)]]])

(defn pm-row [item editing]
  (if editing
    [pm-editable-row item]
    [pm-readonly-row item]))

(defn pm-data []
  (let [pm-data @(rf/subscribe [:pm-data])
        filter-str (:filter pm-data)
        editing-id (:editing-id pm-data)
        filter-row [:div.row>div.col-md-12 "Filter: " [text-input :update-value [[:pm :data :filter]] "text" filter-str nil]]
        pm-data-lists (vals (:lists pm-data))
        filtered-lists (if (clojure.string/blank? filter-str)
                         pm-data-lists
                         (filter (fn [nv]
                                   (some
                                    true?
                                    (map
                                     #(nat-int? (.indexOf (.toLowerCase %) (.toLowerCase (.trim filter-str))))
                                     [(:name nv) (:value nv)])))
                                 pm-data-lists))
        rows (map
              (fn [item]
                [pm-row item (= (:id item) editing-id)])
              filtered-lists)]
    (into [] (concat [:div.container filter-row] rows))))

(defn admin-page [admin]
  [:div.container
   [:div.row
    [:div.col-md-12
     [:button {:on-click #(rf/dispatch [:admin-execute-script]) :type "button" } "Go"]
     [:span (if-let [loading? (:loading? admin)] "loading..." (str (or (:error admin) (:response admin))))]
     [:br]
     [textarea-input :update-value [[:admin :script]] (:script admin) {:rows 10 :cols 50}]]]])

(defn maybe-admin-page []
  (let [pm-auth @(rf/subscribe [:pm-auth])]
    (when
      (:admin pm-auth)
      (let
        [admin @(rf/subscribe [:admin])]
        [admin-page admin]))))

(defn pm-page []
  (let [pm-auth @(rf/subscribe [:pm-auth])]
    (cond
      (:login pm-auth) [pm-data]
      (:registering pm-auth) [pm-register]
      :else [pm-login])))

(defn home-page []
  [:div.container
   (when-let [docs @(rf/subscribe [:docs])]
     [:div.row>div.col-sm-12
      [:div {:dangerouslySetInnerHTML
             {:__html (md->html docs)}}]])])

(def pages
  {:home #'home-page
   :pm #'pm-page
   :admin #'maybe-admin-page
   :settings #'settings-page})

(defn page []
  [:div
   [navbar]
   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:update-value [:page] :home]))

(secretary/defroute "/pm" []
  (rf/dispatch [:update-value [:page] :pm]))

(secretary/defroute "/admin" []
  (rf/dispatch [:update-value [:page] :admin]))

(secretary/defroute "/settings" []
  (rf/dispatch [:update-value [:page] :settings]))

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
  (GET "/docs" {:handler #(rf/dispatch [:update-value [:docs] %])}))

(defn mount-components []
  (rf/clear-subscription-cache!)
  (r/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch-sync [:initialize-db])
  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
