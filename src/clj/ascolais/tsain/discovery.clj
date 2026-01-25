(ns ascolais.tsain.discovery
  "Discovery API for tsain components.

  Merges metadata from two sources:
  - html.yeah: element definitions (:doc, :attributes, :children)
  - SQLite: runtime data (:category, :examples)

  Usage:
    (require '[ascolais.tsain.discovery :as discovery])

    ;; List all components
    (discovery/describe registry)

    ;; Get details for one component
    (discovery/describe registry :sandbox.ui/game-card)

    ;; Search by keyword
    (discovery/grep registry \"button\")

    ;; Find components with specific props
    (discovery/props registry :variant)"
  (:require [ascolais.tsain.db :as db]
            [clojure.string :as str]
            [html.yeah :as hy]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Internal Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-datasource
  "Extract datasource from registry."
  [registry]
  (:ascolais.tsain/datasource registry))

(defn- get-library
  "Extract library from registry state."
  [registry]
  (:library @(:ascolais.tsain/state registry)))

(defn- html-yeah-element
  "Get element metadata from html.yeah registry."
  [tag]
  (hy/element tag))

(defn- merge-component-data
  "Merge html.yeah metadata with SQLite data for a component."
  [tag library-data]
  (let [hy-data (html-yeah-element tag)]
    (cond-> {:tag tag}
      ;; From html.yeah
      hy-data (assoc :doc (:doc hy-data)
                     :attributes (:attributes hy-data)
                     :children (:children hy-data))
      ;; From SQLite (via in-memory library)
      library-data (assoc :category (:category library-data)
                          :examples (:examples library-data)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn describe
  "Get component metadata, merging html.yeah and SQLite data.

  With 1 arg (registry): returns seq of all components with basic info
  With 2 args (registry, tag): returns full details for one component

  Returns:
    {:tag :sandbox.ui/game-card
     :doc \"Cyberpunk-styled game card...\"
     :attributes [:map [:game-card/title :string] ...]
     :children [:* :any]
     :category \"cards\"
     :examples [{:label \"Default\" :hiccup [...]}]}"
  ([registry]
   (let [library (get-library registry)
         ;; Get all html.yeah elements
         hy-elements (hy/elements)
         ;; Merge with library data, preferring html.yeah tags
         all-tags (into #{} (concat (keys library)
                                    (map :tag hy-elements)))]
     (->> all-tags
          (map (fn [tag]
                 (merge-component-data tag (get library tag))))
          (sort-by :tag))))
  ([registry tag]
   (let [library (get-library registry)
         library-data (get library tag)]
     (merge-component-data tag library-data))))

(defn grep
  "Search components by keyword in docs, tags, and categories.

  Searches:
  - html.yeah :doc strings via hy/search-elements
  - Component tags (substring match)
  - SQLite categories via FTS5 (if datasource available)

  Returns seq of component summaries."
  [registry query]
  (let [library (get-library registry)
        ds (get-datasource registry)
        query-lower (str/lower-case query)

        ;; Search html.yeah docs
        hy-matches (->> (hy/search-elements query)
                        (map :tag)
                        set)

        ;; Search tags by substring
        tag-matches (->> (keys library)
                         (filter #(str/includes? (str/lower-case (str %)) query-lower))
                         set)

        ;; Search SQLite FTS if available
        db-matches (when ds
                     (->> (db/search-components ds query)
                          (map #(keyword (:tag %)))
                          set))

        ;; Combine all matches
        all-matches (into #{} (concat hy-matches tag-matches db-matches))]

    (->> all-matches
         (map #(describe registry %))
         (sort-by :tag))))

(defn props
  "Find components that have a specific attribute/prop.

  Searches html.yeah element schemas for attribute names.

  Example:
    (props registry :variant)  ;; Find components with :*/variant props

  Returns seq of component summaries."
  [registry prop-name]
  (let [library (get-library registry)
        prop-str (name prop-name)

        ;; Search all html.yeah elements for matching props
        matching-tags
        (->> (hy/elements)
             (filter (fn [elem]
                       (let [attrs (:attributes elem)]
                         (when (and (vector? attrs) (= :map (first attrs)))
                           ;; Check if any attribute key contains the prop name
                           (some (fn [attr-entry]
                                   (when (vector? attr-entry)
                                     (let [k (first attr-entry)]
                                       (and (keyword? k)
                                            (str/includes? (name k) prop-str)))))
                                 (rest attrs))))))
             (map :tag))]

    (->> matching-tags
         (map #(describe registry %))
         (sort-by :tag))))

(defn categories
  "List all unique categories from the component library.

  Returns sorted seq of category strings."
  [registry]
  (let [library (get-library registry)]
    (->> library
         vals
         (keep :category)
         distinct
         sort)))

(defn by-category
  "Get components filtered by category.

  Returns seq of component summaries."
  [registry category]
  (let [library (get-library registry)]
    (->> library
         (filter (fn [[_ data]] (= (:category data) category)))
         (map (fn [[tag _]] (describe registry tag)))
         (sort-by :tag))))
