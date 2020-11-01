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
   [jamtap.time :as jtime]
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

(defn loading-bar [msg]
  [:div
   [:div.has-text-centered.has-text-weight-semibold.pb-4 msg]
   [:progress.progress.is-large.is-primary]])

(defn offset-wrapper [inner]
  (if-let [offset @(rf/subscribe [:offset])]
    [inner offset]
    (do
      (rf/dispatch [:sync-clock])
      [loading-bar "Loading sum crunchny syncs..."])))

(defn about-page []
  [:img {:src "/img/warning_clojure.png"}])

(defn home-page []
  (when-let [docs @(rf/subscribe [:docs])]
    [:div {:dangerouslySetInnerHTML {:__html (md->html docs)}}]))

(defn list-tracks []
  (let [tracks-type @(rf/subscribe [:get-tracks-type])
        tracks @(rf/subscribe [:get-tracks])]
    [:div
     (for [{:keys [id creator name started_at]} tracks]
       ^{:key id}
       [:div.card.my-3>div.card-content>div.columns.is-vcentered
        [:div.column
         [:p.title.is-5 name]
         [:p.subtitle.is-6.is-italic creator]
         [:time (.toLocaleString started_at)]]
        [:div.column [:button.button.is-link.is-pulled-right
                      {:on-click #(rf/dispatch [:set-track-url {:id id}])}
                      "Join"]]])]))

(defn start-page []
  [offset-wrapper
   (fn [_]
     [:div.card.has-text-centered
      [:div.card-content "Click below when you're jam starts"]
      [:div.card-content>button.button.is-link
       {:on-click #(rf/dispatch [:create-track])}
       "Start!"]])])

(defn format-time [start offset elapsed-time]
  (let [elapsed @elapsed-time]
    [:p (jtime/format-running-time start (- offset elapsed))]))

(defn running-clock [started_at offset]
  (let [dummy (r/atom 0)]
    (js/setInterval (fn [] (swap! dummy inc)) 1000)
    (fn []
      (let [_ @dummy]
        [:p (jtime/format-running-time started_at offset)]))))

(defn comment-form []
  [:div.buttons.columns
   [:button.button.is-primary.is-large.column
    {:on-click #((rf/dispatch [:create-comment]))}
    "I like-a this!"]
   ;; TODO: conditionally show
   [:button.button.is-danger.is-large.column
    {:on-click #((rf/dispatch [:finish-track]))}
    "I'm done, kill this sesh!!"]])

(defn creator-form []
  (r/with-let [draft (r/atom nil)]
    [:div
     [:div.field
      [:label.label "Let's get started..."]
      [:label.label "What is your name?"]
      [:div.control>input.input
       {:type "text"
        :placeholder "Dr. Chilly"
        :on-change #(reset! draft (.. % -target -value))
        :value @draft}]]
     [:div.control>button.button.is-link
      {:on-click #((rf/dispatch [:set-creator @draft]))} "Let's go!"]]))

;; TODO: fix this comments next bit
(defn list-comments []
  (r/with-let [track @(rf/subscribe [:get-active-track])
               raw_comments (:comments track)
               to_reverse (not= nil (:finished_at track))
               comments (if to_reverse (reverse raw_comments) raw_comments)]
    [:ul
     (for [{:keys [id creator content commented_at running_time]} comments]
       ^{:key id}
       [:div.card.my-3>div.card-content
        (str creator " said \"" content "\" at: " (jtime/format-millis running_time))])]))

;; TODO: fix card spacing
(defn show-open-track [track]
  [offset-wrapper
   (fn [offset]
     [:div
      [:div.card.my-3
       [:div.card-header
        [:div.card-content
         [:p.title.is-4 (:name track)]
         [:p.subtitle.is-6.is-italic (str "by: " (:creator track))]]]
       [:div.card-content.has-text-centered.has-text-weight-semibold.pb-4
        [:p "Elapsed Time:"]
        [:div.is-size-1 [running-clock (:started_at track) offset]]]]
      [:div.card.my-3>div.card-content
       (if-let [creator @(rf/subscribe [:creator])]
         [comment-form]
         [creator-form])]
      [:div.card.my-3>div.card-content
       [list-comments]]])])

(defn show-closed-track [track]
  [:div
   [:div.card.my-3
    [:div.card-header
     [:div.card-content
      [:p.title.is-4 (:name track)]
      [:p.subtitle.is-6.is-italic (str "by: " (:creator track))]]]]
   [:div.card.my-3>div.card-content
    [list-comments]]])

(defn view-track []
  (if-let [loading @(rf/subscribe [:track-loading])]
    [loading-bar "Initializing your track..."]
    (let [track @(rf/subscribe [:get-active-track])]
      (if (= nil (:finished_at track))
        [show-open-track track]
        [show-closed-track track]))))

(defn new-track []
  (if @(rf/subscribe [:show-form])
    (r/with-let [draft (r/atom {})]
      [:div
       [:div.field
        [:label.label "What shall we call your jam sesh?"]
        [:div.control>input.input
         {:type "text"
          :placeholder "Joe's basement hang 4/20/19"
          :on-change #(swap! draft assoc :name (.. % -target -value))
          :value (:name @draft)}]]
       [:div.field
        [:label.label "What's YOUR name?"]
        [:div.control>input.input
         {:type "text"
          :placeholder "Alan"
          :on-change #(swap! draft assoc :creator (.. % -target -value))
          :value (:creator @draft)}]]
       [:div.control>button.button.is-link
        {:on-click #((do
                       ;; TODO: this seems bad
                       (rf/dispatch [:set-new-fields @draft])
                       (rf/dispatch [:set-show-form false])
                       (rf/dispatch [:sync-clock])))} "Continue"]])
    [start-page]))

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [:section.section>div.container>div.content
      [page]]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
   [["/" {:name        :home
          :view        home-page
          :controllers [{:start (fn [_]
                                  (rf/dispatch [:page/init-home]))}]}]
    ["/about" {:name :about
               :view about-page}]
    ["/tracks/new" {:name :new-track
                    :view new-track
                    :controllers [{:start (fn [_]
                                            (rf/dispatch [:set-show-form true]))}]}]
    ["/tracks/:id" {:name :view-track
                    :view view-track
                    :controllers [{:parameters {:path [:id]}
                                   :start (fn [{{:keys [id]} :path}]
                                            (rf/dispatch [:set-track-loading true])
                                            (rf/dispatch [:fetch-track id]))}]}]
    ["/tracks" {:name :list-tracks
                :view list-tracks
                :controllers [{:parameters {:query [:active]}
                               :start (fn [{{:keys [active]} :query}]
                                        (rf/dispatch [:fetch-tracks active]))}]}]]
   {:conflicts nil}))

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
