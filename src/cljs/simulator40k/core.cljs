;; TODO: for melee weapons use attacks, for shooting leave type
;; TODO: fix multi force dropdown
;; on change values form
(ns simulator40k.core
  (:require
   [goog.string :as gstring]
   [goog.string.format]
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

(def number-experiments "10")

(def empty-state
  {:attacker-roster         nil
   :defender-roster         nil
   :number-experiments      number-experiments
   :defender-unit-models    nil
   :attacker-unit-models    nil
   :attacker-model nil
   :defender-model nil
   :graph-data              nil
   :attacker-weapons        nil
   :attacker-weapon-selected nil
   :page                    :home
   :show-upload-files       true
   :show-models             false
   :show-graph              false
   :files                   {:Attacker nil
                             :Defender nil}})

(defonce session (r/atom empty-state))

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

;; (-> (c/histogram (:graph-data @session) :x-axis [(apply cljs.core/min (:graph-data @session)) (+ 1 (apply cljs.core/max (:graph-data @session)))])
;;     (s/as-svg :width 500 :height 200))


(defn layout [n]
  (clj->js {:xaxis {:type  "integer",
                    :autorange false
                    :fixedrange true
                    :range [0 10]}
            :yaxis {:title "Value"
                    :tickformat ",d"}
            :barmode "group"
            :bargap 0.5
            :width 450
            }))



(def data {:type "histogram"
           :xbins {:size 1}})

;;(str (:graph-data @session))
(defn graph []
  (list

   ;;(-> @session :graph-data)
   (-> @session :graph-data :stats :total-success)
   [:div {:id "graph"}]

   (when (-> @session :graph-data :percentage-success)
     (list [:p
            [:b (str "Success ")] (str (gstring/format "%.2f" (-> @session :graph-data :percentage-success)) "%")]
           ))

   (when (-> @session :graph-data :avg-damage)
     [:p
        [:b (str "Wounds: " )] (-> @session :graph-data :avg-damage)]
     )



   )




  )


;;FIRST PAGE
(defn title []
  [:div.columns
   [:div.column
    [:div.title
     [:a {:href     "/#"
          :on-click (fn [_]
                      (reset! session empty-state))}
      [:h1 "Simulator 40k"]]]]])

(defn file-roaster [role]
  [:div.field
   [:label.label (str (name role) " roaster:")]
   [:div.file
    [:label.file-label.full-width
     [:input.file-input {:id        (name role)
                         :name      "resume", :type "file"
                         :on-change (fn [e]
                                      (swap! session assoc-in [:files role] (-> e .-target .-value)))}]
     [:span.file-cta.full-width
      [:span.file-icon [:i.fas.fa-upload]]
      [:span.file-label "\n        Choose a file…\n      "]]
     [:span.file-name (role (:files @session))]]]])


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

;;SECOND PAGE

;; DROPDOWNS
(defn set-model! [force-id unit-id model-id k from-models]
  (let [model (:model (first (filter #(and (= (:id (:force %)) force-id) (= (:id (:unit %)) unit-id) (= (:id (:model %)) model-id)) (from-models @session))))]
    (when (= k :attacker-model)
      (swap! session assoc :attacker-weapons (:weapons model))
      (swap! session assoc :attacker-weapon-selected
             (first (:weapons model))))


    (swap! session assoc k model)))

(defn set-weapon! [weapon-id]
  (swap! session assoc :attacker-weapon-selected
         (first (filter #(= (:id %) weapon-id) (:attacker-weapons @session)))))

(defn dropdown-units [models k belong-to]
  [:div.field
   [:div.select.is-dark.full-width
    [:select.full-width {:id        (str "select-" belong-to)
                         :on-change #(let [e        (js/document.getElementById (str "select-" belong-to))
                                           force-id  (.-value (.-idforce (.-attributes (aget (.-options e) (.-selectedIndex e)))))
                                           unit-id  (.-value (.-idunit (.-attributes (aget (.-options e) (.-selectedIndex e)))))
                                           model-id (.-value (.-idmodel (.-attributes (aget (.-options e) (.-selectedIndex e)))))]

                                       (println "selected " force-id unit-id model-id k belong-to)
                                       (set-model! force-id unit-id model-id k belong-to))}
     (for [m models]
       ^{:key (str m (:id (:unit m)))} [:optgroup  {:label (:name (:unit m))}
                                        ^{:key (str m)} [:option
                                                         {:idforce  (:id (:force m))
                                                          :idunit  (:id (:unit m))
                                                          :idmodel (:id (:model m))} (:name (:model m))]])]]])


(defn dropdown [title data on-change-f]
  (list [:h1 title]
        [:div.columns
         [:div.column
          [:div.field
           [:div.select.is-dark.full-width
            [:select.full-width {:id        (str "select-" title)
                                 :on-change on-change-f}
     (for [d data]
       ^{:key (str d (:id d))}
       [:option
        {:id (:id d)} (:value d)])]]]]]))




(defn dropdown-attacker-units []
  (list [:h1 "Attacker"]
        [:div.columns
         [:div.column (dropdown-units
                       (:attacker-unit-models @session)
                       :attacker-model :attacker-unit-models)]]))

(defn dropdown-defender-units []
  (list [:h1 "Defender"]
   [:div.columns
    [:div.column (dropdown-units
                  (:defender-unit-models @session)
                  :defender-model :defender-unit-models)]]))

(defn dropdown-weapons [weapons]
  (list [:h1 "Weapon"]
        [:div.field
                       [:div.select.is-dark.full-width
                        [:select#select-weapons.full-width {:id        "select-weapons"
                                                            :on-change #(let [e         (js/document.getElementById "select-weapons")
                                                                              weapon-id (.-value (.-id (.-attributes (aget (.-options e) (.-selectedIndex e)))))
                                                                              ]
                                                                          (set-weapon! weapon-id))}
                         (for [w weapons]
                           [:option
                            {:id (:id w)} (:name w)])]]]))

(defn dropdown-attacker-weapons []
  [:div.columns
   [:div.column (dropdown-weapons (:weapons (:attacker-model @session)))]])


;; END DROPDOWNS


;; MODEL SELECTED
(defn experiments []
  [:div.field.is-horizontal
   [:div.field-label.is-normal
    [:label.label "Runs:"]]
   [:input.input
    {:type         "text"
     :defaultValue (:number-experiments @session)
     :on-change (fn [e]
                   (swap! session assoc :number-experiments (-> e .-target .-value)))
     }]
   (-> @session :number-experiments)])


(defn attacker []
  (list
   (-> @session :attacker-model :chars)
   [:h1 (-> @session :attacker-model :name)]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "BS/WS:"]]
    [:input.input
     {:type "text"
      :defaultValue (-> @session :attacker-model :chars :bs)
      :on-change (fn [e]
                   (swap! session assoc-in [:attacker-model :chars :bs] (-> e .-target .-value)))}]
    (-> @session :attacker-model :chars :bs)]))


(defn index-selected-weapon []
  (:id (:attacker-weapon-selected @session)))

(defn assoc-weapon-selected! []
  (swap! session assoc-in [:attacker-model :weapons] [(:attacker-weapon-selected @session)]))

(defn attacker-weapons []
  (list
   (-> @session :attacker-weapon-selected)
   [:h1 (-> @session :attacker-weapon-selected :name)]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "AP:"]]
    [:input.input
     {:type "text" :defaultValue (-> @session :attacker-weapon-selected :chars :ap)
      :on-change (fn [e]
                   (swap! session assoc-in [:attacker-weapon-selected :chars :ap] (-> e .-target .-value))
                   (assoc-weapon-selected!))}
     ]

    ]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Attacks:"]]
    [:input.input
     {:type "text" :defaultValue (-> @session :attacker-weapon-selected :chars :type)
      :on-change (fn [e]
                   (swap! session assoc-in [:attacker-weapon-selected :chars :type] (-> e .-target .-value))
                   (assoc-weapon-selected!)
                  )}]
    (-> @session :attacker-weapon-selected :chars :type)]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Strength:"]]
    [:input.input
     {:type "text" :defaultValue (-> @session :attacker-weapon-selected :chars :s)
      :on-change (fn [e]
                   (swap! session assoc-in [:attacker-weapon-selected :chars :s] (-> e .-target .-value))
                   (assoc-weapon-selected!)
                   )}]

    ]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Damage:"]]
    [:input.input
     {:type "text" :defaultValue (-> @session :attacker-weapon-selected :chars :d)
      :on-change (fn [e]
                   (swap! session assoc-in [:attacker-weapon-selected :chars :d] (-> e .-target .-value))
                   (assoc-weapon-selected!)
                  )}]]
   ))

