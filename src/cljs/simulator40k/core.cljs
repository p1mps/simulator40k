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
  {:restart                 false
   :attacker-roster         nil
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
  [:div

   ;;(-> @session :graph-data)
   [:div {:id "graph"}]

   (when (-> @session :graph-data :avg-damage)
     [:div
      [:p {:key "success"}
       [:b "Success "] (str (-> @session :graph-data :percentage-success) "%" )]
      [:p {:key "max-wounds"}
       [:b (str "Max wounds: " )] (-> @session :graph-data :max-damage)]
      [:p {:key "average-wounds"}
       [:b (str "Average Wounds: " )] (-> @session :graph-data :avg-damage)]])]




  )


;;FIRST PAGE
(defn title []
  [:div.columns
   [:div.column
    [:div.title
     [:a {:href     "/#"
          :on-click (fn [_]
                      (reset! session empty-state))}
      [:h6 "Simulator 40k"]]]]])

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
       [:optgroup  {:key (str m (:id (:unit m)))
                    :label (:name (:unit m))}
        [:option
         {:key (str m)
          :idforce  (:id (:force m))
          :idunit  (:id (:unit m))
          :idmodel (:id (:model m))} (:name (:model m))]])]]])


(defn dropdown [title data on-change-f]
  [:div [:h6 title]
        [:div.columns
         [:div.column
          [:div.field
           [:div.select.is-dark.full-width
            [:select.full-width {:id        (str "select-" title)
                                 :on-change on-change-f}
     (for [d data]
       [:option
        {:key (str d (:id d))
         :id (:id d)} (:value d)])]]]]]])




(defn dropdown-attacker-units []
  [:div
   [:h6 {:key "attacker"} "Attacker"]
   [:div.columns {:key "units"}
    [:div.column (dropdown-units
                  (:attacker-unit-models @session)
                  :attacker-model :attacker-unit-models)]]])

(defn dropdown-defender-units []
  [:div [:h6 "Defender"]
   [:div.columns
    [:div.column (dropdown-units
                  (:defender-unit-models @session)
                  :defender-model :defender-unit-models)]]])

(defn dropdown-weapons [weapons]
  [:div [:h6 {:key "title"} "Weapon"]
        [:div.field {:key "field"}
         [:div.select.is-dark.full-width
          [:select#select-weapons.full-width {:id        "select-weapons"
                                              :on-change #(let [e         (js/document.getElementById "select-weapons")
                                                                weapon-id (.-value (.-id (.-attributes (aget (.-options e) (.-selectedIndex e)))))
                                                                ]
                                                            (set-weapon! weapon-id))}
           (for [w weapons]
             [:option
              {:id (:id w)
               :key (str w)} (:name w)])]]]])

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
   ])


(defn attacker []
  [:div
   [:div.border
    [:h6 {:key "title"} (-> @session :attacker-model :name)]
    [:div.field.is-horizontal {:key "field"}
     [:div.field-label.is-normal
      [:label.label "BS/WS:"]]
     [:input.input
      {:type         "text"
       :defaultValue (-> @session :attacker-model :chars :bs)
       :on-change    (fn [e]
                    (swap! session assoc-in [:attacker-model :chars :bs] (-> e .-target .-value)))}]
     ]]])


(defn index-selected-weapon []
  (:id (:attacker-weapon-selected @session)))

(defn assoc-weapon-selected! []
  (swap! session assoc-in [:attacker-model :weapons] [(:attacker-weapon-selected @session)]))

(defn attacker-weapons []
  [:div
   [:div.border
    [:h6 (-> @session :attacker-weapon-selected :name)]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "AP:"]]
     [:input.input
      {:type      "text" :defaultValue (-> @session :attacker-weapon-selected :chars :ap)
       :on-change (fn [e]
                    (swap! session assoc-in [:attacker-weapon-selected :chars :ap] (-> e .-target .-value))
                    (assoc-weapon-selected!))}
      ]

     ]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Attacks:"]]
     [:input.input
      {:type      "text" :defaultValue (-> @session :attacker-weapon-selected :chars :type)
       :on-change (fn [e]
                    (swap! session assoc-in [:attacker-weapon-selected :chars :type] (-> e .-target .-value))
                    (assoc-weapon-selected!)
                    )}]
     ]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Strength:"]]
     [:input.input
      {:type      "text" :defaultValue (-> @session :attacker-weapon-selected :chars :s)
       :on-change (fn [e]
                    (swap! session assoc-in [:attacker-weapon-selected :chars :s] (-> e .-target .-value))
                    (assoc-weapon-selected!)
                    )}]

     ]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Damage:"]]
     [:input.input
      {:type      "text" :defaultValue (-> @session :attacker-weapon-selected :chars :d)
       :on-change (fn [e]
                    (swap! session assoc-in [:attacker-weapon-selected :chars :d] (-> e .-target .-value))
                    (assoc-weapon-selected!)
                    )}]]
    ]])

