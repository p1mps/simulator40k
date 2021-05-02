(ns simulator40k.fight
  (:require
   [clojure.string :as string]))

(def number-experiments 100)


;; TODO: handle grenades (either shoot or grenade)
;; TODO: count failures

;; TODO roll d3

;; D6/3D6/3D6+2
;; nil when just "1"


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

(defn roll-dice [dice]
  (if (string/includes? dice "D")
    (let [parsed-dice (parse-dice dice)
          add         (:add parsed-dice)
          roll        (reduce + (take
                                 (:times parsed-dice)
                                 (repeatedly (partial roll (:dice parsed-dice)))))]

      (+ add roll))
    (read-string dice)))

(defn read-bs [bs]
  (read-string (string/replace bs "+" "")))

(defn strength [{{:keys [s]} :chars}]
  (read-string s))

(defn toughness [{{:keys [t]} :chars}]
  (read-string t))

(defn success? [rolled stat]
  (>= rolled stat))

(defn hit? [{{:keys [bs]} :chars}]
  (let [r (roll 6)]
    (success? r (read-bs bs))))

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

(defn wound? [weapon target-model]
  (let [r (roll 6)]
    (success? r (to-wound weapon target-model))))

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
    (>= r to-roll)))

(defn damage [{{:keys [d]} :chars}]
  (read-string d))

(defn all-models-hit [model]
  (repeat (:number model)  {:hit (hit? model)}))

(defn all-hits-wound [hits weapon target-unit]
  (for [h hits]
    (if (:hit h)
      (assoc h :wound (wound? weapon target-unit))
      (assoc h :wound false))))

(defn all-wounds-save [wounds weapon target]
  (for [w wounds]
    (if (:wound w)
      (assoc w :saved (save? weapon target))
      (assoc w :saved false))))

(defn all-success [results]
  (map #(assoc % :success (and (:hit %) (not (:saved %)) (:wound %))) results))


(defn shoot [model1 model2 w]
  (let [h       (hit? model1)
        s       (save? model2 w)
        wounded (wound? w model2)
        success (and h (not s) wounded)
        result {:hit     h
                :saved   (if (and h wounded s)
                           true
                           false)

                :success success
                :wounded (if h
                           wounded
                           false)
                :damage (if success
                          (roll-dice (damage w))
                          0)}]
    result))

(defn all-shoot [shooter-model target weapon]
  (-> (all-models-hit shooter-model)
      (all-hits-wound weapon target)
      (all-wounds-save weapon target)
      (all-success))
  )

(defn model-weapon [model]
  (first (:weapons model)))

;; (defn total-damage [shots]
;;   (reduce (fn [result value]
;;             (+ result (reduce
;;                        + (:d value))))
;;           0
;;           shots))

(defn calculate-wounds [model1 model2 w]
  (loop [n      (roll-dice (:number model1))
         result []]
    (if (> n 0)
      (recur (dec n) (conj result (shoot model1 model2 w)))
      result)))


;; TODO: number of attacks * number of units
;; number of attacks still not fixed


(defn monte-carlo-shoot [attacker defender n]
  (repeatedly n
              #(calculate-wounds attacker defender (model-weapon attacker))))

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
  (map :total-damage
       (reduce (fn [result experiment]
                 (conj
                  result {:total-damage
                          (reduce + (map :damage experiment))}))
               []
               experiments)))

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
                                    (count (filter #(= (:wounded %) wounded)
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

(defn compute-stats [experiments]
  {:experiments experiments
   :avg-damage
   (/ (float (reduce + (total-damage experiments)))
      (count experiments))

   :success
   (total-success experiments true)

   :not-success
   (total-success experiments false)

   :wounds
   (total-wounds experiments true)

   :not-wounds
   (total-wounds experiments false)

   :max-damage
   (apply max (total-damage experiments))

   :min-damage
   (apply min (total-damage experiments))

   :percentage-success (format "%.2f"
                               (float (percentage
                                       (+ (total-success experiments true)
                                          (total-success experiments false))
                                       (total-success experiments true))))

   :hits
   (total-hits experiments true)

   :not-hits
   (total-hits experiments false)

   :saves
   (total-saves experiments true)

   :not-saves
   (total-saves experiments false)})



;; 100:x = total:number
;; 100*number/total


(defn stats [{:keys [attacker defender n]}]
  (-> (monte-carlo-shoot attacker defender (read-string n))
      (compute-stats)))

(comment

  (def units (:units (simulator40k.parse/parse "spacemarines.rosz")))
  (def captain (first units))

  (def squad (second units))

  (def captain-model (first (:models captain)))

  (simulator40k.parse/parse "Death riders 2000.rosz")

  (shoot captain-model captain-model)

  (monte-carlo-shoot captain-model captain-model 1)

  (get-in captain [:chars])
  (bs captain)

  (stats captain squad)

  (stats squad squad))
