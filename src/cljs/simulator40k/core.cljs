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

(defonce session (r/atom {:page
                          :home
                          :show-upload-files true
                          :show-results false
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
     [:label.file-label.full-width
      [:input.file-input {:name "resume", :type "file"
                          :on-change (fn [e]
                                       (swap! session assoc-in [:files role] (-> e .-target .-value))
                                       )}]
      [:span.file-cta.full-width
       [:span.file-icon [:i.fas.fa-upload]]
       [:span.file-label "\n        Choose a file…\n      "]]
      [:span.file-name (role (:files @session))]]]])

(defn dropdown-units [type-unit units]
  [:div.field
   [:div.select.is-dark.full-width
    [:select.full-width [:option type-unit]
     (for [u units]
       ^{:key u} [:option u])]]])

(defn render-unit [{:keys [bs attacks ap strength save toughness]}]
  [:div.margin
   [:h1 "Attacker"]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "BS:"]]
    [:input.input
     {:placeholder "BS:", :type "text" :value bs}]]
   [:h1 "Attacker's Weapon"]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "AP:"]]
    [:input.input
     {:placeholder "AP:", :type "text" :value ap}]]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Attacks:"]]
    [:input.input
     {:placeholder "Attacks", :type "text" :value attacks}]]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Strength:"]]
    [:input.input
     {:placeholder "Strength:", :type "text" :value strength}]]
   [:h1 "Defender"]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Save:"]]
    [:input.input
     {:placeholder "Save:", :type "text" :value save}]]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Toughness:"]]
    [:input.input
     {:placeholder "Toughness:", :type "text" :value toughness}]]]
  )


(defn upload-rosters []
  [:form (conj (file-roaster :Attacker)
               (file-roaster :Defender))
   [:button.button.is-dark
    {:name     "Upload"
     :on-click (fn [e]
                 (.preventDefault e)
                 (swap! session assoc :show-upload-files false))} "Upload"]] )

(defn home-page []
  [:section.section>div.container>div.content
   (title)
   (when (:show-upload-files @session)
     (upload-rosters))
   (when-not (:show-upload-files @session)
     (list [:div.columns
            [:div.column (dropdown-units "Attacker" ["Attacker1" "Attacker2"])]]
           [:div.columns
            [:div.column (dropdown-units "Attacker weapons" ["weapon1" "weapon2"])]]
           [:div.columns
            [:div.column (dropdown-units "Defender" ["Defender1" "Defender2"])]]
           [:button.button.is-dark
            {:name     "select-units"
             :on-click (fn [e]
                         (.preventDefault e)
                         (swap! session assoc :show-results true))} "Select"])
      )
   (when (:show-results @session)
     (list [:div.columns [:div.column (render-unit {:bs "3+"})]]
           [:button.button.is-dark
            {:name     "fight"
             :on-click (fn [e]
                         (.preventDefault e)
                         (swap! session assoc :show-results true))} "Fight"]))])

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
