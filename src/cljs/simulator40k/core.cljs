(ns simulator40k.core
  (:require
   [clojure.browser.dom :as dom]
   [goog.string.format]
   [ajax.core :refer [POST]]
   [reagent.core :as r]
   [reagent.dom :as rdom]
   [goog.events :as events]
   [goog.history.EventType :as HistoryEventType]
   [simulator40k.ajax :as ajax]
   [simulator40k.home :as home]
   [simulator40k.state :as state]
   [reitit.core :as reitit]
   [clojure.string :as string])
  (:import goog.History))

(def DEBUG false)

(defn simulation-stats []
  (when DEBUG
    [:div [:p (str "Attacker " (-> @state/session :attacker-model :chars))]
     [:p (str "Attacker Weapon selected" (-> @state/session :attacker-weapon-selected))]
     [:p (str "Weapons " (map :name (-> @state/session :attacker-model :weapons)))]
     [:p (str "Defender " (-> @state/session :defender-model :chars))]
     [:p (str "Hit rule " (:hit-rule @state/session))]
     [:p (str "Wound rule " (:wound-rule @state/session))]
     [:p (str "Ap rule " (:ap-rule @state/session))]
     [:p (str "Damage rule " (:damage-rule @state/session))]
     [:p (str "Number shot rule " (:number-shot-rule @state/session))]
     [:p [:b (str "Damage: " ) ]
      (str (-> @state/session :graph-data :damage))
      (-> @state/session :graph-data :damage-stats)]


     [:p (str "Experiments " (-> @state/session :experiments))]

     ])
  )

(defn graph-rolls []
  [:div {:id "graph-rolls"}]
  )

(defn graph []
  [:div

   [:div.columns
    [:div.column
     [:div {:id "graph"}]]
    [:div.column
     [:div {:id "graph-damage"}]]
    ]

   ])

;;SECOND PAGE

