(ns sandbox.state
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

(def components-path "resources/components.edn")

(defn load-library
  "Load component library from EDN file."
  []
  (let [f (io/file components-path)]
    (if (.exists f)
      (edn/read-string (slurp f))
      {})))

(defn save-library!
  "Save component library to EDN file.
   Pretty-prints for human readability."
  [library]
  (io/make-parents components-path)
  (spit components-path
        (with-out-str (pprint/pprint library))))

(defn initial-state
  "Create initial state, loading persisted library."
  []
  (atom {:preview {:hiccup nil}
         :view    {:type :preview}
         :library (load-library)}))
