(ns simulator40k.home
  (:require
   [simulator40k.state :as state]
   [ajax.core :refer [GET POST]]))

(defn title []
  [:div.columns
   [:div.column
    [:div.title
     [:a {:href     "/#"
          :on-click (fn [_]
                      (reset! state/session state/empty-state))}
      [:h6 "Simulator 40k"]]]]])

(defn file-roaster [role]
  [:div.field
   [:label.label (str (name role) " roaster:")]
   [:div.file
    [:label.file-label.full-width
     [:input.file-input {:id        (name role)
                         :name      "resume", :type "file"
                         :on-change (fn [e]
                                      (swap! state/session assoc-in [:files role] (-> e .-target .-value)))}]
     [:span.file-cta.full-width
      [:span.file-icon [:i.fas.fa-upload]]
      [:span.file-label "\n        Choose a file…\n      "]]
     [:span.file-name (role (:files @state/session))]]]])

(defn handler-upload [response]
  (swap! state/session assoc :attacker-roster (:attacker-roster response))
  (swap! state/session assoc :defender-roster (:defender-roster response)))

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
                 (swap! state/session assoc :show-upload-files false))} "Upload"]] )
