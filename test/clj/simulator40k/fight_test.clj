(ns simulator40k.fight-test
  (:require [simulator40k.fight :as sut]
            [simulator40k.parse :as parse]
            [simulator40k.data-test :as data-test]
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
  (is (= 3 (sut/strength data-test/lasgun))))

(deftest toughness
  (is (= 3 (sut/toughness data-test/guardsmen))))

(deftest success?
  (is (= true (sut/success? 6 5)))
  (is (= false (sut/success? 4 5))))

(deftest to-wound
  (is 5 (= (sut/to-wound lasgun data-test/intercessor-seargent))))

(deftest wound?
  (with-redefs [sut/roll (fn [_] 5)]
    (is true (sut/wound? lasgun data-test/intercessor-seargent))))


(deftest to-save
  (is 5 (= (sut/to-save data-test/intercessor-seargent lasgun))))

(deftest save?
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= true (sut/save? lasgun data-test/intercessor-seargent))))
  (with-redefs [sut/roll (fn [_] 1)]
    (is (= false (sut/save? lasgun data-test/intercessor-seargent)))))

(deftest hit?
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= true (sut/hit? data-test/seargent)))))


(deftest all-models-hit
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= (repeat 9 {:hit true}) (sut/all-models-hit data-test/guardsmen lasgun)))
    ))


(deftest all-hits-wound
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= (repeat 9 {:hit true :wound true}) (sut/all-hits-wound (repeat 9 {:hit true}) lasgun data-test/intercessor-seargent)))
    (is (= (conj (repeat 9 {:hit true :wound true}) {:hit false :wound false} ) (sut/all-hits-wound (conj (repeat 9 {:hit true}) {:hit false}) lasgun data-test/intercessor-seargent)))))


(deftest all-wounds-save
  (with-redefs [sut/roll (fn [_] 5)]
    (is (= (repeat 9 {:wound true :saved true}) (sut/all-wounds-save (repeat 9 {:wound true}) lasgun data-test/intercessor-seargent)))
    (is (= (conj (repeat 9 {:wound true :saved true}) {:wound false :saved false}) (sut/all-wounds-save (conj (repeat 9 {:wound true}) {:wound false}) lasgun data-test/intercessor-seargent)))))


(deftest all-shot
  (is (= 9 (count (sut/all-shoot data-test/guardsmen data-test/intercessor-seargent lasgun))))
  (is (= (repeat 9 1) (map :damage (sut/all-shoot data-test/guardsmen data-test/intercessor-seargent lasgun))))
  (is (= (repeat 9 1) (map :damage (sut/all-shoot data-test/guardsmen data-test/intercessor-seargent lasgun))))

  )


(run-tests)
