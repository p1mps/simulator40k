(ns simulator40k.routes.home
  (:require
   [simulator40k.layout :as layout]
   [simulator40k.parse :as parse]
   [simulator40k.fight :as fight]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [simulator40k.middleware :as middleware]
   [ring.util.response]
   [ring.util.http-response :as response]))

(defn run-fight [request]
  (let [stats (fight/stats (:params request))]

    {:status  200
     :headers {}
     :body    (json/generate-string {:fight
                                     stats})}))

(defn parse-rosters [request]
  {:status 200
   :headers {}
   :body {:attacker-roster
          (parse/parse (:tempfile (:Attacker (:params request))))
          :defender-roster
          (parse/parse (:tempfile (:Defender (:params request))))}})

(defn home-page [request]
  (layout/render request "home.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/api/parse" {:post parse-rosters}]
   ["/api/fight" {:post run-fight}]])
