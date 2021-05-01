(ns simulator40k.zip-reader
  (:require
   [clojure.java.io :refer [file] :as io]
   [clojure.xml :as xml]
   [clojure.zip :as zip])
  (:import (java.io File)
           [java.util.zip   ZipInputStream]))

(defn unzip-file
  "uncompress zip archive.
  `input` - name of zip archive to be uncompressed.
  `output` - name of folder where to output."
  [input output]
  (with-open [stream (-> input io/input-stream ZipInputStream.)]
    (loop [entry (.getNextEntry stream)]
      (if entry
        (let [save-path (str "./" output)
              out-file (File. save-path)]
          (if (.isDirectory entry)
            (if-not (.exists out-file)
              (.mkdirs out-file))
            (let [parent-dir (File. (.substring save-path 0 (.lastIndexOf save-path (int File/separatorChar))))]
              (if-not (.exists parent-dir) (.mkdirs parent-dir))
              (clojure.java.io/copy stream out-file)))
          (recur (.getNextEntry stream))))))
  (slurp output))

(defn zipper [file]
  (-> (.getBytes file)
      java.io.ByteArrayInputStream.
      xml/parse
      zip/xml-zip))
