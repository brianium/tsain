(ns ascolais.tsain.db
  "SQLite database utilities for tsain component storage.

  Provides:
  - Datasource creation from config
  - Schema migration runner
  - Query helpers

  Usage:
    (require '[ascolais.tsain.db :as db])

    ;; Create datasource
    (def ds (db/create-datasource {:database-file \"tsain.db\"}))

    ;; Run pending migrations
    (db/migrate! ds)

    ;; Check schema version
    (db/schema-version ds)"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Datasource Creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-datasource
  "Create a SQLite datasource from config.

  Config keys:
    :database-file - Path to SQLite file (required)

  Returns a next.jdbc datasource."
  [{:keys [database-file]}]
  (when-not database-file
    (throw (ex-info "Missing :database-file in config" {})))
  (jdbc/get-datasource {:dbtype "sqlite" :dbname database-file}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Migration System
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ensure-schema-version-table!
  "Create schema_version table if it doesn't exist."
  [ds]
  (jdbc/execute! ds
                 ["CREATE TABLE IF NOT EXISTS schema_version (
       version INTEGER PRIMARY KEY,
       applied_at TEXT NOT NULL DEFAULT (datetime('now'))
     )"]))

(defn schema-version
  "Get current schema version, or 0 if no migrations applied."
  [ds]
  (ensure-schema-version-table! ds)
  (let [result (jdbc/execute-one! ds
                                  ["SELECT MAX(version) as version FROM schema_version"]
                                  {:builder-fn rs/as-unqualified-maps})]
    (or (:version result) 0)))

(defn- list-migrations
  "List available migration resources sorted by version number."
  []
  (->> (io/resource "tsain/migrations")
       io/file
       file-seq
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".sql"))
       (map (fn [f]
              (let [name (.getName f)
                    version (parse-long (re-find #"^\d+" name))]
                {:version version
                 :name name
                 :file f})))
       (sort-by :version)))

(defn- strip-leading-comments
  "Remove leading comment lines from SQL string."
  [s]
  (->> (str/split-lines s)
       (drop-while #(or (str/blank? %) (str/starts-with? (str/trim %) "--")))
       (str/join "\n")
       str/trim))

(defn- run-migration!
  "Execute a single migration file."
  [ds {:keys [version name file]}]
  (let [sql (slurp file)]
    ;; Split on --;; delimiter (handles triggers with embedded semicolons)
    (doseq [stmt (str/split sql #"--;;")]
      (let [clean-stmt (strip-leading-comments stmt)]
        (when-not (str/blank? clean-stmt)
          (jdbc/execute! ds [clean-stmt]))))
    ;; Record version
    (jdbc/execute! ds
                   ["INSERT INTO schema_version (version) VALUES (?)" version])
    (tap> {:tsain/migration-applied {:version version :name name}})))

(defn migrate!
  "Run all pending migrations.

  Returns map with :applied (count) and :current-version."
  [ds]
  (ensure-schema-version-table! ds)
  (let [current (schema-version ds)
        pending (->> (list-migrations)
                     (filter #(> (:version %) current)))]
    (doseq [migration pending]
      (run-migration! ds migration))
    {:applied (count pending)
     :current-version (schema-version ds)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Query Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn find-component-by-tag
  "Find a component by its tag keyword."
  [ds tag]
  (jdbc/execute-one! ds
                     ["SELECT * FROM components WHERE tag = ?" (name tag)]
                     {:builder-fn rs/as-unqualified-maps}))

(defn find-all-components
  "Get all components ordered by tag."
  [ds]
  (jdbc/execute! ds
                 ["SELECT * FROM components ORDER BY tag"]
                 {:builder-fn rs/as-unqualified-maps}))

(defn find-examples-for-component
  "Get all examples for a component ID."
  [ds component-id]
  (jdbc/execute! ds
                 ["SELECT * FROM examples WHERE component_id = ? ORDER BY sort_order, id"
                  component-id]
                 {:builder-fn rs/as-unqualified-maps}))

(defn insert-component!
  "Insert a new component, returning the inserted row."
  [ds {:keys [tag category]}]
  (jdbc/execute-one! ds
                     ["INSERT INTO components (tag, category, created_at, updated_at)
      VALUES (?, ?, datetime('now'), datetime('now'))
      RETURNING *"
                      (name tag) category]
                     {:builder-fn rs/as-unqualified-maps}))

(defn update-component!
  "Update a component's category and updated_at timestamp."
  [ds component-id {:keys [category]}]
  (jdbc/execute-one! ds
                     ["UPDATE components
      SET category = ?, updated_at = datetime('now')
      WHERE id = ?
      RETURNING *"
                      category component-id]
                     {:builder-fn rs/as-unqualified-maps}))

(defn delete-component!
  "Delete a component and its examples (via CASCADE)."
  [ds component-id]
  (jdbc/execute! ds
                 ["DELETE FROM components WHERE id = ?" component-id]))

(defn insert-example!
  "Insert an example for a component."
  [ds {:keys [component-id label hiccup sort-order]}]
  (jdbc/execute-one! ds
                     ["INSERT INTO examples (component_id, label, hiccup, sort_order)
      VALUES (?, ?, ?, ?)
      RETURNING *"
                      component-id label (pr-str hiccup) (or sort-order 0)]
                     {:builder-fn rs/as-unqualified-maps}))

(defn delete-examples-for-component!
  "Delete all examples for a component."
  [ds component-id]
  (jdbc/execute! ds
                 ["DELETE FROM examples WHERE component_id = ?" component-id]))

(defn search-components
  "Full-text search across components using FTS5.
  Returns components matching the query."
  [ds query]
  (jdbc/execute! ds
                 ["SELECT c.* FROM components c
      JOIN components_fts fts ON c.id = fts.rowid
      WHERE components_fts MATCH ?
      ORDER BY rank"
                  query]
                 {:builder-fn rs/as-unqualified-maps}))

(defn parse-hiccup
  "Parse hiccup string from database back to Clojure data."
  [hiccup-str]
  (when hiccup-str
    (edn/read-string hiccup-str)))
