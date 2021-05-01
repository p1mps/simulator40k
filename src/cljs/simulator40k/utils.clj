(ns simulator40k.utils)

(defn add-react-key [coll]
  (loop [coll   coll
         result []
         i      0]
    (if (seq coll)
      (recur
       (rest coll)
       (conj result (with-meta (first coll) {:key i}))
       (inc i))
      result)))

(defn generate-graph-data []
  (take 100 (repeatedly rand)))