;; DROPDOWNS
(defn set-model! [force-id unit-id model-id k from-models]
  (let [model (:model (first (filter #(and (= (:id (:force %)) force-id) (= (:id (:unit %)) unit-id) (= (:id (:model %)) model-id)) (from-models @state/session))))]
    (when (= k :attacker-model)
      (swap! state/session assoc :attacker-weapon-selected
             (first (:weapons model))))

    (swap! state/session assoc k model)))

(defn set-weapon! [weapon-id]
  (swap! state/session assoc :attacker-weapon-selected
         (first (filter #(= (:id %) weapon-id) (:weapons (:attacker-model @state/session))))))

(defn dropdown-units [models k belong-to]
  [:div.field
   [:div.select.is-dark.full-width
    [:select.full-width {:key       (str "select-" (name belong-to))
                         :id        (str "select-" (name belong-to))
                         :on-change #(let [select          (.-target %)
                                           selected-option (.-attributes (aget (.-options select) (.-selectedIndex select)))
                                           force-id         (.-value (.getNamedItem selected-option "idforce"))
                                           unit-id         (.-value (.getNamedItem selected-option "idunit"))
                                           model-id        (.-value (.getNamedItem selected-option "idmodel"))]
                                       (println "selected " force-id unit-id model-id k belong-to)
                                       (set-model! force-id unit-id model-id k belong-to))}
     (for [m models]
       ;; [:optgroup  {:key   (str m (:id (:unit m)))
       ;;              :label (:name (:unit m))}]
       [:option
        {:key     (str (:id (:force m)) (:id (:unit m)) (:id (:model m)))
         :idforce (:id (:force m))
         :idunit  (:id (:unit m))
         :idmodel (:id (:model m))} (:name (:model m))])]]])

(def num-rules
  (r/atom
   {:hit    1
    :wound  1
    :damage 1
    :ap     1
    :runs   1}))

(defn dropdown [title data on-change-f id]
  [:div [:h6 title]
   [:div.columns
    [:div.column
     [:div.columns {:key (str title)}
      [:div.field
       [:div.select.is-dark.full-width
        [:select.full-width {:key       id
                             ;;:id-select i
                             :id        id
                             :on-change on-change-f
                             :disabled (if (or (= "Damage rules" title) (= "Ap rules" title))
                                         true
                                         false)}
         (for [d data]
           [:option
            {:key (str d title (:id d))
             ;;:id-select i
             :id  (:id d)} (:value d)])]]]

      ]]]])

(defn dropdown-attacker-units []
  [:div
   [:h6 {:key "attacker"} "Attacker"]
   [:div.columns {:key "units"}
    [:div.column (dropdown-units
                  (:attacker-unit-models @state/session)
                  :attacker-model :attacker-unit-models)]]])

(defn dropdown-defender-units []
  [:div [:h6 "Defender"]
   [:div.columns
    [:div.column (dropdown-units
                  (:defender-unit-models @state/session)
                  :defender-model :defender-unit-models)]]])

(defn dropdown-weapons [weapons]
  [:div [:h6 {:key "title"} "Weapon"]
   [:div.field {:key "field"}
    [:div.select.is-dark.full-width
     [:select#select-weapons.full-width {:id        "select-weapons"
                                         :on-change #(let [e         (js/document.getElementById "select-weapons")
                                                           weapon-id (.-value (.-id (.-attributes (aget (.-options e) (.-selectedIndex e)))))]
                                                       (set-weapon! weapon-id))}
      (for [w weapons]
        [:option
         {:id  (:id w)
          :key (str w)} (:name w)])]]]])

(defn dropdown-attacker-weapons []
  [:div.columns
   [:div.column (dropdown-weapons (:weapons (:attacker-model @state/session)))]])


;; END DROPDOWNS


;; MODEL SELECTED


(defn attacker []
  [:div
   [:div.border
    [:h6 {:key "title"} (-> @state/session :attacker-model :name)]
    [:div.field.is-horizontal {:key "BS"}
     [:div.field-label.is-normal
      [:label.label "BS/WS:"]]
     [:input.input
      {:type      "text"
       :value     (-> @state/session :attacker-model :chars :bs)
       :on-change (fn [e]
                       (swap! state/session assoc-in [:attacker-model :chars :bs] (-> e .-target .-value)))}]]
    [:div.field.is-horizontal {:key "Models"}
     [:div.field-label.is-normal
      [:label.label "Models"]]
     [:input.input
      {:type      "text"
       :value     (-> @state/session :attacker-model :number)
       :on-change (fn [e]
                       (swap! state/session assoc-in [:attacker-model :number
                                                      ] (-> e .-target .-value)))}]]
    (-> @state/session :attacker-model :chars :description)]])

(defn index-selected-weapon []
  (:id (:attacker-weapon-selected @state/session)))

(defn assoc-weapon-selected! []
  (swap! state/session assoc-in [:attacker-model :weapons] [(:attacker-weapon-selected @state/session)]))

(defn weapon-attacks []
  (-> @state/session :attacker-weapon-selected :weapon-attacks))

(defn attacker-weapons []
  [:div
   [:div.border
    [:h6 (-> @state/session :attacker-weapon-selected :name)]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "AP:"]]
     [:input.input
      {:type      "text" :value (-> @state/session :attacker-weapon-selected :chars :ap)
       :on-change (fn [e]
                    (swap! state/session assoc-in [:attacker-weapon-selected :chars :ap] (-> e .-target .-value)))}]]

    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Attacks:"]]
     [:input.input
      {:type      "text" :value (weapon-attacks)
       :on-change (fn [e]
                    (swap! state/session assoc-in [:attacker-weapon-selected :weapon-attacks] (-> e .-target .-value)))}]]

    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Strength:"]]
     [:input.input
      {:type      "text" :value (-> @state/session :attacker-weapon-selected :chars :s)
       :on-change (fn [e]
                    (swap! state/session assoc-in [:attacker-weapon-selected :chars :s] (-> e .-target .-value)))}]]

    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Damage:"]]
     [:input.input
      {:type      "text" :value (-> @state/session :attacker-weapon-selected :chars :d)
       :on-change (fn [e]
                    (swap! state/session assoc-in [:attacker-weapon-selected :chars :d] (-> e .-target .-value)))}]]


    (-> @state/session :attacker-weapon-selected :chars :abilities)]])

(defn defender []
  [:div
   [:div.border
    [:h6 (-> @state/session :defender-model :name)]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Save: "]]
     [:input.input
      {:type      "text" :value (-> @state/session :defender-model :chars :save)
       :on-change (fn [e]
                    (swap! state/session assoc-in [:defender-model :chars :save] (-> e .-target .-value)))}]]

    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Toughness:"]]
     [:input.input
      {:type      "text" :value (-> @state/session :defender-model :chars :t)
       :on-change (fn [e]
                    (swap! state/session assoc-in [:defender-model :chars :t] (-> e .-target .-value)))}]]
    [:p [:b "Wounds: "] (-> @state/session :defender-model :chars :w)]
    [:p "(if Invuln save set AP 0)"]

    (-> @state/session :defender-model :chars :description)]])

