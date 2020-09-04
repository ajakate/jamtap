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
      [:a.navbar-item {:href "/" :style {:font-weight :bold}} "jamtap"]
      [:span.navbar-burger.burger
       {:data-target :nav-menu
        :on-click #(swap! expanded? not)
        :class (when @expanded? :is-active)}
       [:span] [:span] [:span]]]
     [:div#nav-menu.navbar-menu
      {:class (when @expanded? :is-active)}
      [:div.navbar-start
       [nav-link "#/" "Home" :home]
       [nav-link "#/about" "About" :about]]]]))

(defn navbar1 []
  [:> ui/AppBar {:position "static"}
   [:> ui/Toolbar
    [:> ui/Box {:p 2}
     [:> ui/Typography {:variant "h6"} "Jamtap"]]
    [:> ui/Box {:m 2}
     [:> ui/ButtonGroup
      [:> ui/Button {:color "inherit" :href "/#/tracks/new"} "Create Track"]
      [:> ui/Button {:color "inherit" :href "/#/tracks/list"} "Join Track"]
      [:> ui/Button {:color "inherit" :href "/#/tracks/list"} "Find Old Track"]
      [:> ui/Button {:color "inherit" :href "/#/main"} "Test"]]]]])


(defn about-page []
  [:section.section>div.container>div.content
   [:img {:src "/img/warning_clojure.png"}]])

(defn home-page []
  [:section.section>div.container>div.content
   (when-let [docs @(rf/subscribe [:docs])]
     [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}])])

(defn list-tracks []
  [:div "click below to whatever your new track"])

(defn new-track []
  [:div "click below to create your new track"])

(defn main-page []
  [:> ui/Box {:m "auto" :p 4}
   [:> ui/ButtonGroup {:orientation "vertical"}
    [:> ui/Button {:color "primary" :href "/#/tracks/new"} "Create Track"]
    [:> ui/Button {:color "primary" :href "/#/tracks/list"} "Join Track"]
    [:> ui/Button {:color "primary" :href "/#/tracks/list"} "Find Old Track"]]])

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar1]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        #'home-page
          :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
    ["/about" {:name :about
               :view #'about-page}]
    ["/main" {:name :main
              :view #'main-page}]
    ["/tracks/new" {:name :new_track
              :view #'new-track}]
    ["/tracks/list" {:name :list-tracks
                    :view #'list-tracks}]]))

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
