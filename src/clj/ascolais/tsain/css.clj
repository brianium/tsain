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

(defn find-rule-end-line
  "Find the line number where a CSS rule ends (closing brace).
   start-line is 1-indexed. Returns 1-indexed end line.

   Scans from start-line counting braces until balanced.
   Handles:
   - Multi-line rules
   - Braces inside strings (content: \"}\")
   - Braces in comments"
  [css-string start-line]
  (let [lines (vec (str/split-lines css-string))
        start-idx (dec start-line)
        ;; Join lines from start position into single string for scanning
        text (str/join "\n" (subvec lines start-idx))]
    (loop [i 0
           brace-count 0
           in-string false
           string-char nil
           in-comment false
           line-offset 0]
      (if (>= i (count text))
        ;; End of text - return last line
        (count lines)
        (let [ch (nth text i)
              prev-ch (when (pos? i) (nth text (dec i)))
              next-ch (when (< (inc i) (count text)) (nth text (inc i)))]
          (cond
            ;; Newline - track line position
            (= ch \newline)
            (recur (inc i) brace-count in-string string-char in-comment (inc line-offset))

            ;; Comment start /*
            (and (not in-string) (not in-comment) (= ch \/) (= next-ch \*))
            (recur (+ i 2) brace-count in-string string-char true line-offset)

            ;; Comment end */
            (and in-comment (= ch \*) (= next-ch \/))
            (recur (+ i 2) brace-count in-string string-char false line-offset)

            ;; Inside comment - skip
            in-comment
            (recur (inc i) brace-count in-string string-char in-comment line-offset)

            ;; String start
            (and (not in-string) (or (= ch \") (= ch \')))
            (recur (inc i) brace-count true ch in-comment line-offset)

            ;; String end
            (and in-string (= ch string-char) (not= prev-ch \\))
            (recur (inc i) brace-count false nil in-comment line-offset)

            ;; Inside string - skip brace counting
            in-string
            (recur (inc i) brace-count in-string string-char in-comment line-offset)

            ;; Opening brace
            (= ch \{)
            (recur (inc i) (inc brace-count) in-string string-char in-comment line-offset)

            ;; Closing brace
            (= ch \})
            (let [new-count (dec brace-count)]
              (if (zero? new-count)
                ;; Found closing brace - return 1-indexed line number
                (+ start-line line-offset)
                (recur (inc i) new-count in-string string-char in-comment line-offset)))

            :else
            (recur (inc i) brace-count in-string string-char in-comment line-offset)))))))

(defn- find-selector-line
  "Find the 1-indexed line number where a selector appears in CSS.
   Returns nil if not found.

   Searches for the selector followed by whitespace then comma or brace.
   Works for selectors anywhere in a line (e.g., inside @media blocks)."
  [css-string selector-text]
  (let [lines (vec (str/split-lines css-string))
        ;; Escape regex special chars in selector
        escaped (str/replace selector-text #"[.*+?^${}()|\\[\\]\\\\]" "\\\\$0")
        ;; Match selector followed by optional whitespace then comma or opening brace
        ;; No ^ anchor - selector can appear anywhere in the line
        pattern (re-pattern (str escaped "\\s*[,{]"))]
    (loop [idx 0]
      (when (< idx (count lines))
        (if (re-find pattern (nth lines idx))
          (inc idx)  ;; 1-indexed
          (recur (inc idx)))))))

(defn remove-rules
  "Remove rules from stylesheet content string, return [remaining-css removed-css].
   Finds rules by their selector text and removes by line range."
  [css-string rules-to-remove]
  (if (empty? rules-to-remove)
    [(-> css-string str/trim (str "\n")) ""]
    (let [lines (vec (str/split-lines css-string))
          ;; Build line ranges for each rule by finding selector in source
          ranges (for [^RuleSet rule rules-to-remove
                       :let [;; Get the first selector's text
                             selector-text (str (first (.getSelectors rule)))
                             start-line (find-selector-line css-string selector-text)]
                       :when start-line
                       :let [end-line (find-rule-end-line css-string start-line)]]
                   {:start start-line
                    :end end-line
                    :selector selector-text})
          ;; Sort descending by start line to preserve line numbers during removal
          sorted-ranges (sort-by :start > ranges)
          ;; Extract removed content before modifying
          removed-sections (for [{:keys [start end]} (sort-by :start ranges)]
                             (str/join "\n" (subvec lines (dec start) end)))
          removed-css (str/join "\n\n" removed-sections)
          ;; Remove ranges from lines (bottom-up to preserve indices)
          remaining-lines (reduce (fn [ls {:keys [start end]}]
                                    ;; Convert 1-indexed to 0-indexed
                                    (let [start-idx (dec start)
                                          end-idx end]
                                      (vec (concat (subvec ls 0 start-idx)
                                                   (subvec ls end-idx)))))
                                  lines
                                  sorted-ranges)
          remaining-css (str/join "\n" remaining-lines)
          ;; Clean up excessive blank lines (3+ consecutive blank lines -> 2)
          remaining-css (str/replace remaining-css #"\n{3,}" "\n\n")
          ;; Trim leading/trailing whitespace but preserve single trailing newline
          remaining-css (-> remaining-css str/trim (str "\n"))]
      [remaining-css removed-css])))

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