(defn models []
  [:div.margin
   [:div.columns
    [:div.column {:key "attacker"} (attacker)]
    [:div.column {:key "weapon"} (attacker-weapons)]
    [:div.column {:key "defender"} (defender)]]])

;; END MODELS

(defn init-models! [forces type-key]
  (let [unit-models (for [f forces
                          u (:units f)
                          m (:models u)]
                      {:force f :unit u :model m})]

    (swap! state/session assoc type-key unit-models))
  (set-model! "0" "0" "0" :attacker-model :attacker-unit-models)
  (set-model! "0" "0" "0" :defender-model :defender-unit-models)
  (swap! state/session assoc :attacker-weapon-selected (first (:weapons (:attacker-model @state/session)))))


(def rule->key
  {"None"        nil
   "Re-roll 1s"  :re-roll-1s
   "Re-roll all" :re-roll-all
   "Auto hit"    :auto-hit
   "FNP 5+"      :fnp-5+
   "FNP 6+"      :fnp-6+
   "Exploding 6's" :exploding-6s})


(defn read-response [response]
  (-> (.parse js/JSON response) (js->clj :keywordize-keys true)))


(def show-loader (r/atom false))

(def fight-error (r/atom false))

(defn handler-error-fight [response]
  (swap! state/session assoc :fight-error true)
  )


(defn handler-fight [response]
  (swap! state/session assoc :show-table true)
  (swap! state/session assoc :experiments (:experiments (:fight (read-response response))))
  (swap! state/session assoc :fight-error false)
  (swap! state/session assoc :graph-data (:fight (read-response response)))

  (js/Plotly.newPlot (.getElementById js/document "graph")
                     (clj->js
                      [

                       ;; {:x ["no success"]
                       ;;  :y [(-> @state/session :graph-data :not-success)]
                       ;;  :showlegend true
                       ;;  :name "no success"
                       ;;  :type "bar"}

                       {:x          ["hit"]
                        :y          [(-> @state/session :graph-data :percentage-hit)]
                        :name       "hit"
                        :showlegend true
                        :type       "bar"}

                       ;; {:x ["no hit"]
                       ;;  :y [(-> @state/session :graph-data :not-hits)]
                       ;;  :name "no hit"
                       ;;  :showlegend true
                       ;;  :type "bar"}

                       {:x          ["wound"]
                        :y          [(-> @state/session :graph-data :percentage-wound)]
                        :name       "wound"
                        :showlegend true
                        :type       "bar"}

                       ;; {:x ["no wound"]
                       ;;  :y [(-> @state/session :graph-data :not-wounds)]
                       ;;  :name "no wound"
                       ;;  :showlegend true
                       ;;  :type "bar"}

                       ;; {:x          ["save"]
                       ;;  :y          [(-> @state/session :graph-data :percentage-save)]
                       ;;  :name       "save"
                       ;;  :showlegend true
                       ;;  :type       "bar"}


                       {:x ["no save"]
                        :y [(-> @state/session :graph-data :percentage-not-save)
                            ]
                        :name "no save"
                        :showlegend true
                        :type "bar"}

                       ;; {:x          ["success"]
                       ;;  :y          [(-> @state/session :graph-data :percentage-success)]
                       ;;  :showlegend true
                       ;;  :name       "success"
                       ;;  :type       "bar"}
                       ])(clj->js {:title      "% Hit Wound No Save"
                                   :yaxis      {:title {:text "Percentage"}}
                                   :responsive true}))

  ;; TODO: fill the gaps with 0 0 on damage
  ;; damage should take into consideration the wounds of the enemy
  (js/Plotly.newPlot (.getElementById js/document "graph-damage")
                     (clj->js
                      [{
                        :name ""
                        ;;:boxpoints false
                        ;;:width "100px"
                        ;;:heigth "100px"
                        ;;:orientation "h"
                        ;;:mode "markers"
                        :y (-> @state/session :graph-data :damage)
                        ;; :lowerfence [(:Min (-> @state/session :graph-data :damage-stats))]
                        ;; :q1 [(:Q1 (-> @state/session :graph-data :damage-stats))]
                        ;; :median [(:Median (-> @state/session :graph-data :damage-stats))]
                        ;; :q3 [(:Q3 (-> @state/session :graph-data :damage-stats))]
                        ;; :upperfence [(:Max (-> @state/session :graph-data :damage-stats))(-> @state/session :graph-data :max-damage)]
                        :type "box"}])
                     (clj->js {:title "Damage"
                               :yaxis      {:title {:text "Damage"}}}))

  (js/Plotly.newPlot (.getElementById js/document "graph-rolls")
                     (clj->js
                      [{
                        :x    (map first (-> @state/session :graph-data :rolls))
                        :y    (map second (-> @state/session :graph-data :rolls))

                        :type "bar"}])
                     (clj->js {:title "rolls"
                               :yaxis      {:title {:text "n. rolls"}}}))


  (swap! state/session assoc :fight-error false)
  (swap! state/session assoc :show-loader-fight  false))

