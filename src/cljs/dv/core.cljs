(ns dv.core
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [secretary.core :as secretary]
            [goog.events :as events]
            [goog.history.EventType :as HistoryEventType]
            [markdown.core :refer [md->html]]
            [ajax.core :refer [GET POST]]
            [clojure.string]
            [dv.utils :refer [new-window to-csv]]
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
            {
             :on-click #(rf/dispatch [:auth-logout]) } "Log out"] [:span "       "])]
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
         [maybe-nav-link login? "#/settings" "Settings" :settings collapsed?]
         [maybe-logout-link login?]]]])))



(defn- text-input [event-id event-params input-type init-val save-on-change? props]
  (let [val (r/atom init-val)
        stop #(reset! val init-val)
        save #(rf/dispatch (into [] (concat [event-id] event-params [(if (nil? %) @val %)])))
        save-on-change #(rf/dispatch (into [] (concat [event-id] event-params [@val])))]
    [:input
      (merge
       {
        :type input-type
        :default-value init-val
        :on-blur (partial save nil)
        :on-change #(let [new-val (-> % .-target .-value)]
                      (if save-on-change? (save new-val) (reset! val new-val)))
        :on-key-down #(case (.-which %)
                        13 (save nil)
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
       [:button.btn.btn-default.btn-sm {:on-click #(rf/dispatch [:admin-execute-script]) :type "button" } "Go"]
       [:br]
       [textarea-input :update-value [[:admin :script]] (:script admin) {:rows 10 :cols 50}]]
      [:div.col-md-6
       (if-let [loading? (:loading? admin)] "loading..." (str (:results admin)))]]]))

(defn settings-page []
  (let [pm-auth @(rf/subscribe [:pm-auth])]
    [:div.container
     (when
       (:is-admin? pm-auth)
       (let
         [admin @(rf/subscribe [:admin])]
         [admin-page admin]))
     [:div.row [:div.col-md-6 [:span "There are no settings"]]]]))

(defn pm-login [pm-auth]
  [:div.container
   [:div.row [:div.col-md-2 "Email address: "]
    [:div.col-md-6 [text-input :update-value [[:pm :auth :auth-name]] "text" (:auth-name pm-auth) false nil]]]
   [:div.row [:div.col-md-2 "Password: "]
    [:div.col-md-6
     [text-input :update-value [[:pm :auth :password]] "password" (:password pm-auth) false nil]]]
   [:div.row
    [:div.col-md-2]
    [:div.col-md-6
     [:button.btn.btn-default.btn-sm {:on-click #(rf/dispatch [:auth-login]) :type "button" } "Login"]]]

   [:div.row [:div.col-md-8 [:span.error (get-in pm-auth [:login :error])]]]

   [:div.row
    [:div.col-md-8 "Don't have an account? please "
     [:button.btn.btn-default.btn-sm
      {:type "button" :on-click #(rf/dispatch [:update-value [:pm :auth :registering?] true])} "register"]]]])

(defn pm-register [pm-auth]
  [:div.container
   [:div.row [:div.col-md-2 "Email address: "]
    [:div.col-md-6 [text-input :update-value [[:pm :auth :auth-name]] "text" (:auth-name pm-auth) nil]]]
   [:div.row [:div.col-md-2 "Password: "]
    [:div.col-md-6
     [text-input :update-value [[:pm :auth :password]] "password" (:password pm-auth) false
      {:on-paste #(do)}]]]
   [:div.row [:div.col-md-2 "Confirm Password: "]
    [:div.col-md-6
     [text-input :update-value [[:pm :auth :confirm-password]] "password" (:confirm-password pm-auth) false
      {:on-paste #(do)}]]]
   [:div.row
    [:div.col-md-2]
    [:div.col-md-6
     [:button.btn.btn-default.btn-sm
      {:on-click
       #(rf/dispatch [:auth-register])
       :type "button" } "Register"]]]

   [:div.row [:div.col-md-8 [:span.error (get-in pm-auth [:register :error])]]]
   [:div.row [:div.col-md-8 [:span.message (get-in pm-auth [:register :msg])]]]

   [:div.row
    [:div.col-md-8 "Already have an account? please "
     [:button.btn.btn-default.btn-sm
      {:type "button" :on-click #(rf/dispatch [:update-value [:pm :auth :registering?] false])}
      "login"]]]])

(defn pm-editable-row [item]
  [:div.row
   [:div.col-md-4
    [text-input :pm-update-row [[nil (:id item) :name]] "text" (:name item) false nil]]
   [:div.col-md-8
    [text-input :pm-update-row [[nil (:id item) :value]] "text" (:value item) false nil]]])

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
        pm-data-list (vals (:list pm-data))
        add-row [:div.row
                 [:div.col-md-1 "Name: "
                  ]
                 [:div.col-md-5 [text-input :update-value [[:pm :data :new-row :name]] "text" new-row-name false {:size 30}]
                  [:br]
                  [:button.btn.btn-default.btn-sm {:on-click #(rf/dispatch [:pm-add-item nil]) :type "button" } "Add New Item"]
                  [:br]
                  [:button.btn.btn-default.btn-sm
                   {:on-click #(new-window "" (to-csv pm-data-list [:id :name :value] name)) :type "button" } "Export"]

                  ]
                 [:div.col-md-1 "Value: "]
                 [:div.col-md-5 [textarea-input :update-value [[:pm :data :new-row :value]] new-row-value {:rows 3 :cols 30}]]
                 ]
        filter-str (:filter pm-data)
        editing-id (:editing-id pm-data)
        filter-row [:div.row>div.col-md-12 "Filter: "
                    [text-input :update-value [[:pm :data :filter]] "text" filter-str true nil]]
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
              (sort-by :name filtered-list))]
    (into [] (concat [:div.container add-row [:hr] filter-row] rows))))

(defn pm-page []
  (let [pm @(rf/subscribe [:pm])
        pm-auth (:auth pm)]
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
   :settings #'settings-page})

(defn page []
  [:div
   [navbar]
   [:div.container
    (let [init @(rf/subscribe [:init])]
      (cond
        (= (:msg-type init) :error)
        [:div.row [:div.col-md-8 [:span.error (:msg init)]]]

        (= (:msg-type init) :ok)
        [:div.row [:div.col-md-8 [:span.message (:msg init)]]]

        :else [:div]))]

   [(pages @(rf/subscribe [:page]))]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (rf/dispatch [:update-value [:page] :pm]))

(secretary/defroute "/pm" []
  (rf/dispatch [:update-values [[:page] :pm]]))

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
  (let [init-elem (.getElementById js/document "__init__")
        init-str (if (nil? init-elem) "{}" (.-textContent init-elem))
        init-json (if (clojure.string/blank? init-str) nil (js->clj (.parse js/JSON init-str)))]
    (rf/dispatch-sync [:initialize-db init-json]))

  (load-interceptors!)
  (fetch-docs!)
  (hook-browser-navigation!)
  (mount-components))
