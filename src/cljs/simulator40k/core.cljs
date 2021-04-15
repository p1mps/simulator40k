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
  {:attacker-roster         nil
   :defender-roster         nil
   :defender-unit-models    nil
   :attacker-unit-models    nil
   :selected-attacker-model nil
   :selected-defender-model nil
   :graph-data              nil
   :attacker-weapons        nil
   :page                    :home
   :show-upload-files       true
   :show-models             false
   :show-graph              false
   :files                   {:Attacker nil
                             :Defender nil}})

(defonce session (r/atom empty-state))

(defn console-log [s]
  (cljs.pprint/pprint s))


(defn add-react-key [coll]
  (loop [coll   coll
         result []
         i      0]
    (if (seq coll)
      (recur
       (rest coll)
       (conj result (with-meta (first coll) {:key i}))
       (inc i))
      result)))


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
     [:input.file-input {:id        (name role)
                         :name      "resume", :type "file"
                         :on-change (fn [e]
                                      (swap! session assoc-in [:files role] (-> e .-target .-value))
                                      )}]
     [:span.file-cta.full-width
      [:span.file-icon [:i.fas.fa-upload]]
      [:span.file-label "\n        Choose a file…\n      "]]
     [:span.file-name (role (:files @session))]]]])



(defn dropdown-weapons [weapons]
  [:div.field
   [:div.select.is-dark.full-width
    [:select.full-width
     (for [w weapons]
        [:option
         {:id (:id w)} (:name w)])]]])


(defn attacker []

  (list
   [:h1 "Attacker"]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "BS:"]]
    [:input.input
     {:type "text"
      :value (-> @session :attacker-model :chars :bs)
      }]]))

(defn attacker-weapons []
  (list
   [:h1 "Attacker's Weapon"]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "AP:"]]
    [:input.input
     {:type "text" :value 1}]]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Attacks:"]]
    [:input.input
     {:type "text" :value 1}]]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Strength:"]]
    [:input.input
     {:type "text" :value 1}]]
   ))


(defn defender []
  (list
   [:h1 "Defender"]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Save:"]]
    [:input.input
     {:type "text" :value (-> @session :defender-model :chars :save)}]]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Toughness:"]]
    [:input.input
     {:type "text" :value (-> @session :defender-model :chars :t)}]]
   ))

(defn models []
  [:div.margin
   (attacker)
   (attacker-weapons)
   (defender)])


(defn handler-upload [response]
  (swap! session assoc :attacker-roster (:attacker-roster response))
  (swap! session assoc :defender-roster (:defender-roster response)))

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
                       form-data     (doto
                                         (js/FormData.)
                                       (.append "Attacker" file-attacker)
                                       (.append "Defender" file-defender))]
                   (POST "/api/parse" {:body    form-data
                                       :handler handler-upload}))
                 (swap! session assoc :show-upload-files false))} "Upload"]] )

(defn set-model! [unit-id model-id k from-models]
  (swap! session assoc k
         (:model (first (filter #(and (= (:id (:unit %)) unit-id) (= (:id (:model %)) model-id)) (from-models @session))))))

(defn dropdown-units [models k belong-to]
  [:div.field
   [:div.select.is-dark.full-width
    [:select.full-width {:id        "select"
                         :on-change #(let [e        (js/document.getElementById "select")
                                           unit-id  (.-value (.-idunit (.-attributes (aget (.-options e) (.-selectedIndex e)))))
                                           model-id (.-value (.-idmodel (.-attributes (aget (.-options e) (.-selectedIndex e)))))]
                                       (set-model! unit-id model-id k belong-to))}
     (for [m models]
       ^{:key (str m (:id (:unit m)))} [:optgroup  {
                                                    :label (:name (:unit m))}
                                        ^{:key (str m)} [:option
                                                         {:idunit  (:id (:unit m))
                                                          :idmodel (:id (:model m))} (:name (:model m))]])]]])

(defn init-models! [roster type-key]
  (let [unit-models (for [u (:units roster)
                          m (:models u)]
                      {:unit u :model m})]
    (swap! session assoc type-key unit-models))
  (set-model! "0" "0" :attacker-model :attacker-unit-models)
  (set-model! "0" "0" :defender-model :defender-unit-models))

(defn dropdown-attacker-units []
  [:div.columns
   [:div.column (dropdown-units
                 (:attacker-unit-models @session)
                 :attacker-model :attacker-unit-models)]])

(defn dropdown-attacker-weapons []
  [:div.columns
   [:div.column (dropdown-weapons (:weapons (:attacker-model @session)))]])

(defn dropdown-defender-units []
  [:div.columns
       [:div.column (dropdown-units
                     (:defender-unit-models @session)
                     :defender-model :defender-unit-models)]])


(defn home-page []
  [:section.section>div.container>div.content
   (title)
   (when (:show-upload-files @session)
     (upload-rosters))
   (when-not (:show-upload-files @session)
     (when-not (and (:attacker-model @session) (:defender-model @session))
       (init-models! (:attacker-roster @session) :attacker-unit-models)
       (init-models! (:defender-roster @session) :defender-unit-models))

     (list
      (dropdown-attacker-units)
      (dropdown-defender-units)
      (dropdown-attacker-weapons)
      [:div.columns [:div.column (models)]]
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
