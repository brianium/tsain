(ns ascolais.tsain.css
  "CSS utilities for tsain component development.

  Provides:
  - Formatter detection (prettier, biome, or JVM fallback)
  - Basic CSS normalization for JVM-only environments
  - jStyleParser wrappers for CSS parsing/manipulation"
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str])
  (:import [cz.vutbr.web.css CSSFactory StyleSheet RuleSet]
           [java.io StringReader]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Formatter Detection
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sh-available?
  "Check if a shell command is available on the system."
  [cmd]
  (let [{:keys [exit]} (shell/sh "which" cmd)]
    (zero? exit)))

(defn basic-css-formatter
  "Basic JVM CSS normalizer - not full formatting, just readable.
   - Consistent newlines after { and before }
   - Trim trailing whitespace per line
   - Preserve existing structure"
  [path]
  (let [content (slurp path)
        lines (str/split-lines content)
        normalized (->> lines
                        (map str/trimr)
                        (str/join "\n"))]
    (spit path (str normalized "\n"))))

(defn detect-css-formatter
  "Detect available CSS formatter, with JVM fallback.

   Returns a formatter function that takes a path and formats in-place."
  []
  (cond
    (sh-available? "prettier")
    (fn [path]
      (let [{:keys [exit err]} (shell/sh "prettier" "--write" path)]
        (when-not (zero? exit)
          (throw (ex-info "prettier failed" {:path path :exit exit :err err})))))

    (sh-available? "biome")
    (fn [path]
      (let [{:keys [exit err]} (shell/sh "biome" "format" "--write" path)]
        (when-not (zero? exit)
          (throw (ex-info "biome failed" {:path path :exit exit :err err})))))

    :else
    basic-css-formatter))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Project Root Inference
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- find-marker-file
  "Walk up from dir looking for a marker file."
  [dir marker-name]
  (loop [current (io/file dir)]
    (when current
      (let [marker (io/file current marker-name)]
        (if (.exists marker)
          (.getCanonicalPath current)
          (recur (.getParentFile current)))))))

(defn infer-project-root
  "Infer project root by looking for marker files.

   Priority:
   1. tsain.edn
   2. deps.edn
   3. project.clj
   4. .git directory"
  ([]
   (infer-project-root (System/getProperty "user.dir")))
  ([starting-dir]
   (or (find-marker-file starting-dir "tsain.edn")
       (find-marker-file starting-dir "deps.edn")
       (find-marker-file starting-dir "project.clj")
       (find-marker-file starting-dir ".git")
       starting-dir)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; jStyleParser Wrappers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-stylesheet
  "Parse CSS string to jStyleParser StyleSheet."
  [css-string]
  (CSSFactory/parseString css-string nil))

(defn- selector-matches-pattern?
  "Check if a selector string starts with the given pattern.
   Pattern '.card' matches '.card', '.card-header', '.card:hover', etc."
  [selector-str pattern]
  (let [pattern-str (if (str/starts-with? pattern ".")
                      pattern
                      (str "." pattern))]
    (or (= selector-str pattern-str)
        (str/starts-with? selector-str (str pattern-str "-"))
        (str/starts-with? selector-str (str pattern-str ":"))
        (str/starts-with? selector-str (str pattern-str "["))
        (str/starts-with? selector-str (str pattern-str " "))
        (str/starts-with? selector-str (str pattern-str ">"))
        (str/starts-with? selector-str (str pattern-str "+"))
        (str/starts-with? selector-str (str pattern-str "~")))))

(defn- rule-matches-pattern?
  "Check if a CSS rule has any selector matching the pattern."
  [^RuleSet rule pattern]
  (some #(selector-matches-pattern? (str %) pattern)
        (.getSelectors rule)))

(defn find-rules-by-pattern
  "Find all rules where any selector matches the pattern.
   Pattern '.card' matches '.card', '.card-header', '.card:hover', etc.

   Returns a vector of RuleSet objects."
  [^StyleSheet stylesheet pattern]
  (->> stylesheet
       (filter #(instance? RuleSet %))
       (filter #(rule-matches-pattern? % pattern))
       vec))

(defn remove-rules
  "Remove rules from stylesheet content string, return [remaining-css removed-css].
   Uses jStyleParser for reliable parsing."
  [css-string rules-to-remove]
  (let [lines (str/split-lines css-string)
        rule-texts (set (map str rules-to-remove))
        ;; Simple approach: serialize rules to remove and filter from original
        removed-css (str/join "\n\n" rule-texts)
        remaining-css (reduce (fn [css rule-text]
                                (str/replace css rule-text ""))
                              css-string
                              rule-texts)
        ;; Clean up extra blank lines
        remaining-css (str/replace remaining-css #"\n{3,}" "\n\n")]
    [remaining-css removed-css]))

(defn serialize
  "Convert StyleSheet back to CSS string."
  [^StyleSheet stylesheet]
  (.toString stylesheet))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Category to Pattern Mapping
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def category-patterns
  "Default category to selector patterns mapping."
  {"cards"     [".card" ".cards"]
   "controls"  [".btn" ".button" ".input" ".select" ".toggle" ".checkbox" ".radio"]
   "layout"    [".container" ".grid" ".flex" ".row" ".col" ".spacer"]
   "feedback"  [".toast" ".alert" ".loader" ".progress" ".spinner"]
   "navigation" [".nav" ".menu" ".tab" ".breadcrumb"]
   "display"   [".badge" ".avatar" ".text" ".heading" ".label"]
   "overlays"  [".modal" ".popover" ".tooltip" ".dropdown"]})

(defn patterns-for-category
  "Get selector patterns for a category, or infer from category name."
  [category]
  (or (get category-patterns category)
      [(str "." category) (str "." category "s")]))