(defn defender []
  (list
   (-> @session :defender-model :chars)
   [:h1 (-> @session :defender-model :name)]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Save:"]]
    [:input.input
     {:type "text" :defaultValue (-> @session :defender-model :chars :save)
      :on-change (fn [e]
                   (swap! session assoc-in [:defender-model :chars :save] (-> e .-target .-value)))}]
    (-> @session :defender-model :chars :save)]
   [:div.field.is-horizontal
    [:div.field-label.is-normal
     [:label.label "Toughness:"]]
    [:input.input
     {:type "text" :defaultValue (-> @session :defender-model :chars :t)
      :on-change (fn [e]
                   (swap! session assoc-in [:defender-model :chars :t] (-> e .-target .-value)))}]
    (-> @session :defender-model :chars :t)]))

(defn models []
  [:div.margin
   (attacker)
   (attacker-weapons)
   (defender)])

;; END MODELS

(defn init-weapons! []
  (swap! session assoc :attacker-weapons (:weapons (:attacker-model @session)))
  )

(defn init-models! [forces type-key]
  (let [unit-models (for [f forces
                          u (:units f)
                          m (:models u)]
                      {:force f :unit u :model m})]

    (swap! session assoc type-key unit-models))
  (set-model! "0" "0" "0" :attacker-model :attacker-unit-models)
  (set-model! "0" "0" "0" :defender-model :defender-unit-models)
  )

