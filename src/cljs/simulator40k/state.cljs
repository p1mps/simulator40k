(ns simulator40k.state
  (:require
   [reagent.core :as r]))

(def number-experiments "100")

(def empty-state
  {:hit-rules               []
   :wound-rules             []
   :ap-rules                []
   :damage-rules            []
   :runs                    number-experiments
   :restart                 false
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

(def runs-experiments
  [{:id "1" :value "100"}
   {:id "2" :value "1000"}])

(def session (r/atom empty-state))

(def hit-rules
  [{:id "0" :value "None"}
   {:id "1" :value "Re-roll 1s"}
   {:id "2" :value "Re-roll all"}
   {:id "3" :value "Exploding 6's"}
   {:id "4" :value "Auto wound on 6's"}
   {:id "5" :value "Auto hit"}
   {:id "6" :value "Extra hit on 6's"}])

(def wound-rules
  [{:id "0" :value "None"}
   {:id "1" :value "Re-roll 1s"}
   {:id "2" :value "Re-roll all"}
   {:id "3" :value "Mortal wounds on 6's"}
   {:id "3" :value "+1 damage on 6's"}
   {:id "4" :value "Ignore Fnp"}
   {:id "5" :value "Ignore wounds on 5's"}
   {:id "5" :value "Always wound on 2's"}])

(def damage-rules
  [{:id "0" :value "None"}
   {:id "1" :value "Double damage on 6's"}
   {:id "2" :value "First wound 0 damage"}])

(def ap-rules
  [{:id "1" :value "none"}
   {:id "2" :value "Increase AP on 6's"}
   {:id "6" :value "quantum shielding"}])
