(ns simulator40k.fight-test
  (:require [simulator40k.fight :as sut]
            [simulator40k.parse :as parse]
            [clojure.test :refer :all]))

(def d1
  {:times 1
   :dice 1
   :add 0})

(def d6
  {:times 1
   :dice 6
   :add 0})

(def d6+1
  {:times 1
   :dice 6
   :add 1})

(def twod6
  {:times 2
   :dice 6
   :add 0})

(def twod6+2
  {:times 2
   :dice 6
   :add 2})

(def intercessors
  (->
   (parse/parse "rosters/spacemarines.rosz")
   first
   :units))


(def infantry-squad
  (->
   (parse/parse "rosters/guard.rosz")
   first
   :units))

(def guardsman
  (-> infantry-squad
      first
      :models
      first))

(def lasgun
  (-> guardsman
      :weapons
      first))


(deftest parse-dice
  (is d1 (= 6 (sut/parse-dice "1")))
  (is d6 (= 6 (sut/parse-dice "D6")))
  (is d6+1 (= 6 (sut/parse-dice "D6+1")))
  (is twod6+2 (= 6 (sut/parse-dice "2D6+2"))))


(deftest roll
  (with-redefs [rand-nth (fn [_] 1)]
    (is (= 1 (sut/roll 1))))
  (with-redefs [rand-nth (fn [_] 5)]
    (is (= 5 (sut/roll 6))))
  (is (= 0 (sut/roll 7)))
  (is (= 0 (sut/roll 0))))


(deftest roll-dice
  (with-redefs [rand-nth (fn [_] 1)]
    (is (= 1 (sut/roll-dice "1")))
    (is (= 1 (sut/roll-dice "D6")))
    (is (= 3 (sut/roll-dice "D6+2")))
    (is (= 2 (sut/roll-dice "2D6")))
    (is (= 4 (sut/roll-dice "2D6+2")))))


(deftest bs
  (is (= 4 (sut/bs guardsman))))

(deftest strength
  (is (= 3 (sut/strength lasgun))))

(deftest toughness
  (is (= 3 (sut/toughness guardsman))))

(deftest success?
  (is (= true (sut/success? 6 5)))
  (is (= false (sut/success? 4 5))))

(run-tests)