(defn defender []
  [:div
   [:div.border
    [:h6 (-> @session :defender-model :name)]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Save:"]]
     [:input.input
      {:type      "text" :defaultValue (-> @session :defender-model :chars :save)
       :on-change (fn [e]
                    (swap! session assoc-in [:defender-model :chars :save] (-> e .-target .-value)))}]
     ]
    [:div.field.is-horizontal
     [:div.field-label.is-normal
      [:label.label "Toughness:"]]
     [:input.input
      {:type      "text" :defaultValue (-> @session :defender-model :chars :t)
       :on-change (fn [e]
                    (swap! session assoc-in [:defender-model :chars :t] (-> e .-target .-value)))}]
     ]]])

(defn models []
  [:div.margin
   [:div.columns
    [:div.column (attacker)]
    [:div.column (attacker-weapons)]
    [:div.column (defender)]]])

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


(defn handler-fight [response]
  (swap! session assoc :graph-data (:fight (read-response response)))

  (js/Plotly.newPlot (.getElementById js/document "graph")
                     (clj->js
                      [
                       {:x ["successes" "not successes" "hits" "not hits" "wounds" "not wounds" "saves"  "not saves"]
                        :y [(-> @session :graph-data :success)
                            (-> @session :graph-data :not-success)
                            (-> @session :graph-data :hits)
                            (-> @session :graph-data :not-hits)
                            (-> @session :graph-data :wounds)
                            (-> @session :graph-data :not-wounds)
                            (-> @session :graph-data :saves)
                            (-> @session :graph-data :not-saves)


                            ]
                        :marker
                        {:color ["rgba(204,204,204,1)"
                                 "rgba(204,204,204,1)"
                                 "rgba(204,204,204,1)"
                                 "rgba(204,204,204,1)"
                                 "rgba(204,204,204,1)"
                                 "rgba(204,204,204,1)"
                                 "rgba(204,204,204,1)"
                                 "rgba(204,204,204,1)"
                                 ]

                        }
                        :type "bar"}
                       ]))


  )


(defn home-page []
  [:section.section>div.container>div.content
   (title)
   (if (:show-upload-files @session)
     (upload-rosters)
     (do

       (when (and (empty? (:attacker-unit-models @session)) (empty? (:defender-unit-models @session)))
         (init-models! (:attacker-roster @session) :attacker-unit-models)
         (init-models! (:defender-roster @session) :defender-unit-models)
         (init-weapons!))
       [:div
        [:div.columns {:key "dropdowns"}
         [:div.column (dropdown-attacker-units)]
         [:div.column (dropdown-attacker-weapons)]
         [:div.column (dropdown-defender-units)]
         ]

        (models)


        [:div.columns {:key "swap"}
         [:div.column
          [:button.button.is-dark.is-medium
           {:name     "Swap"
            :on-click (fn [e]
                        (.preventDefault e)

                        (println "swap!")
                        (let [attacker (-> @session :attacker-model)
                              weapons  (-> @session :defender-model :weapons)
                              defender (:defender-model @session)
                              attacker-unit-models (-> @session :attacker-unit-models)]
                          ;;(init-models! (:defender-roster @session) :attacker-unit-models)
                          ;;(init-models! (:attacker-roster @session) :defender-unit-models)
                          ;;(init-weapons!)
                          (swap! session assoc :attacker-unit-models (-> @session :defender-unit-models))
                          (swap! session assoc :defender-unit-models attacker-unit-models)
                          (swap! session assoc :attacker-model defender)
                          (swap! session assoc :attacker-model defender)
                          (swap! session assoc :attacker-weapons weapons)
                          (swap! session assoc :defender-model attacker)
                          (set-weapon! "0"))

                        )} "Swap Attacker defender"]]]

        [:div.columns {:key "re-rolls"}
         [:div.column
          (dropdown "Re-roll shots"
                    [{:id "1" :value "none"}
                     {:id "2" :value "1s"}
                     {:id "3" :value "all"}] (fn [e] ()))]
         [:div.column
          (dropdown "Re-roll wounds"
                    [{:id "1" :value "none"}
                     {:id "2" :value "1s"}
                     {:id "3" :value "all"}] (fn [e] ()))]
         [:div.column
          (dropdown "Additional rules"
                    [{:id "1" :value "none"}
                     {:id "2" :value "disgusting resilience"}
                     {:id "3" :value "quantum shielding"}
                     {:id "4" :value "ignore wounds on 5's"}] (fn [e] ()))]
         [:div.column
          (dropdown "Runs experiments"
                    [{:id "1" :value "100"}
                     {:id "2" :value "1000"}] (fn [e] ()))]]




        [:div.columns {:key "fight"}
         [:div.column
          [:button.button.is-dark.is-medium
           {:name     "fight"
            :on-click (fn [e]
                        (.preventDefault e)
                        (println "click")
                        (println (:attacker-model @session))
                        (println (:defender-model @session))
                        (POST "/api/fight" {:params  {:attacker (:attacker-model @session)
                                                      :defender (:defender-model @session)
                                                      :n        (:number-experiments @session)}
                                            :handler handler-fight})




                        )} "Fight"]]]]))



   ;;(str (-> @session :graph-data))
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


(swap! session update :restart #(complement %))
