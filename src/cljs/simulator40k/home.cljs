(ns simulator40k.home
  (:require
   [clojure.string :as string]
   [simulator40k.state :as state]
   [clojure.browser.dom :as dom]
   [ajax.core :refer [GET POST]]
   [reagent.core :as r]))

(defn title []
  [:div.columns
   [:div.column
    [:div.title
     [:a {:href     "/#"
          :on-click (fn [_]
                      (dom/remove-children "graph")
                      (dom/remove-children "graph-damage")
                      (dom/remove-children "graph-rolls")

                      (reset! state/session state/empty-state))}
     [:h6 "Simulator 40k"]]]]])

(defn file-roaster [role]
  [:div
   [:div.field
    [:label.label (str (name role) " roster: (rosz battlescribe file)")]
    [:div.file
     [:label.file-label.full-width
      [:input.file-input {:id        (name role)
                          :name      "resume", :type "file"
                          :on-change (fn [e]
                                       (swap! state/session assoc-in [:files role] (-> e .-target .-value)))}]
      [:span.file-cta.full-width
       [:span.file-icon [:i.fas.fa-upload]]
       [:span.file-label "\n        Choose a file…\n      "]]
      [:span.file-name.full-width (last (string/split (role (:files @state/session)) "\\"))]]]]

   ])


(defn handler-upload [response]
  (swap! state/session assoc :attacker-roster (:attacker-roster response))
  (swap! state/session assoc :defender-roster (:defender-roster response))
  (swap! state/session assoc :error-upload false)
  (swap! state/session assoc :show-upload-files false)
  (swap! state/session assoc :error-upload false)
  (swap! state/session assoc :show-loader-uploader false)
  )


(defn error-handler [response]
  (swap! state/session assoc :error-upload true)
  )


(defn upload-rosters []
  [:div [:article.message.is-warning {:id "message"}
         [:div.message-header
          [:p "Warning"]
          [:button.delete {:aria-label "delete"
                           :on-click (fn [e]
                                       (.preventDefault e)
                                       (dom/remove-children "message"))}]]
         [:div.message-body
          "Please remove from your rosters all the stratagems, detachment costs etc. Leave just the "
          [:strong "units."]]]
   [:form
    [:div.columns
     [:div.column
      (conj (file-roaster :Attacker)
            (file-roaster :Defender))
      [:div.column [:button.button.is-dark
                    {:name     "Upload"
                     :on-click (fn [e]
                                 (.preventDefault e)
                                 (swap! state/session assoc :show-loader-uploader true)
                                 (let [file-attacker (aget (.-files
                                                            (.getElementById js/document "Attacker")) 0)
                                       file-defender (aget (.-files
                                                            (.getElementById js/document "Defender")) 0)
                                       form-data     (doto
                                                         (js/FormData.)
                                                       (.append "Attacker" file-attacker)
                                                       (.append "Defender" file-defender))]
                                   (POST "/api/parse" {:body          form-data
                                                       :handler       handler-upload
                                                       :error-handler error-handler}))
                                 )} "Upload"]]

      (when (:show-loader-uploader @state/session)
        [:div.column [:div.loader]])
      (when (:error-upload @state/session)
        [:div.column [:div.has-background-danger-light "ERROR UPLOAD"]])]]]])
