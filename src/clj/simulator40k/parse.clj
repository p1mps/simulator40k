(ns simulator40k.parse
  (:require
   [clojure.data.zip.xml :as zx]
   [simulator40k.xml-select :as xml-select]
   [simulator40k.zip-reader :as zip-reader]

   [clojure.xml :as xml]
   [clojure.string :as string]))

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
                                    (keywordize-chars))
                            ;; TODO: FIX THIS
                            }))

            []
            weapons)
    ))

(defn unit-weapons [unit]
  (let [weapons (xml-select/unit->weapons unit)]
    (reduce (fn [result w]
              (conj result {:name (attrs-name (first w))
                            :chars (->
                                    (map #(attrs-and-content (first %))
                                         (zx/xml-> w :characteristics :characteristic))
                                    (keywordize-chars))
                            ;; TODO: FIX THIS
                            }))

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

(defn assoc-weapon-attacks [weapons]
  ;; (map #(cond
  ;;         (= "Melee" (-> % :chars :type)) (assoc % :weapon-attacks (:a (:chars %)))
  ;;         (or
  ;;          (clojure.string/includes? (-> % :chars :type) "Grenade")
  ;;          (clojure.string/includes? (-> % :chars :type) "Heavy")
  ;;          (clojure.string/includes? (-> % :chars :type) "Pistol"))
  ;;         (assoc % :weapon-attacks (-> (clojure.string/replace (-> % :chars :type) #"Grenade|Heavy|Pistol" "")
  ;;                                      (clojure.string/replace #"\s+" ""
  ;;                                       )))
  ;;         :else
  ;;         (assoc % :weapon-attacks "1")

  ;;         ) weapons)


  (map #(assoc % :weapon-attacks (-> % :chars :type)) weapons)


  )

(defn remove-battle-size [models]
  (remove #(or (string/includes? (:name %) "Detachment")
               (string/includes? (:name %) "Stat")
               (string/includes? (:name %) "Imperial")
               (string/includes? (:name %) "Chapter")
               (string/includes? (:name %) "Doctrine")
               (string/includes? (:name %) "Size")) models))


(defn models-models [force]
  (distinct (for [m (concat (xml-select/models force) (xml-select/models-upgrades force))]
              {:name (attrs-name (first m))
               :models
               (remove-battle-size (list {:name    (attrs-name (first m))
                                          :number  (:number (:attrs (first m)))
                                          :chars   (characteristics  m)
                                          :weapons (assoc-weapon-attacks
                                                    (assoc-ids (concat (unit-weapons m) (weapons m))))}))})))





(defn unit-models [u]
  (set (for [m (concat
                (xml-select/unit-upgrade->model  u)
                (xml-select/unit->models-as-upgrades u)
                (xml-select/unit->models u)
                (xml-select/unit-upgrade->model u))]
         {:name    (attrs-name (first m))
          :number  (:number (:attrs (first m)))
          :chars   (characteristics  m)
          :weapons (assoc-weapon-attacks
                    (assoc-ids (concat (unit-weapons u) (weapons m))))})))

(defn get-models [f]
  (->> (models-models f)
       (map #(update % :models assoc-ids))))


(defn get-units [force]
  (->> (distinct (for [u (xml-select/units force)]
                   {:name
                    (attrs-name (first u))
                    :models (unit-models u)}))

      (map #(update % :models assoc-ids))))

(defn edn [forces]
  (assoc-ids (for [f forces]
               {:force-name (attrs-name (first f))
                ;; TODO: units must be unique (use sets)
                :units      (assoc-ids (set (concat (get-models f) (get-units f))))})))


(defn file->edn [file]
  (-> file
      zip-reader/zipper
      xml-select/forces
      edn
      ))

(defn parse [file-rosz]
  ;; TODO: generate random xml name file
  (let [file (zip-reader/unzip-file file-rosz "output.xml")]
    (file->edn file)))



(comment


  (def file "/Users/andreaimparato/Downloads/custTourney.rosz")

  (parse file)

  )
