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
   :units
   first))


(def infantry-squad
  (->
   (parse/parse "rosters/guard.rosz")
   first
   :units
   first))

(def guardsmen
  (-> infantry-squad
      :models
      first
      ))

(def sergent
  (-> infantry-squad
      :models
      second
      ))



(def intercessor
  (-> intercessors
      :models
      first))

(def lasgun
  (-> guardsmen
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
  (is (= 4 (sut/read-bs "4+"))))

(deftest strength
  (is (= 3 (sut/strength lasgun))))

(deftest toughness
  (is (= 3 (sut/toughness guardsmen))))

(deftest success?
  (is (= true (sut/success? 6 5)))
  (is (= false (sut/success? 4 5))))

(deftest to-wound
  (is 5 (= (sut/to-wound lasgun intercessor))))

(deftest wound?
  (with-redefs [sut/roll (fn [_] 5)]
    (is true (sut/wound? lasgun intercessor))))


(deftest to-save
  (is 5 (= (sut/to-save intercessor lasgun))))

(deftest save?
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= true (sut/save? lasgun intercessor))))
  (with-redefs [sut/roll (fn [_] 1)]
    (is (= false (sut/save? lasgun intercessor)))))

(deftest hit?
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= true (sut/hit? sergent)))))


(deftest all-models-hit
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= (repeat 9 {:hit true}) (sut/all-models-hit guardsmen)))
    ))


(deftest all-hits-wound
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= (repeat 9 {:hit true :wound true}) (sut/all-hits-wound (repeat 9 {:hit true}) lasgun intercessor)))
    (is (= (conj (repeat 9 {:hit true :wound true}) {:hit false :wound false} ) (sut/all-hits-wound (conj (repeat 9 {:hit true}) {:hit false}) lasgun intercessor)))))


(deftest all-wounds-save
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= (repeat 9 {:wound true :saved true}) (sut/all-wounds-save (repeat 9 {:wound true}) lasgun intercessor)))
    (is (= (conj (repeat 9 {:wound true :saved true}) {:wound false :saved false}) (sut/all-wounds-save (conj (repeat 9 {:wound true}) {:wound false}) lasgun intercessor)))))


(deftest all-shot
  (is (= 9 (count (sut/all-shoot guardsmen intercessor lasgun)))))


(run-tests)
