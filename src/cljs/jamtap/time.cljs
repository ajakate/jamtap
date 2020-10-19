(ns jamtap.time
  (:require
   [ajax.core :refer [GET POST]]
   [luminus-transit.time :as time]
   [cognitect.transit :as transit]
   [re-frame.core :as rf]))

(declare do-sync)

(def sync-count 30)

(defn current-time []
  (.now js/Date))

(defn mean [items]
  (/ (reduce + items) (count items)))

(defn calc-offset [{:keys [start end server]} time]
  (/ (- (* 2 server) end start) 2))

(defn server-time-handler [start-time times-atom]
  (fn [response]
    (swap! times-atom conj
           {:start start-time
            :server (:server_time response)
            :end (current-time)})
    (do-sync times-atom)))

(defn do-sync [times-atom]
  (if (= sync-count (count @times-atom))
    (rf/dispatch
     [:set-offset
      (->> @times-atom
           (map calc-offset)
           mean)])
    (let [now (current-time)]
      (GET "/time" {:handler (server-time-handler now times-atom)}))))

(defn trigger-sync []
  (let [times (atom [])]
    (do-sync times)))
