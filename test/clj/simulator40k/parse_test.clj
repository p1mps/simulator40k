(ns simulator40k.parse-test
  (:require [simulator40k.parse :as sut]
            [simulator40k.data-test :as data-test]
            [clojure.test :refer :all]))


(deftest weapon-attacks
  (is (= "1" (:weapon-attacks data-test/lasgun)))
  (is (= ["1" "D6" "1"] (map :weapon-attacks (:weapons data-test/intercessor-seargent)))))


(run-tests)
