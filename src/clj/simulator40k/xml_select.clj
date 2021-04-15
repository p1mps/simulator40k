(ns simulator40k.xml-select
  (:require
   [clojure.data.zip.xml :as zx]))

(defn unit->models [unit]
  (zx/xml->
   unit
   :selections
   :selection))


(defn units [force]
  (zx/xml->
   force
   :selections
   :selection
   (zx/attr= :type "unit")))


(defn models [force]
  (zx/xml->
   force
   :selections
   :selection
   (zx/attr= :type "model")))


(defn forces [zipper]
  (zx/xml->
   zipper
   :roster
   :forces
   :force))
