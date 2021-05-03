(ns simulator40k.xml-select
  (:require
   [clojure.data.zip.xml :as zx]))

(defn unit->models [unit]
  (zx/xml->
   unit
   :selections
   :selection
   (zx/attr= :type "model")))

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

(defn forces [zipper]
  (zx/xml->
   zipper
   :roster
   :forces
   :force))
