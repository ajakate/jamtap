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
   [reitit.core :as reitit]
   [reitit.frontend.easy :as rfe]
   [clojure.string :as string]
   ["@material-ui/core" :as ui])
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
      [:a.navbar-item {:href "/#/main" :style {:font-weight :bold}} "jamtap"]
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

(defn new-track []
  (let [type @(rf/subscribe [:common/path-params])]
    [:section.section>div.container>div.content
     [:div.field
      [:label.label "What shall we call your jam sesh?"]
      [:div.control
       [:input.input {:type "text" :placeholder "Joe's basement hang 4/20/19"}]]]
     [:div.field
      [:label.label "What's YOUR name?"]
      [:div.control
       [:input.input {:type "text" :placeholder "Alan"}]]]
     [:div.control
      [:button.button.is-link "Continue"]]]))

(defn main-page []
  [:> ui/Box {:m "auto" :p 4}
   [:> ui/ButtonGroup {:orientation "vertical"}
    [:> ui/Button {:color "primary" :href "/#/tracks/new"} "Create Track"]
    [:> ui/Button {:color "primary" :href "/#/tracks?active=true"} "Join Track"]
    [:> ui/Button {:color "primary" :href "/#/tracks?active=false"} "Find Old Track"]]])

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
    ["/main" {:name :main
              :view main-page}]
    ["/tracks/new" {:name :new-track
                    :view new-track}]
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
