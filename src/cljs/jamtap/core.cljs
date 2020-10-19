(ns jamtap.core
  (:require
   [day8.re-frame.http-fx]
   [reagent.dom :as rdom]
   [reagent.core :as r]
   [re-frame.core :as rf]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [markdown.core :refer [md->html]]
   [jamtap.ajax :as ajax]
   [jamtap.events]
   [jamtap.time :as poo]
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [clojure.string :as string])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    [:nav.navbar.is-info>div.container
     [:div.navbar-brand
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "jamtap"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/tracks/new" "Create Track" :about]
       [nav-link "/#/tracks?active=true" "Join Track" :about]
       [nav-link "/#/tracks?active=false" "Find Old Track" :about]]]]))

(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn list-tracks []
  (let [active (:active @(rf/subscribe [:common/query-params]))]
    [:div "getting tracks where active is" active]))

(defn start-page []
  [:section.section>div.container>div.content
   (if-let [offset @(rf/subscribe [:offset])]
     [:div "omg its hurrr: " offset]
     [:div  [:div.has-text-centered.has-text-weight-semibold.pb-4 "Loading sum crunchny syncs..."]
      [:progress.progress.is-large.is-primary "looooooading"]])])

(defn new-track []
  (if @(rf/subscribe [:show-form])
    (r/with-let [draft (r/atom {})]
      [:section.section>div.container>div.content
       [:div.field
        [:label.label "What shall we call your jam sesh?"]
        [:div.control
         [:input.input {:type "text"
                        :placeholder "Joe's basement hang 4/20/19"
                        :on-change #(swap! draft assoc :name (.. % -target -value))
                        :value (:name @draft)}]]]
       [:div.field
        [:label.label "What's YOUR name?"]
        [:div.control
         [:input.input {:type "text"
                        :placeholder "Alan"
                        :on-change #(swap! draft assoc :creator (.. % -target -value))
                        :value (:creator @draft)}]]]
       [:div.control
        [:button.button.is-link
         {:on-click #((rf/dispatch [:set-new-fields @draft])
                      (rf/dispatch [:set-show-form false])
                      (rf/dispatch [:sync-clock]))} "Continue"]]])
    [start-page]))

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        home-page
          :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
    ["/about" {:name :about
               :view about-page}]
    ["/tracks/new" {:name :new-track
                    :view new-track
                    :controllers [{:start (fn [_] (rf/dispatch [:set-show-form true]))}]}]
    ["/tracks" {:name :list-tracks
                :view list-tracks}]]))

(defn start-router! []
  (rfe/start!
   router
   navigate!
   {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
