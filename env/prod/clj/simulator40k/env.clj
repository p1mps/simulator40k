(ns simulator40k.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[simulator40k started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[simulator40k has shut down successfully]=-"))
   :middleware identity})
