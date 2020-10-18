(ns jamtap.time
  (:require
   [ajax.core :refer [GET POST]]
   [luminus-transit.time :as time]
   [cognitect.transit :as transit]
   [re-frame.core :as rf]))

(def sync-count 30)

(defn current-time []
  (.now js/Date))

(defn mean [items]
  (/ (reduce + items) (count items)))

(defn calc-offset [{:keys [start end server]} time]
  (/ (- (* 2 server) end start) 2))

(defn aggregate-times [times]
  (.log js/console "plz be ")
  (let [timess (map calc-offset (vals @times))]
    (.log js/console "YUSSSSSS" (str timess)))
  (mean times))

(defn server-time-handler [index times-atom]
  (fn [response]
    ;; (.log js/console "now we handle"  index @times-atom)
    (swap! times-atom assoc-in [index :server] (:server_time response))
    (swap! times-atom assoc-in [index :end] (current-time))


    (if
     (= sync-count (count (filter #(not= (:end %) nil) (vals @times-atom))))
      (rf/dispatch [:set-offset @times-atom])
      (aggregate-times times-atom))



    (.log js/console "FIRST"  (get-in @times-atom [0 :start]))
    (.log js/console "LOOOOOK"  (get-in @times-atom [0 :server]))
    (.log js/console "AGAIN"  (get-in @times-atom [0 :end]))
    ;; (rf/dispatch [:set-offset @times-atom])
    ))

(defn get-time-from-server [index times-atom]
  (GET "/time" {:handler (server-time-handler index times-atom)}))

(defn fig [index times-atom]
  (swap! times-atom assoc index {:start (current-time)})
  (.log js/console "we ok??" @times-atom)
  (get-time-from-server index times-atom))

(defn trigger-sync []
  (let [times (atom {})]
    (.log js/console "hiiiiiiiiiiii" "nooo :()")
    (dotimes [n sync-count] (fig n times))))