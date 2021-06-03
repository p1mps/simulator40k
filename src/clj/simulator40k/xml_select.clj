(ns simulator40k.xml-select
  (:require
   [clojure.data.zip.xml :as zx]))

(defn unit->models [unit]
  (zx/xml->
   unit
   :selections
   :selection
   (zx/attr= :type "model")))

(defn unit-upgrade->model [unit]
  (zx/xml->
   unit
   :profiles
   :profile
   (zx/attr= :typeName "Unit")
))

(defn unit->models-as-upgrades [unit]
  (zx/xml->
   unit
   :selections
   :selection
   (zx/attr= :type "upgrade")))

(defn unit->weapons [unit]
  (zx/xml->
   unit
   :profiles
   :profile
   (zx/attr= :typeName "Weapon")))

(defn units [force]
  (zx/xml->
   force
   :force
   :selections
   :selection
   (zx/attr= :type "unit")))

(defn units-as-upgrades [force]
  (zx/xml->
   force
   :force
   :selections
   :selection
   (zx/attr= :type "upgrade")))

(defn forces->force [forces]
  (zx/xml->
   forces))

(defn models [force]
  (zx/xml->
   force
   :force
   :selections
   :selection
   (zx/attr= :type "model")))


(defn models-upgrades [force]
  (zx/xml->
   force
   :force
   :selections
   :selection
   (zx/attr= :type "upgrade")
   ))

(defn unit-as-upgrade-characteristcs [unit]
  (zx/xml->
   unit
   :characteristics
))

(defn forces [zipper]
  (zx/xml->
   zipper
   :roster
   :forces
   :force))