(defn read-response [response]
  (-> (.parse js/JSON response) (js->clj :keywordize-keys true)))


(defn graph-data [k]
  (js->clj (for [i (range (-> @session :graph-data k))]
             k)))


(defn handler-fight [response]
  (js/Plotly.newPlot (.getElementById js/document "graph")
                     (clj->js
                      [
                       {:x ["successes" "hits" "wounded" "saves" "damage"]
                        :y [(-> @session :graph-data :stats :total-success)
                            (-> @session :graph-data :stats :total-hits)
                            (-> @session :graph-data :stats :total-wounds)
                            (-> @session :graph-data :stats :total-saves)
                            (-> @session :graph-data :stats :total-damage)
                                                        ]
                        :type "bar"}
                       ]))
  (swap! session assoc :show-graph true)

  (swap! session assoc :graph-data (:fight (read-response response))))

(defn home-page []
  [:section.section>div.container>div.content
   (title)
   (when (:show-upload-files @session)
     (upload-rosters))
   (when-not (:show-upload-files @session)
     (when-not (and (:attacker-model @session) (:defender-model @session))
       (init-models! (:attacker-roster @session) :attacker-unit-models)
       (init-models! (:defender-roster @session) :defender-unit-models)
       (init-weapons!)

       )
     (list

      (str (:attacker-model @session) (:defender-model @session))
      (dropdown-attacker-units)
      (dropdown-defender-units)
      (dropdown-attacker-weapons)
      [:div.columns [:div.column (models)]]
      (dropdown "Re-roll number of shots"
                [{:id "1" :value "none"}
                 {:id "2" :value "1s"}
                 {:id "3" :value "all"}] (fn [e] ()))
      (dropdown "Re-roll number of wounds"
                [{:id "1" :value "none"}
                 {:id "2" :value "1s"}
                 {:id "3" :value "all"}] (fn [e] ()))
      (dropdown "Additional rules"
                [{:id "1" :value "None"}
                 {:id "2" :value "disgusting resilience"}
                 {:id "3" :value "quantum shielding"}] (fn [e] ()))
      (experiments)
      [:button.button.is-dark
            {:name     "fight"
             :on-click (fn [e]
                         (.preventDefault e)
                         (println "click")

                         (POST "/api/fight" {:params    {:attacker (:attacker-model @session)
                                                         :defender (:defender-model @session)
                                                         :n (:number-experiments @session)}
                                             :handler handler-fight})




                         )} "Fight"]))




    (graph)])

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



;;(cljs.pprint/pprint (first (filter #(= (:id %) "1") (:attacker-weapons @session))))
;;(cljs.pprint/pprint (:attacker-model @session))
;;(cljs.pprint/pprint (:graph-data @session))
