(ns jamtap.events
  (:require
   [re-frame.core :as rf]
   [ajax.core :as ajax]
   [reitit.frontend.easy :as rfe]
   [reitit.frontend.controllers :as rfc]
   [jamtap.time :as jtime]))

;;dispatchers

(rf/reg-event-db
 :common/navigate
 (fn [db [_ match]]
   (let [old-match (:common/route db)
         new-match (assoc match :controllers
                          (rfc/apply-controllers (:controllers old-match) match))]
     (assoc db :common/route new-match))))

(rf/reg-fx
 :common/navigate-fx!
 (fn [[k & [params query]]]
   (rfe/push-state k params query)))

(rf/reg-event-fx
 :common/navigate!
 (fn [_ [_ url-key params query]]
   {:common/navigate-fx! [url-key params query]}))

(rf/reg-event-db
 :set-docs
 (fn [db [_ docs]]
   (assoc db :docs docs)))

(rf/reg-event-fx
 :set-track-url
 (fn [_ [_ track]]
   ;; TODO: SQL QUERY RETURN JUST THE ID??
   (rfe/push-state :view-track {:id (:id track)})))

(rf/reg-event-fx
 :set-track-url-comment
 (fn [_ [_ comment]]
   {:dispatch [:fetch-track (:track_id comment)]}))

(rf/reg-event-db
 :set-active-track
 (fn [db [_ track]]
   (assoc db :active-track track :track-loading false)))

(rf/reg-event-db
 :set-track-loading
 (fn [db [_ loading]]
   (assoc db :track-loading loading)))

(rf/reg-event-db
 :set-response
 (fn [db [_ res]]
   (assoc db :res res)))

(rf/reg-event-db
 :set-creator
 (fn [db [_ creator]]
   (assoc db :creator creator)))

(rf/reg-event-db
 :set-new-fields
 (fn [db [_ fields]]
   (assoc db
          :form/new fields
          :creator (:creator fields))))

(rf/reg-event-db
 :set-offset
 (fn [db [_ fields]]
   (assoc db :offset fields)))

(rf/reg-event-db
 :set-show-form
 (fn [db [_ show-form]]
   (assoc db :show-form show-form)))

(rf/reg-event-fx
 :fetch-track
 (fn [_ [_ id]]
   {:http-xhrio {:method          :get
                 :uri             (str "/tracks/" id)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-active-track]}}))

(rf/reg-event-fx
 :fetch-docs
 (fn [_ _]
   {:http-xhrio {:method          :get
                 :uri             "/docs"
                 :response-format (ajax/raw-response-format)
                 :on-success       [:set-docs]}}))

(rf/reg-event-fx
 :create-track
 (fn [{:keys [db]} [_]]
   {:http-xhrio {:method          :post
                 :uri             "/tracks"
                 :params {:creator (get-in db [:form/new :creator])
                          :name (get-in db [:form/new :name])
                          :started_at (+ (:offset db) (jtime/current-time))}
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-track-url]
                 :on-failure [:set-track-url]}}))

(rf/reg-event-fx
 :create-comment
 (fn [{:keys [db]} [_]]
   {:http-xhrio {:method          :post
                 :uri             "/comments"
                 :params {:creator (:creator db)
                          :content "Very Niiice!"
                          :commented_at (+ (:offset db) (jtime/current-time))
                          :track_id (get-in db [:active-track :id])}
                 :format          (ajax/json-request-format)
                 :response-format  (ajax/json-response-format {:keywords? true})
                 :on-success       [:set-track-url-comment]
                 :on-failure [:set-track-url]}}))

(rf/reg-event-fx
 :sync-clock
 (fn [_ _]
   (jtime/trigger-sync)))

(rf/reg-event-db
 :common/set-error
 (fn [db [_ error]]
   (assoc db :common/error error)))

(rf/reg-event-fx
 :page/init-home
 (fn [_ _]
   {:dispatch [:fetch-docs]}))

;;subscriptions

;; TODO: set if null
(rf/reg-sub
 :offset
 (fn [db _]
   (-> db :offset)))

(rf/reg-sub
 :creator
 (fn [db _]
   (-> db :creator)))

(rf/reg-sub
 :track-loading
 (fn [db _]
   (-> db :track-loading)))

(rf/reg-sub
 :get-active-track
 (fn [db _]
   (-> db :active-track)))

(rf/reg-sub
 :show-form
 (fn [db _]
   (-> db :show-form)))

(rf/reg-sub
 :common/route
 (fn [db _]
   (-> db :common/route)))

(rf/reg-sub
 :common/page-id
 :<- [:common/route]
 (fn [route _]
   (-> route :data :name)))

(rf/reg-sub
 :common/page
 :<- [:common/route]
 (fn [route _]
   (-> route :data :view)))

(rf/reg-sub
 :common/query-params
 :<- [:common/route]
 (fn [route _]
   (-> route :query-params)))

(rf/reg-sub
 :common/path-params
 :<- [:common/route]
 (fn [route _]
   (-> route :path-params)))

(rf/reg-sub
 :docs
 (fn [db _]
   (:docs db)))

(rf/reg-sub
 :common/error
 (fn [db _]
   (:common/error db)))
