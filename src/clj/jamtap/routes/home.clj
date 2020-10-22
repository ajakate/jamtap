(ns jamtap.routes.home
  (:require
   [jamtap.layout :as layout]
   [jamtap.db.core :as db]
   [clojure.java.io :as io]
   [jamtap.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]
   [clj-time.coerce :as c]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]
   ["/time" {:get (fn [_]
                    {:body {:server_time (System/currentTimeMillis)}})}]
   ["/tracks"
    {:get (fn [_]
            (response/ok (db/get-active-tracks)))
     :post (fn [{{:keys [creator name started_at]} :body-params}]

             (println creator)
             (println name)
             (println  started_at)
             (response/ok (db/create-track! {:creator creator
                                             :name name
                                             :started_at (c/to-timestamp (c/from-long started_at))})))}]])

