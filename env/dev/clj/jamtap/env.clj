(ns jamtap.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [jamtap.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[jamtap started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[jamtap has shut down successfully]=-"))
   :middleware wrap-dev})
