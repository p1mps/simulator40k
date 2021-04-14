(ns simulator40k.core
  (:require
   [ajax.core :refer [GET POST]]
   [b1.charts :as c]
   [b1.svg :as s]
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

(def empty-state
  {:graph-data        :nil
   :page              :home
   :show-upload-files true
   :show-models      false
   :show-graph        false
   :files             {:Attacker nil
                       :Defender nil}})

(defonce session (r/atom empty-state))

(defn generate-graph-data []
  (take 100 (repeatedly rand)))

(defn graph []
  (-> (c/histogram (:graph-data @session) :x-axis [0 1] :n-bins 10)
      (s/as-svg :width 500 :height 200)))

(defn title []
  [:div.columns
   [:div.column
    [:div.title
     [:a {:href     "/#"
          :on-click (fn [_]
                      (reset! session empty-state))}
      [:h1 "Simulator 40k"]]]]
   ])

(defn file-roaster [role]
  [:div.field
   [:label.label (str (name role) " roaster:")]
   [:div.file
    [:label.file-label.full-width
     [:input.file-input {:id (name role)
                         :name      "resume", :type "file"
                         :on-change (fn [e]
                                      (swap! session assoc-in [:files role] (-> e .-target .-value))
                                      )}]
     [:span.file-cta.full-width
      [:span.file-icon [:i.fas.fa-upload]]
      [:span.file-label "\n        Choose a file…\n      "]]
     [:span.file-name (role (:files @session))]]]])

(defn dropdown-units [units]
  [:div.field
   [:div.select.is-dark.full-width
    [:select.full-width
     (for [u units]
       ^{:key u} [:option u])]]])


(defn attacker [{:keys [bs attacks ap strength save toughness]}]
  (list
   [:h1 "Attacker"]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "BS:"]]
    [:input.input
     {:type "text" :value bs}]]))

(defn attacker-weapons [{:keys [bs attacks ap strength save toughness]}]
  (list
   [:h1 "Attacker's Weapon"]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "AP:"]]
    [:input.input
     {:type "text" :value ap}]]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Attacks:"]]
    [:input.input
     {:type "text" :value attacks}]]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Strength:"]]
    [:input.input
     {:type "text" :value strength}]]
   ))


(defn defender [{:keys [bs attacks ap strength save toughness]}]
  (list
   [:h1 "Defender"]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Save:"]]
    [:input.input
     {:type "text" :value save}]]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Toughness:"]]
    [:input.input
     {:type "text" :value toughness}]]
   ))

(defn models [models]
  [:div.margin
   (attacker models)
   (attacker-weapons models)
   (defender models)])


(defn console-log [s]
  (.log js/console s))


(defn handler-upload [response]
  (console-log (str response)))


(defn upload-rosters []
  [:form
   (conj (file-roaster :Attacker)
               (file-roaster :Defender))
   [:button.button.is-dark
    {:name     "Upload"
     :on-click (fn [e]
                 (.preventDefault e)

                 (let [file-attacker (aget (.-files
                                            (.getElementById js/document "Attacker")) 0)
                       file-defender (aget (.-files
                                            (.getElementById js/document "Defender")) 0)
                       form-data (doto
                                     (js/FormData.)
                                   (.append "Attacker" file-attacker)
                                   (.append "Defender" file-defender))]

                   (POST "/api/parse" {:body  form-data
                                       :handler handler-upload}))
                 (swap! session assoc :show-upload-files false))} "Upload"]] )

(defn home-page []
  [:section.section>div.container>div.content
   (title)
   (when (:show-upload-files @session)
     (upload-rosters))
   (when-not (:show-upload-files @session)
     (list [:div.columns
            [:div.column (dropdown-units ["Attacker1" "Attacker2"])]]
           [:div.columns
            [:div.column (dropdown-units ["Weapon1" "Weapon2"])]]
           [:div.columns
            [:div.column (dropdown-units ["Defender1" "Defender2"])]]
           [:button.button.is-dark
            {:name     "select-units"
             :on-click (fn [e]
                         (.preventDefault e)
                         (swap! session assoc :show-models true)
                         )} "Select"])
     )
   (when (:show-models @session)
     (list [:div.columns [:div.column (models {:bs "3+"})]]
           [:button.button.is-dark
            {:name     "fight"
             :on-click (fn [e]
                         (.preventDefault e)
                         (swap! session assoc :graph-data (generate-graph-data))
                         (swap! session assoc :show-graph true))} "Fight"]))
   (when (:show-graph @session)
     (graph))])

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
