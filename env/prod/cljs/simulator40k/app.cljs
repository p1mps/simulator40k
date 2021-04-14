(ns simulator40k.app
  (:require [simulator40k.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
