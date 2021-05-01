(ns simulator40k.fight-test
  (:require [simulator40k.fight :as sut]
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


(deftest test-dice
  (is d1 (= 6 (sut/parse-dice "1")))
  (is d6 (= 6 (sut/parse-dice "D6")))
  (is d6+1 (= 6 (sut/parse-dice "D6+1")))
  (is twod6+2 (= 6 (sut/parse-dice "2D6+2"))))
