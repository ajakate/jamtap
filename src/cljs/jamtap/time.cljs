(ns jamtap.time
  (:require
   [ajax.core :refer [GET POST]]
   [luminus-transit.time :as time]
   [cognitect.transit :as transit]
   [cljs-time.coerce :as c]
   [re-frame.core :as rf]))

(declare do-sync)

(def sync-count 20)

(defn current-time []
  (.now js/Date))

(defn mean [items]
  (.round js/Math (/ (reduce + items) (count items))))

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

(defn format-leading-zero [seconds]
  (if (> 10 seconds)
    (str "0" seconds)
    seconds))

(defn append-hours [hours time-string]
  (if (< 0 hours)
    (str hours ":" time-string)
    time-string))

(defn format-daytime [hour minute]
  (let [full-minutes (format-leading-zero minute)
        m (if (< 11 hour) "pm" "am")]
    (str (-> hour (+ 11) (mod 12) (+ 1)) ":" full-minutes m)))

(defn format-millis [millis]
  (let [full_seconds (int (/ millis 1000))
        full_minutes (quot full_seconds 60)
        seconds (format-leading-zero (mod full_seconds 60))
        hours (quot full_minutes 60)
        minutes (format-leading-zero (mod full_minutes 60))]
    (append-hours hours (str minutes ":" seconds))))

(defn format-running-time [server-time offset]
  (let [start-millis (- (c/to-long server-time) offset)
        running (- (current-time) start-millis)]
    (format-millis running)))

(defn time-difference [begin end]
  (let [start-millis (c/to-long begin)
        end-millis (c/to-long end)]
    (format-millis (- end-millis start-millis))))
