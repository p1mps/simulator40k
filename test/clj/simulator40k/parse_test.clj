(ns simulator40k.parse-test
  (:require [simulator40k.parse :as sut]
            [clojure.test :as t]))

(def units (sut/parse "spacemarines.rosz"))
