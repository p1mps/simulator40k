(ns simulator40k.state
  (:require
   [reagent.core :as r]))

(def number-experiments "100")

(def empty-state
  {:show-table              false
   :fight-error             false
   :show-loader-fight       false
   :show-loader-uploader    false
   :hit-rule                nil
   :wound-rule              nil
   :number-shot-rule        nil
   :ap-rule                 nil
   :damage-rule             nil
   :runs                    number-experiments
   :restart                 false
   :error-upload            false
   :attacker-roster         nil
   :defender-roster         nil
   :number-experiments      number-experiments
   :defender-unit-models    nil
   :attacker-unit-models    nil
   :attacker-model          nil
   :defender-model          nil
   :graph-data              nil
   :attacker-weapons        nil
   :attacker-weapon-selected nil
   :page                    :home
   :show-upload-files       true
   :show-models             false
   :show-graph              false
   :files                   {:Attacker nil
                             :Defender nil}})

(def runs-experiments
  [{:id "0" :value "10"}
   {:id "1" :value "100"}
   {:id "2" :value "1000"}
   {:id "3" :value "10000"}
   {:id "4" :value "100000"}])

(def session (r/atom empty-state))

(def hit-rules
  [{:id "0" :value "None"}
   {:id "1" :value "Re-roll 1s"}
   {:id "2" :value "Re-roll all"}
   {:id "3" :value "Exploding 6's"}
   {:id "4" :value "Auto hit"}])

(def wound-rules
  [{:id "0" :value "None"}
   {:id "1" :value "Re-roll 1s"}
   {:id "2" :value "Re-roll all"}
   ;; {:id "3" :value "Mortal wounds on 6's"}
   ;; {:id "4" :value "+1 damage on 6's"}
   ;; {:id "5" :value "Auto wound on 6's"}
   ;; {:id "6" :value "Ignore Fnp"}
   ;; {:id "7" :value "FNP 5+"}
   ;; {:id "8" :value "FNP 6+"}
   ;; {:id "9" :value "Always wound on 2's"}
   ]

  )

(def damage-rules
  [{:id "0" :value "None"}
   {:id "1" :value "Double damage on 6's"}
   {:id "2" :value "First wound 0 damage"}])

(def number-shot-rules
  [{:id "0" :value "None"}
   {:id "1" :value "Re roll only 1"}
   {:id "2" :value "Re roll all"}])

(def ap-rules
  [{:id "1" :value "None"}
   {:id "2" :value "Increase AP on 6's"}
   {:id "3" :value "quantum shielding"}])
