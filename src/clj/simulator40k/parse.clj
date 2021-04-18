(ns simulator40k.parse
  (:require
   [clojure.data.zip.xml :as zx]
   [simulator40k.xml-select :as xml-select]
   [simulator40k.zip-reader :as zip-reader]))

;; TODO: remove Game type from parsed

(defn attrs-name [e]
    (:name (:attrs e)))

(defn attrs-and-content [e]
  [(:name (:attrs e)) (first (:content e))])

(defn content [e]
  (first (:content e)))

(defn keywordize [string]
  (keyword (clojure.string/lower-case string)))


(defn keywordize-chars [chars]
  (reduce (fn [result value]
            (let [[characteristic v]  value
                  c (keywordize characteristic)]
              (assoc result c v)))
          {}
          chars))


(defn weapons [model]
  (let [weapons (zx/xml->
                 model
                 :selections
                 :selection
                 :profiles
                 :profile
                 (zx/attr= :typeName "Weapon"))]
    (reduce (fn [result w]
              (conj result {:name (attrs-name (first w))
                            :chars (->
                                    (map #(attrs-and-content (first %))
                                         (zx/xml-> w :characteristics :characteristic))
                                    (keywordize-chars))}))

            []
            weapons)))

(defn characteristics [model]
  (let [chars (zx/xml-> model
                        :profiles
                        :profile
                        :characteristics
                        :characteristic)]
    (->
     (map #(attrs-and-content (first %)) chars)
     (keywordize-chars))))

(defn assoc-ids [units]
  (loop [u units
         id 0
         result []]
    (if (seq u)
      (recur (rest u)
             (inc id)
             (conj result
                   (assoc
                    (first u)
                    :id
                    (str id))))
      result)))

(defn get-models [force]
  (for [m (xml-select/models force)]
    {:name  (attrs-name (first m))
     :model true
     :models
     (assoc-ids (list {:name    (attrs-name (first m))
                       :number  (read-string (:number (:attrs (first m))))
                       :chars   (characteristics  m)
                       :weapons (assoc-ids (weapons m))}))}))

(defn get-units [force]
  (for [u (xml-select/units force)]
      {:name
       (attrs-name (first u))
       :unit   true
       :models (filter #(:m (:chars %))
                       (assoc-ids (for [m (concat (xml-select/unit->models-as-upgrades u)
                                                  (xml-select/unit->models u))]
                                    {:name    (attrs-name (first m))
                                     :number  (read-string (:number (:attrs (first m))))
                                     :chars   (characteristics  m)
                                     :weapons (assoc-ids (weapons m))})))}))


(defn edn [forces]
  (assoc-ids (for [f forces]
               {:force-name (attrs-name (first f))
                ;; TODO: units must be unique (use sets)
                :units      (assoc-ids (concat (get-models f) (get-units f)))})))


;; TODO: we change the data we got from battescribe
(defn find-unique-models [unit unique-models]
  (filter #(= (:unit %) unit) unique-models))

(defn remove-found-model [model models]
  (remove #(= (:name %) (:name model)) models))

(defn final-result [forces]
  (let [unique-models (distinct (flatten (for [f forces]
                                      (for [u (:units f)]
                                        (for [m (:models u)]
                                          {:unit u
                                           :model   (dissoc m :id)})))))]
    (for [f forces]
      (assoc f :units
             (for [u (:units f)]
               (assoc u :models
                      (assoc-ids
                       (map :model (find-unique-models u unique-models)))))))))


(defn file->edn [file]
  (-> file
      zip-reader/zipper
      xml-select/forces
      edn
      final-result))


(defn parse [file-rosz]
  ;; TODO: generate random xml name file
  (let [file (zip-reader/unzip-file file-rosz "output.xml")]
    (file->edn file)))

(comment

  (->

   (mapcat :units (parse "spacemarines.rosz"))
   (assoc-ids)

   )


  (unzip-file "spacemarines.rosz" "spacemarines.ros")

  (clojure.string/join "" (drop-last "hello"))

  (def file (slurp "spacemarines.ros"))

  (def zipper (zipper file))

  (def forces (forces zipper))


  (parse "spacemarines.rosz")


 ,)
