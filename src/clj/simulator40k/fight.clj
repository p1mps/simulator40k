(ns simulator40k.fight
  (:require
   [clojure.string :as string]
   [simulator40k.stats :as stats]
   ))

(def number-experiments 100)


;; TODO set damage max to n.wounds defender

;; TODO: handle grenades (either shoot or grenade)
;; TODO: count failures

;; TODO roll d3

;; D6/3D6/3D6+2
;; nil when just "1"

;; [0 1 5 7] -> [0 1 2 3 4 5 7]
;; [1 2 3 4]    [1 2 0 0 0 3 4]
;; [0 1] [1 2] [5 3] [7 4] -> [0 1] [1 2] [2 0] [3 0] [4 0] [5 3] [6 0] [7 4] -> [0
(defn all-numbers [numbers]
  (for [i (range 0 (+ 1 (apply max numbers)))]
    i))


(defn fill-gaps [numbers]
  (reduce (fn [result value]
            (if (get result value)
              result
              (assoc result value 0)))
          (frequencies numbers)
          (all-numbers numbers)))




(defn parse-dice [dice]
  (if (string/includes? dice "D")
    (let [[times dice] (string/split dice #"D")]
      (if (string/includes? dice "+")
        (let [[dice add] (clojure.string/split dice #"\+")]
          {:times (if (seq times) (Integer/parseInt times) 1)
           :dice  (read-string dice)
           :add   (read-string add)})
        {:times (if (seq times) (Integer/parseInt times) 1)
         :dice  (read-string dice)
         :add   0}))
    {:times 1
     :dice  1
     :add   0}))


;; roll 6 3

(defn roll [dice]
  (cond
    (> dice 6) 0
    (<= dice 0) 0
    (and (>= dice 1) (<= dice 6))
    (rand-nth (range 1 (+ 1 dice)))))

(def re-rolls
  {:none       (fn [dice _]
                 ;;(println "none reroll")
                 (roll dice))
   :re-roll-1s (fn [dice _]
                 ;;(println "reroll 1s")
                 (let [previous-roll (roll dice)]
                   (if (= previous-roll 1)
                     (roll dice)
                     previous-roll)))
   :re-roll-all (fn [dice to-roll]
                  ;;(println "reroll all")
                  (let [previous-roll (roll dice)]
                   (if (< previous-roll to-roll)
                     (roll dice)
                     previous-roll))
                  )})




(comment
  ((:re-roll-1s re-rolls) 1 6)

  ((:re-roll-all re-rolls) 5 6 5))


(defn roll-dice [dice]
  (if (string/includes? dice "D")
    (let [parsed-dice (parse-dice dice)
          add         (:add parsed-dice)
          roll        (reduce + (take
                                 (:times parsed-dice)
                                 (repeatedly (partial roll (:dice parsed-dice)))))]

      (+ add roll))
    (read-string dice)))

(def attacks-rules
  {:exploding-6s (fn []
                   (let [first-roll (roll-dice 6)]
                     (if (= first-roll 6)
                       (list first-roll (roll-dice 6))
                       (list first-roll))))
   :auto-hit (fn [] true)
   })

(def fnp-rules
  {:none   (fn [wound] wound)
   :fnp-5+ (fn [wound]
                      (let [r (roll 6)]
                        (if (and wound (>= r 5))
                          false
                          wound)))
   :fnp-6+ (fn [wound]
             (let [r (roll 6)]
                        (if (and wound (>= r 6))
                          false
                          wound)))})

(defn read-bs [bs]
  (read-string (string/replace bs "+" "")))

(defn strength [{{:keys [s]} :chars}]
  (read-string s))

(defn toughness [{{:keys [t]} :chars}]
  (read-string t))

(defn success? [rolled stat]
  (>= rolled stat))

(defn exploding-hits [])

(defn get-hit-roll-fn [hit-rule bs]
  (condp = hit-rule
    nil (fn [] (roll 6))
    :re-roll-1s (fn [] (let [r (roll 6)]
                        (if (= r 1)
                          (roll 6)
                          r)))
    :re-roll-all (fn [] (let [r (roll 6)]
                        (if (< r bs)
                          (roll 6)
                          r)))))

(defn re-roll-dice? [roll _]
  (< roll 3))

(parse-dice "2D6")


(defn get-rerollable-shots [rolls]
  (keep-indexed (fn [idx v] (when (< v 3) idx)) rolls))

(defn all-rerolls [rolls idx-re-rollable dice]
  (reduce (fn [result value]
            (assoc result value (roll dice))
            )
          rolls
          idx-re-rollable))

(defn get-number-shots-fn [number-shot-rule number-attacks]
  (condp = number-shot-rule
    nil (fn [] [(roll-dice number-attacks)])
    :re-roll-1s (fn [] (let [parsed-dice (parse-dice number-attacks)
                            _ (println parsed-dice)
                            rolls       (vec (repeatedly (:times parsed-dice) #(roll (:dice parsed-dice))))
                            _ (println rolls)
                            re-rollable (get-rerollable-shots rolls)
                            _ (println "rerollable " re-rollable)]
                        (if (not-empty re-rollable)
                          (assoc rolls (first re-rollable) (roll (:dice parsed-dice)))
                          rolls)))
    :re-roll-all (fn [] (let [parsed-dice (parse-dice number-attacks)
                            _ (println parsed-dice)
                            rolls       (vec (repeatedly (:times parsed-dice) #(roll (:dice parsed-dice))))
                            _ (println rolls)
                            re-rollable (get-rerollable-shots rolls)
                            _ (println "rerollable all" re-rollable)]
                        (if (not-empty re-rollable)
                          (all-rerolls rolls re-rollable (:dice parsed-dice)
                           )
                          rolls)))))


((get-number-shots-fn :re-roll-1s "2D6"))


(defn hit? [{{:keys [bs]} :chars} hit-rule n-attacks]
  ;;(println "type of re-rolls applied to hit" hit-rules)
  (if (= hit-rule :auto-hit)
    {:n-attacks n-attacks
     :hit true
     :roll-hit 6}
    (let [r ((get-hit-roll-fn hit-rule (read-bs bs)))]
      {:hit  (success? r (read-bs bs))
       :n-attacks n-attacks
       :roll-hit r}

      )))

;; TODO check double strength weapon
;; check more than double
(defn to-wound [weapon target-model]
  (let [strength  (strength weapon)
        toughness (toughness target-model)]
    (cond
      (>= toughness (* 2 strength))  6
      (>= strength  (* 2 toughness)) 2
      (= toughness strength)         4
      (>= (- toughness strength) 1)  5
      (<= (- toughness strength) -1) 3)))

(defn wound? [weapon target-model {:keys [wound-rule]}]
  (let [to-wound (to-wound weapon target-model)
        r ((get-hit-roll-fn wound-rule to-wound))]
    {:roll-wound r
     :wound (success? r to-wound)
     :to-wound to-wound}

    ))

(defn valid-value [value]
  (not= "-" value))

(defn to-save [{{:keys [save]} :chars} {{:keys [ap]} :chars}]
  ;;(read-string (string/replace (:save (:chars model)) "+" ""))
  (let [save (read-string (string/replace save "+" ""))]
    (if (valid-value ap)
      (-
       save
       (read-string ap))
      save)))


(defn save? [weapon target-model]
  (let [to-roll (to-save target-model weapon)
        r (roll 6)]
    {:roll-save r
     :to-save to-roll
     :saved (success? r to-roll)}
    ))

(defn damage [{{:keys [d]} :chars}]
  d)

(defn all-models-hit [model weapon {:keys [hit-rule number-shot-rule]}]
  (let [n-attacks (* (reduce + ((get-number-shots-fn number-shot-rule (:weapon-attacks weapon)))) (read-string (:number model)))
        rolls (repeatedly n-attacks  #(hit? model hit-rule n-attacks))]
    (if (= hit-rule :exploding-6s)
      (let [n6s (count (filter #{6} (map :roll rolls)))]
        (concat rolls (repeat n6s {:hit  true
                             :roll 6})))
      rolls)))

(defn all-hits-wound [hits weapon target-unit rules]
  (for [h hits]
    (merge h (wound? weapon target-unit rules))
    ))

(defn all-wounds-save [wounds weapon target]
  (for [w wounds]
    (merge w (save? weapon target))
    ))

(defn all-damage [shoots weapon]
  (map #(assoc % :damage
               (if (:success %)
                 (roll-dice (damage weapon))
                 0) ) shoots))

(defn all-success [results]
  (map #(assoc % :success
               (if (and (:hit %) (not (:saved %)) (:wound %))
                 true
                 false)) results))

(defn print-list [l]
  (println l)
  l)

(defn all-shoot [shooter-model target weapon rules]
  (-> (all-models-hit shooter-model weapon rules)
      ;;(print-list)
      (all-hits-wound weapon target rules)
      ;;(print-list)
      (all-wounds-save weapon target)
      ;;(print-list)
      (all-success)
      ;;(print-list)
      (all-damage weapon)
      ;;(print-list)
      )
  )

(defn model-weapon [model]
  (first (:weapons model)))

;; (defn total-damage [shots]
;;   (reduce (fn [result value]
;;             (+ result (reduce
;;                        + (:d value))))
;;           0
;;           shots))

;; TODO: number of attacks * number of units
;; number of attacks still not fixed


(defn monte-carlo-shoot [attacker defender n rules]
  (repeatedly n
              #(all-shoot attacker defender (model-weapon attacker) rules)))

(defn average
  [numbers]
  (if (empty? numbers)
    0
    (/ (reduce + numbers) (count numbers))))

(defn avg [n total]
  (/ n total))

(defn percentage [total number]
  (* 100 (/ number total)))

(defn total-damage [experiments]
  (reduce + (map :damage experiments))
  )

(defn total-success [experiments success]
  (reduce + (map :total-success
                 (reduce (fn [result experiment]
                           (conj
                            result {:total-success
                                    (count (filter #(= (:success %) success)
                                                   experiment))}))

                         []
                         experiments))))

(defn total-wounds [experiments wounded]
  (reduce + (map :total-wounds
                 (reduce (fn [result experiment]
                           (conj
                            result {:total-wounds
                                    (count (filter #(= (:wound %) wounded)
                                                   experiment))}))

                         []
                         experiments))))

(defn total-hits [experiments hit]
  (reduce + (map :total-hits
                 (reduce (fn [result experiment]
                           (conj
                            result {:total-hits
                                    (count (filter #(= (:hit %) hit)
                                                   experiment))}))

                         []
                         experiments))))

(defn total-saves [experiments saved]
  (reduce + (map :total-saves
                 (reduce (fn [result experiment]
                           (conj
                            result {:total-saves
                                    (count (filter #(= (:saved %) saved)
                                                   experiment))}))

                         []
                         experiments))))

;; y = (x**lmbda - 1) / lmbda,  for lmbda != 0
;;     log(x),                  for lmbda = 0

(defn normalize [values]
  (map #(Math/sqrt %

          )


       values ))

(defn mode [data]
  (first (last (sort-by second (frequencies data)))))

(defn only-success [e]
  (filter #(= (:success %))) e)

(defn get-damage [experiments]
  (sort (map #(reduce + (map :damage %))
             experiments)))

(defn quantile [q xs]
  (let [n (dec (count xs))
        i (-> (* n q)
              (+ 1/2)
              (int))]
    (nth (sort xs) i)))
(def latest-experiments (atom nil))


(defn get-exp [rolls k]
  (map k rolls))

(last
 (sort (->>
        (map

         #(get-exp % :damage)
         @latest-experiments

         )
        (map #(reduce + %)))))


(->> (map (partial sort-by :damage) @latest-experiments)
     (map last)
     (map :damage))

(map

             #(get-exp % :n-attacks)
             @latest-experiments

             )


(defn roll-keys [experiment]
  (select-keys experiment [:roll-hit :roll-save :roll-wound]))


(defn get-rolls [experiments]
  (let [all-rolls
        (mapcat
         #(map roll-keys %)  experiments)]
    (concat (map :roll-hit all-rolls)
            (map :roll-wound all-rolls)
            (map :roll-save all-rolls))))


(defn compute-stats [experiments]
  (reset! latest-experiments experiments)
  (let [damage (sort (get-damage experiments))]
    {:experiments      experiments
     :damage-stats     (stats/stats-map damage)
     :damage           damage
     :rolls            (frequencies (get-rolls experiments))
     ;; :success
     ;; (total-success experiments true)

     ;; :not-success
     ;; (total-success experiments false)

     ;; :wounds
     ;; (total-wounds experiments true)

     ;; :not-wounds
     ;; (total-wounds experiments false)

     :percentage-success (format "%.2f"
                                 (float (percentage
                                         (reduce +
                                                 (map count experiments))
                                         (total-success experiments true)
                                         )))


     :percentage-hit (format "%.2f"
                             (float (percentage
                                     (reduce +
                                             (map count experiments))

                                     (total-hits experiments true)
                                     )))

     :percentage-wound (format "%.2f"
                               (float (percentage
                                       (reduce +
                                               (map count experiments))

                                       (total-wounds experiments true)
                                       )))

     :percentage-not-save (format "%.2f"
                                  (float (percentage
                                          (reduce +
                                                  (map count experiments))

                                          (total-saves experiments false)
                                          )))

     ;; :hits
     ;; (total-hits experiments true)

     ;; :not-hits
     ;; (total-hits experiments false)

     ;; :saves
     ;; (total-saves experiments true)

     ;; :not-saves
     ;; (total-saves experiments false)

     }

    ))



;; 100:x = total:number
;; 100*number/total


(defn stats [{:keys [attacker defender n rules]}]
  (println "running " n "simulations")
  (println rules)
  (-> (monte-carlo-shoot attacker defender (read-string n) rules)
      (compute-stats)))

(comment

  (def units (:units (simulator40k.parse/parse "spacemarines.rosz")))
  (def captain (first units))

  (def squad (second units))

  (def captain-model (first (:models captain)))

  (simulator40k.parse/parse "Death riders 2000.rosz"))
