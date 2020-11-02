(ns jamtap.routes.home
  (:require
   [jamtap.layout :as layout]
   [jamtap.db.core :as db]
   [clojure.java.io :as io]
   [jamtap.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [clj-time.coerce :as c])
  (:import (java.util TimeZone)))

(TimeZone/setDefault (TimeZone/getTimeZone "UTC"))

(defn home-page [request]
  (layout/render request "home.html"))

(defn format-comments [id track_started]
  (let [comments (db/get-comments-for-track {:track_id id})]
    (map
     (fn [comment]
       (assoc comment
              :running_time
              (- (.getTime (:commented_at comment))
                 (.getTime track_started))))
     comments)))

(defn format-track [id]
  (let [track (db/get-track {:id id})]
    (assoc
     track
     :comments
     (format-comments id (:started_at track)))))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/time" {:get (fn [_]
                    {:body {:server_time (System/currentTimeMillis)}})}]
   ["/tracks"
    {:get (fn [{{:keys [active]} :params}]
            (if (read-string active)
              (response/ok (db/get-active-tracks))
              (response/ok (db/get-old-tracks))))
     :post (fn [{{:keys [creator name started_at]} :body-params}]
             (response/ok
              (db/create-track!
               {:creator creator
                :name name
                :started_at (c/to-sql-time (c/from-long started_at))})))}]
   ["/tracks/:id"
    {:get (fn [req]
            (let [id (-> req :path-params :id Integer/parseInt)]
              (response/ok
               (format-track id))))
     :post (fn [req]
             (let [id (Integer/parseInt (get-in req [:path-params :id]))
                   finished_at (get-in req [:body-params :finished_at])]
               (response/ok
                (db/finish-track!
                 {:id id
                  :finished_at (c/to-sql-time (c/from-long finished_at))}))))
     :delete (fn [req]
               (let [id (Integer/parseInt (get-in req [:path-params :id]))]
                 (response/ok (db/delete-track! {:id id}))))}]
   ["/comments"
    {:post (fn [{{:keys [creator content commented_at track_id]} :body-params}]
             (response/ok
              (db/create-comment!
               {:creator creator
                :content content
                :commented_at (c/to-sql-time (c/from-long commented_at))
                :track_id track_id})))}]])
