(ns ascolais.tsain.clj
  "Clojure code utilities for tsain component development.

  Provides:
  - cljfmt formatter detection (if on classpath)
  - Component name extraction from defelem forms
  - Category inference from component names"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Clojure Formatter Detection
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn load-cljfmt-config
  "Load cljfmt configuration from project's .cljfmt.edn if present.
   Falls back to empty map (cljfmt defaults) if not found."
  []
  (let [config-file (io/file ".cljfmt.edn")]
    (if (.exists config-file)
      (try
        (-> config-file slurp edn/read-string)
        (catch Exception e
          (tap> {:cljfmt/config-error (.getMessage e)})
          {}))
      {})))

(defn detect-clj-formatter
  "Detect available Clojure formatter.

   Returns a formatter function that takes a path and formats in-place.
   - Uses cljfmt if available on classpath (project's .cljfmt.edn config)
   - No-op if cljfmt not available

   Since cljfmt is a library (not CLI), we try-require it at detection time."
  []
  (if (try
        (require 'cljfmt.core)
        true
        (catch Exception _
          (tap> {:cljfmt/status :not-available
                 :message "Add cljfmt to :dev deps for Clojure formatting"})
          false))
    (let [config (load-cljfmt-config)
          reformat (resolve 'cljfmt.core/reformat-string)]
      (tap> {:cljfmt/status :available :config config})
      (fn [path]
        (let [original (slurp path)
              formatted (reformat original config)]
          (when (not= original formatted)
            (spit path formatted)))))
    ;; No-op formatter if cljfmt not available
    (fn [_path] nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component Name Extraction
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn extract-defelem-name
  "Extract the component name from a defelem form string.

   Example:
     (extract-defelem-name \"(hy/defelem game-card [...] ...)\")
     => \"game-card\"

   Returns nil if no defelem form found."
  [code-string]
  (when-let [[_ name] (re-find #"\(hy/defelem\s+(\S+)" code-string)]
    name))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Category Inference
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def component-category-suffixes
  "Map of category to component name suffixes that indicate that category."
  {"cards"      ["card" "tile" "panel"]
   "controls"   ["btn" "button" "input" "select" "toggle" "checkbox" "radio"]
   "feedback"   ["toast" "alert" "loader" "progress" "spinner"]
   "navigation" ["nav" "menu" "tab" "breadcrumb"]
   "display"    ["badge" "avatar" "text" "heading" "label" "indicator"]
   "overlays"   ["modal" "popover" "tooltip" "dropdown"]
   "layout"     ["container" "grid" "flex" "row" "col" "spacer"]})

(defn infer-category-from-name
  "Infer category from component name suffix.

   Example:
     (infer-category-from-name \"game-card\")   => \"cards\"
     (infer-category-from-name \"action-btn\")  => \"controls\"
     (infer-category-from-name \"my-widget\")   => nil

   Returns nil if no category can be inferred."
  [component-name]
  (let [name-str (str component-name)]
    (->> component-category-suffixes
         (filter (fn [[_cat suffixes]]
                   (some #(or (= name-str %)
                              (str/ends-with? name-str (str "-" %)))
                         suffixes)))
         first
         first)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespace Path Utilities
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn namespace->path
  "Convert namespace symbol to relative file path.

   Example:
     (namespace->path 'sandbox.ui) => \"sandbox/ui.clj\"
     (namespace->path 'sandbox.ui.cards) => \"sandbox/ui/cards.clj\""
  [ns-sym]
  (-> (str ns-sym)
      (str/replace "." "/")
      (str/replace "-" "_")
      (str ".clj")))

(defn find-namespace-file
  "Find the file for a namespace across source paths.

   Returns the first matching path, or nil if not found."
  [ns-sym source-paths project-root]
  (let [relative-path (namespace->path ns-sym)]
    (->> source-paths
         (map #(io/file project-root % relative-path))
         (filter #(.exists ^java.io.File %))
         first
         (#(when % (.getPath ^java.io.File %))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Defelem Form Extraction
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-defelem-forms
  "Find all defelem forms in a Clojure source string.

   Returns a vector of maps:
     [{:name \"game-card\"
       :content \"(hy/defelem game-card ...)\"
       :start-line 42
       :end-line 55}]

   Uses bracket/paren matching to find complete forms."
  [source]
  (let [lines (str/split-lines source)
        line-count (count lines)]
    (loop [idx 0
           forms []
           in-form nil
           depth 0
           form-lines []]
      (if (>= idx line-count)
        ;; Return accumulated forms
        (if in-form
          (conj forms {:name in-form
                       :content (str/join "\n" form-lines)
                       :start-line (- idx (count form-lines) -1)
                       :end-line idx})
          forms)
        (let [line (nth lines idx)
              trimmed (str/trim line)]
          (if in-form
            ;; Count parens to find form end
            (let [opens (count (re-seq #"\(" line))
                  closes (count (re-seq #"\)" line))
                  new-depth (+ depth opens (- closes))
                  new-form-lines (conj form-lines line)]
              (if (<= new-depth 0)
                ;; Form complete
                (recur (inc idx)
                       (conj forms {:name in-form
                                    :content (str/join "\n" new-form-lines)
                                    :start-line (- idx (count form-lines) -1)
                                    :end-line (inc idx)})
                       nil
                       0
                       [])
                (recur (inc idx) forms in-form new-depth new-form-lines)))
            ;; Look for start of defelem
            (if-let [[_ name] (re-find #"^\(hy/defelem\s+(\S+)" trimmed)]
              (let [opens (count (re-seq #"\(" line))
                    closes (count (re-seq #"\)" line))
                    new-depth (+ opens (- closes))]
                (if (<= new-depth 0)
                  ;; Single-line defelem (unlikely but possible)
                  (recur (inc idx)
                         (conj forms {:name name
                                      :content line
                                      :start-line (inc idx)
                                      :end-line (inc idx)})
                         nil 0 [])
                  (recur (inc idx) forms name new-depth [line])))
              (recur (inc idx) forms nil 0 []))))))))

(defn patterns-for-category
  "Get component name patterns for a category.

   Returns vector of patterns that match component names for this category.
   Patterns check for exact match or -suffix match."
  [category]
  (get component-category-suffixes category
       ;; Default: category name and plural
       [(str category) (str category "s")]))

(defn component-matches-category?
  "Check if a component name matches a category's patterns."
  [component-name category]
  (let [patterns (patterns-for-category category)
        name-str (str component-name)]
    (some #(or (= name-str %)
               (str/ends-with? name-str (str "-" %)))
          patterns)))

(defn filter-forms-by-category
  "Filter defelem forms to those matching a category."
  [forms category]
  (filter #(component-matches-category? (:name %) category) forms))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Namespace Manipulation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn add-require-to-source
  "Add a require clause to a Clojure source file.

   Finds the (:require ...) form and adds the new namespace.
   If the require already exists, returns nil (no change needed).
   Returns the modified source string."
  [source required-ns]
  (let [require-clause (str "[" required-ns "]")]
    (if (str/includes? source require-clause)
      nil ;; Already has this require
      ;; Find (:require and insert after the first [
      (if-let [[match] (re-find #"\(:require\s+\[" source)]
        ;; Has :require - insert right after "(:require "
        (str/replace-first source
                           #"(\(:require\s+)"
                           (str "$1" require-clause "\n            "))
        ;; No :require - add before first defelem
        (if-let [[match] (re-find #"(?m)^\(hy/defelem" source)]
          (str/replace-first source
                             #"(?m)(^\(hy/defelem)"
                             (str "(:require " require-clause ")\n\n$1"))
          source)))))

(defn remove-forms-from-source
  "Remove specified defelem forms from source code.

   Returns the source with forms removed and extra blank lines cleaned up."
  [source forms-to-remove]
  (reduce (fn [src form]
            (str/replace src (:content form) ""))
          source
          forms-to-remove))

(defn generate-sub-namespace
  "Generate source code for a sub-namespace containing extracted forms.

   parent-ns: symbol like 'sandbox.ui
   category: string like \"cards\"
   forms: vector of form maps from find-defelem-forms"
  [parent-ns category forms]
  (let [sub-ns (symbol (str parent-ns "." category))
        header (str "(ns " sub-ns "\n"
                    "  \"" (str/capitalize category) " components extracted from " parent-ns ".\"\n"
                    "  (:require [html.yeah :as hy]))\n\n")
        body (str/join "\n\n" (map :content forms))]
    (str header body "\n")))
