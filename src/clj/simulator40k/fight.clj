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
    (< dice 0) 0
    (and (>= dice 1) (<= dice 6))


    (rand-nth (range 1 (+ 1 dice)))

    ))

(defn roll-dice [dice]
  (if (or (string/includes? dice "D") (= "1" dice))
    (let [parsed-dice (parse-dice dice)
          add         (:add parsed-dice)
          roll        (reduce + (take
                                 (:times parsed-dice)
                                 (repeatedly (partial roll (:dice parsed-dice)))))]


      (+ add roll))
    (read-string dice)))


(defn bs [unit]
  (read-string (string/replace (:bs (:chars unit)) "+" "")))

(defn strength [weapon]
  (read-string (:s (:chars weapon))))


(defn toughness [unit]
  (read-string (:t (:chars unit))))


(defn success? [rolled stat]
  (>= rolled stat))

(defn hit? [char]
  (let [r (roll 6)]
    (success? r char)))

;; TODO check double strength weapon
;; check more than double
(defn to-wound [weapon target-unit]
  (let [strength  (strength weapon)
        toughness (toughness target-unit)]
    (cond
      (>= toughness (* 2 strength))  6
      (>= strength  (* 2 toughness)) 2
      (= toughness strength)         4
      (>= (- toughness strength) 1)  5
      (<= (- toughness strength) -1) 3
      )))

(defn wound? [weapon target-unit]
  (let [r (roll 6)]



    (success? r (to-wound weapon target-unit))
    )
  )

(defn save? [armor-to-roll]
  (let [s (roll 6)]
    (if (> armor-to-roll 6)
      false
      (>= s armor-to-roll))))

(defn damage [weapon]
  (:d (:chars weapon)))


(defn valid-value [value]
  (not= "-" value))

(defn save [model ap]
  ;;(read-string (string/replace (:save (:chars model)) "+" ""))
  (if (valid-value ap)
    (-
     (read-string (string/replace (:save (:chars model)) "+" "") )
     (read-string ap))
    (read-string (string/replace (:save (:chars model)) "+" "") )))

(defn shoot-succes [model1 model2 w]
  (let [h       (hit? (bs model1))
        s       (save? (save model2 (:ap (:chars w))))
        wounded (wound? w model2)]
    (and h (not s) wounded)))

(defn calculate-wounds [model1 model2 w]
  (reduce +
            (loop [n      (roll-dice "1")
                   result []]
              (if (> n 0)
                (if (shoot-succes model1 model2 w)
                  (let [d (roll-dice (damage w))]
                    (recur (dec n) (conj result d)))
                  (recur (dec n) (conj result 0)))
                result)
              ))
  )


;; TODO: number of attacks * number of units

(defn shoot [model1 model2]
  (calculate-wounds model1 model2 (first (:weapons model1))))

(defn monte-carlo-shoot [model1 model2 n]
  (repeatedly n #(shoot model1 model2)))

(defn assoc-weapons [stats]
  (reduce (fn [result value]
            (conj result (reduce (fn [r v]
                                   (update r (first v) (comp vec concat) (second v)))
                                 {}
                                 value))

            )

          {}
          stats
          )
  )


(defn stats [m1 m2 n]

  (monte-carlo-shoot m1 m2 n)
            ;;(assoc-weapons)




  )

(comment

  (def units (:units (simulator40k.parse/parse "spacemarines.rosz")))
  (def captain (first units))

  (def squad (second units))

  (def captain-model (first (:models captain)))




  (shoot captain-model captain-model)

  (monte-carlo-shoot captain-model captain-model 1)

  (get-in captain [:chars])
  (bs captain)

  (stats captain squad)


  (stats squad squad)



  )
