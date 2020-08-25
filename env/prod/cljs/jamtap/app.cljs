(ns jamtap.app
  (:require [jamtap.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