(defn table-stats []
  (when (-> @state/session :show-table)
    [:div.column
     [:table.table
      [:thead
       [:th "Stat"]
       [:th "Value"]]
      [:tbody
       [:tr
        [:td "Hit"]
        [:td (str (-> @state/session :graph-data :percentage-hit) "%")]]
       [:tr
        [:td "Wound"]
        [:td (str (-> @state/session :graph-data :percentage-wound) "%")]]
       [:tr
        [:td "No Save"]
        [:td (str (-> @state/session :graph-data :percentage-not-save) "%")]]
       ;; [:tr
       ;;  [:td "Success"]
       ;;  [:td (str (-> @state/session :graph-data :percentage-success) "%")]]
        ;; [:tr
       ;;  [:td "Average wounds: "]
       ;;  [:td (-> @state/session :graph-data :avg-damage)]]
       ;; [:tr
       ;;  [:td "Mode wounds: "]
       ;;  [:td (-> @state/session :graph-data :mode-damage)]]

       ]]]))

(defn table-damage []
  (when (-> @state/session :show-table)
    (let [stats (-> @state/session :graph-data :damage-stats)]
      [:div
       [:table.table
        [:thead
         [:th "Damage"]
         [:th "Value"]
         ]
        [:tbody
         [:tr
          [:td "Min"]
          [:td (:Min stats)]]
         [:tr
          [:td "Q1"]
          [:td (:Q1 stats)]]
         [:tr
          [:td "Median"]
          [:td (:Median stats)]]
         [:tr
          [:td "Q3"]
          [:td (:Q3 stats)]]
         [:tr
          [:td "Max"]
          [:td (:Max stats)]]

         ]]])
    )
  )

