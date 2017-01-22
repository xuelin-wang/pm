(ns dv.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [dv.ajax :refer [load-interceptors!]]
            [dv.commonutils :as commonutils]
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

(defn maybe-nav-link [check? uri title page collapsed?]
  (if check?
    (let [selected-page (rf/subscribe [:page])]
      [:li.nav-item
       {:class (when (= page @selected-page) "active")}
       [:a.nav-link
        {:href uri
         :on-click #(reset! collapsed? true)} title]])
    [:span]))


(defn maybe-logout-link [login?]
  (let [maybe-logout-button
        (if login?
          [:a.nav-link.pointer
            {:on-click #(rf/dispatch [:auth-logout]) } "Log out"] [:span "       "])]
    [:li.nav-item maybe-logout-button]))

(defn navbar []
  (let
    [pm-auth @(rf/subscribe [:pm-auth])
     login? (:login? pm-auth)
     is-admin? [:is-admin? pm-auth]]
    (r/with-let [collapsed? (r/atom true)]
      [:nav.navbar.navbar-dark.bg-primary
       [:button.navbar-toggler.hidden-sm-up
        {:on-click #(swap! collapsed? not)} "☰"]
       [:div.collapse.navbar-toggleable-xs
        (when-not @collapsed? {:class "in"})
        [:ul.nav.navbar-nav
         [nav-link "#/pm" "Home" :pm collapsed?]
         [maybe-nav-link is-admin? "#/admin" "Admin" :admin collapsed?]
         [maybe-nav-link login? "#/settings" "Settings" :settings collapsed?]
         [maybe-logout-link login?]]]])))


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
    [:div.col-md-6 [text-input :update-value [[:pm :auth :auth-name]] "text" (:auth-name pm-auth) nil]]]
   [:div.row [:div.col-md-2 "Password: "]
    [:div.col-md-6
     [text-input :update-value [[:pm :auth :password]] "password" (:password pm-auth) nil]]]
   [:div.row
    [:div.col-md-2]
    [:div.col-md-6
     [:button {:on-click #(rf/dispatch [:auth-login]) :type "button" } "Login"]]]

   [:div.row [:div.col-md-8 [:span.error (get-in pm-auth [:login :error])]]]

   [:div.row
    [:div.col-md-8 "Don't have an account? please "
     [:a.pointer {:on-click #(rf/dispatch [:update-value [:pm :auth :registering?] true])} "register"]]]])

(defn pm-register [pm-auth]
  [:div.container
   [:div.row [:div.col-md-2 "Email address: "]
    [:div.col-md-6 [text-input :update-value [[:pm :auth :auth-name]] "text" (:auth-name pm-auth) nil]]]
   [:div.row [:div.col-md-2 "Password: "]
    [:div.col-md-6
     [text-input :update-value [[:pm :auth :password]] "password" (:password pm-auth) nil]]]
   [:div.row
    [:div.col-md-2]
    [:div.col-md-6
     [:button {:on-click #(rf/dispatch [:auth-register]) :type "button" } "Register"]]]

   [:div.row [:div.col-md-8 [:span.error (get-in pm-auth [:register :error])]]]

   [:div.row
    [:div.col-md-8 "Please click the link as instructed in the confirmation email after registration"]]
   [:div.row
    [:div.col-md-8 "Already have an account? please "
     [:a.pointer {:on-click #(rf/dispatch [:update-value [:pm :auth :registering?] false])} "login"]]]])

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
        new-row-name (get-in pm-data [:new-row :name])
        new-row-value (get-in pm-data [:new-row :value])
        add-row [:div.row
                 [:div.col-md-5 "Name: " [text-input :update-value [[:pm :data :new-row :name]] "text" new-row-name nil]]
                 [:div.col-md-5 "Value: " [text-input :update-value [[:pm :data :new-row :value]] "text" new-row-value nil]]
                 [:div.col-md-2
                  [:button {:on-click #(rf/dispatch [:pm-add-item nil]) :type "button" } "Add"]]]
        filter-str (:filter pm-data)
        editing-id (:editing-id pm-data)
        filter-row [:div.row>div.col-md-12 "Filter: "
                    [text-input :update-value [[:pm :data :filter]] "text" filter-str nil]]
        pm-data-list (vals (:list pm-data))
        filtered-list (if (clojure.string/blank? filter-str)
                         pm-data-list
                         (filter (fn [nv]
                                   (some
                                    true?
                                    (map
                                     #(nat-int? (.indexOf (.toLowerCase %) (.toLowerCase (.trim filter-str))))
                                     [(:name nv) (:value nv)])))
                                 pm-data-list))
        rows (map
              (fn [item]
                [pm-row item (= (:id item) editing-id)])
              filtered-list)]
    (into [] (concat [:div.container add-row filter-row] rows))))

(defn admin-page [admin]
  (let [script-type0 (:script-type admin)
        script-type (if script-type0 script-type0 (first commonutils/admin-script-types))]
    [:div.container
     [:div.row
      [:div.col-md-6
       [:select
        {:value script-type
         :on-change #(rf/dispatch [:update-value [:admin :script-type] (-> % .-target .-value)])}
        (map-indexed (fn [idx val] [:option {:key (str "_admin_s_t_" idx)} val]) commonutils/admin-script-types)]
       [:button {:on-click #(rf/dispatch [:admin-execute-script]) :type "button" } "Go"]
       [:br]
       [textarea-input :update-value [[:admin :script]] (:script admin) {:rows 10 :cols 50}]]
      [:div.col-md-6
       (if-let [loading? (:loading? admin)] "loading..." (str (:results admin)))]]]))

(defn maybe-admin-page []
  (let [pm-auth @(rf/subscribe [:pm-auth])]
    (when
      (:is-admin? pm-auth)
      (let
        [admin @(rf/subscribe [:admin])]
        [admin-page admin]))))

(defn pm-page []
  (let [pm-auth @(rf/subscribe [:pm-auth])]
    (cond
      (:login? pm-auth) [pm-data]
      (:registering? pm-auth) [pm-register pm-auth]
      :else [pm-login pm-auth])))

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
  (rf/dispatch [:update-value [:page] :pm]))

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
