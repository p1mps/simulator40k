(ns simulator40k.core
  (:require
   [cljs.pprint :refer [pprint]]
    [reagent.core :as r]
    [reagent.dom :as rdom]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [simulator40k.ajax :as ajax]
    [ajax.core :refer [GET POST]]
    [reitit.core :as reitit]
    [clojure.string :as string])
  (:import goog.History))

(defonce session (r/atom {:page :home
                          :files {:Attacker nil
                                  :Defender nil}}))

(defn title []
  [:div.columns
   [:div.column
    [:div.title [:h1 "Simulator 40k"]]]
   ])

(defn file-roaster [role]
  [:div.field
   [:label.label (str (name role) " roaster:")]
   [:div.file
     [:label.file-label
      [:input.file-input {:name "resume", :type "file"
                          :on-change (fn [e]
                                       (swap! session assoc-in [:files role] (-> e .-target .-value)))}]
      [:span.file-cta
       [:span.file-icon [:i.fas.fa-upload]]
       [:span.file-label "\n        Choose a file…\n      "]]
      [:span.file-name (role (:files @session))]]]])


(defn upload-rosters []
  [:form (conj (file-roaster :Attacker)
               (file-roaster :Defender))
   [:button.button.is-dark
    {:name "Upload"
     :on-click (fn [e]
                  (.preventDefault e)
                  (.log js/console "Hello, world!"))} "Upload"]] )

(defn home-page []
  [:section.section>div.container>div.content
   (title)
   (upload-rosters)])



(def pages
  {:home #'home-page})

(defn page []
  [(pages (:page @session))])

;; -------------------------
;; Routes

(def router
  (reitit/router
    [["/" :home]]))

(defn match-route [uri]
  (->> (or (not-empty (string/replace uri #"^.*#" "")) "/")
       (reitit/match-by-path router)
       :data
       :name))
;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
      HistoryEventType/NAVIGATE
      (fn [^js/Event.token event]
        (swap! session assoc :page (match-route (.-token event)))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rdom/render [#'title] (.getElementById js/document "title"))
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (enable-console-print!)
  (ajax/load-interceptors!)
  (hook-browser-navigation!)
  (mount-components))