(defn home-page []
  [:section.section>div.container>div.content
   (home/title)
   (if (:show-upload-files @state/session)
     (home/upload-rosters)
     (do
       (when (and (empty? (:attacker-unit-models @state/session)) (empty? (:defender-unit-models @state/session)))
         (init-models! (:attacker-roster @state/session) :attacker-unit-models)
         (init-models! (:defender-roster @state/session) :defender-unit-models))
       [:div
        [:div.columns
         [:div.column (dropdown-attacker-units)]
         [:div.column (dropdown-attacker-weapons)]
         [:div.column (dropdown-defender-units)]]

        (models)



        [:div.columns
         [:div.column
          (dropdown "Hit rules"
                    state/hit-rules
                    (fn [event]
                      (.preventDefault event)
                      (let [e            (.-target event)
                            id           (.-value (.-id (.-attributes (aget (.-options e) (.-selectedIndex e)))))
                            hit-rule-str (first (filter #(= (:id %) id) state/hit-rules))
                            hit-rule     (get rule->key (:value (js->clj hit-rule-str)))]
                        (println "hit rule" hit-rule)
                        (swap! state/session assoc :hit-rule hit-rule))) "hit-rules")]
         [:div.column
          (dropdown "Wounds rules"
                    state/wound-rules
                    (fn [event]
                      (.preventDefault event)
                      (let [e              (.-target event)
                            id             (.-value (.-id (.-attributes (aget (.-options e) (.-selectedIndex e)))))
                            wound-rule-str (first (filter #(= (:id %) id) state/wound-rules))
                            wound-rule     (get rule->key (:value (js->clj wound-rule-str)))]
                        (swap! state/session assoc :wound-rule wound-rule)))

                    "wound-rules")]

         [:div.column
          (dropdown "Number of shots"
                    state/number-shot-rules
                    (fn [event]
                      (.preventDefault event)
                      (let [e              (.-target event)
                            id             (.-value (.-id (.-attributes (aget (.-options e) (.-selectedIndex e)))))
                            number-shot-rule-str (first (filter #(= (:id %) id) state/wound-rules))
                            number-shot-rule     (get rule->key (:value (js->clj number-shot-rule-str)))]
                        (swap! state/session assoc :number-shot-rule number-shot-rule)))
                    "number-shot-rules")]

         [:div.column
          (dropdown "Ap rules"
                    state/ap-rules (fn [e] ()) "ap rules")]

         [:div.column
          (dropdown "Runs"
                    state/runs-experiments
                    (fn [element]
                      (.preventDefault element)
                      (let [e    (.getElementById js/document "runs")
                            id   (.-value (.-id (.-attributes (aget (.-options e) (.-selectedIndex e)))))
                            runs (first (filter #(= (:id %) id) state/runs-experiments))]
                        (println "set runs experiments" e id runs)
                        (swap! state/session assoc :runs (:value runs)))) :runs)]]

        [:div.columns {:key "fight"}
         [:div.column
          [:button.button.is-dark.is-medium
           {:name     "fight"
            :on-click (fn [e]
                        (.preventDefault e)
                         ;;(:show-loader @state/session)
                        (swap! state/session assoc :show-loader-fight true)
                        (let [attacker (:attacker-model @state/session)
                              attacker (assoc
                                        attacker
                                        :weapons [(:attacker-weapon-selected @state/session)])]

                          (POST "/api/fight" {:params        {:attacker attacker
                                                              :defender (:defender-model @state/session)
                                                              :rules (select-keys @state/session [:hit-rule :wound-rule :ap-rule :damage-rule :number-shot-rule])
                                                              :damage-rule  (:damage-rule @state/session)
                                                              :n        (:runs @state/session)}
                                              :handler       handler-fight
                                              :error-handler handler-error-fight})

                          (swap! state/session assoc :show-table false))
                        )
            }

           "Fight"]
          [:div.column
           (when (:show-loader-fight @state/session)
             [:div.loader])]]
         ;; [:div.column
         ;;  [:button.button.is-dark.is-medium
         ;;   {:name     "Swap"
         ;;    :on-click (fn [e]
         ;;                (.preventDefault e)

         ;;                (println "swap!")
         ;;                (let [attacker             (-> @state/session :attacker-model)
         ;;                      weapons              (-> @state/session :defender-model :weapons)
         ;;                      defender             (:defender-model @state/session)
         ;;                      attacker-unit-models (-> @state/session :attacker-unit-models)]
         ;;                  ;;(init-models! (:defender-roster @state/session) :attacker-unit-models)
         ;;                  ;;(init-models! (:attacker-roster @state/session) :defender-unit-models)
         ;;                  ;;(init-weapons!)
         ;;                  (swap! state/session assoc :attacker-unit-models (-> @state/session :defender-unit-models))
         ;;                  (swap! state/session assoc :defender-unit-models attacker-unit-models)
         ;;                  (swap! state/session assoc :attacker-model defender)
         ;;                  (swap! state/session assoc :attacker-model defender)
         ;;                  (swap! state/session assoc :attacker-weapons weapons)
         ;;                  (swap! state/session assoc :defender-model attacker)
         ;;                  (set-weapon! "0")))} "Swap Attacker defender"]]



         [:div.column
          (when (:fight-error @state/session)
            [:div.has-background-danger-light "ERROR re-check parameters units"])]]]))



   ;;(str (-> @state/session :graph-data))


   (graph)
   [:div.columns
    [:div.column
     (table-stats)]

    [:div.column
     (graph-rolls)]

    (simulation-stats)]
   ])

(def pages
  {:home #'home-page})

(defn page []
  [(pages (:page @state/session))])

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
       (swap! state/session assoc :page (match-route (.-token event)))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-components []
  (rdom/render [#'home/title] (.getElementById js/document "title"))
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (enable-console-print!)
  (ajax/load-interceptors!)
  (hook-browser-navigation!)
  (mount-components))



;;(cljs.pprint/pprint (first (filter #(= (:id %) "1") (:attacker-weapons @state/session))))
;;(cljs.pprint/pprint (:attacker-model @state/session))
;;(cljs.pprint/pprint (:graph-data @state/session))


;;(swap! state/session update :restart #(complement %))
