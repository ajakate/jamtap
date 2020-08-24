(ns jamtap.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[jamtap started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[jamtap has shut down successfully]=-"))
   :middleware identity})
