# Better Component Storage - Implementation Plan

## Overview

Step-by-step implementation tasks. Update checkboxes and add commit hashes as you progress.

## Prerequisites

- [x] Add html.yeah dependency to tsain
- [x] Add manse dependency to tsain
- [x] Add SQLite JDBC driver: `org.xerial/sqlite-jdbc {:mvn/version "3.51.1.0"}`
- [x] Verify SQLite works in consumer project context

## Phase 1: html.yeah Integration

- [x] Add html.yeah to deps.edn
- [x] Convert `dev/src/clj/sandbox/ui.clj` from `c/resolve-alias` to `defelem`:
  - `::game-card` → `(defelem game-card ...)`
  - `::player-hud` → `(defelem player-hud ...)`
  - `::toast` → `(defelem toast ...)`
  - `::event-modal` → `(defelem event-modal ...)`
  - `::action-buttons` → `(defelem action-buttons ...)`
  - `::resource-display` → `(defelem resource-display ...)`
  - `::combat-log` → `(defelem combat-log ...)`
  - `::card-type-badges` → `(defelem card-type-badges ...)`
  - `::player-portrait` → `(defelem player-portrait ...)`
  - Add malli schemas for attributes on each
  - Add `:doc` metadata to each element
- [x] Verify `hy/element` returns expected metadata for converted elements
- [ ] Test runtime validation with malli.dev instrumentation
- [x] Update CLAUDE.md with `defelem` patterns

## Phase 2: Database Foundation

- [x] Create `ascolais.tsain.db` namespace for database utilities
- [x] Implement `create-datasource` function (SQLite file path from config)
- [x] Create `resources/tsain/migrations/` directory
- [x] Write `001-initial-schema.sql` migration:
  - `components` table (id, tag, category, created_at, updated_at)
  - `examples` table (id, component_id, label, hiccup, sort_order)
  - `schema_version` table
  - FTS5 virtual table for search
  - **Note:** No `props` table - html.yeah provides prop schemas
- [x] Implement migration runner (check version, run pending SQL)
- [x] Add `:database-file` to `default-config` in `tsain.clj`
- [x] Initialize datasource in `registry` function

## Phase 3: Core Effects Migration

- [x] Refactor `::commit` effect to write to SQLite
  - Insert into `components` table (tag, category, timestamps)
  - Insert examples into `examples` table
  - Update FTS index (via triggers)
  - **Simplified:** No props storage - html.yeah provides those
- [x] Refactor `::uncommit` effect to delete from SQLite
- [x] Refactor library loading in `registry` to read from SQLite
- [x] Update `::show-components` to query SQLite (works via in-memory library sync)
- [x] Ensure all sandbox views work with SQLite-backed data
- [ ] Remove EDN persistence code (keep for migration only)

## Phase 4: Discovery API

- [ ] Create `ascolais.tsain.discovery` namespace
- [ ] Implement `describe` function:
  - 1-arity: list all components, merge html.yeah metadata with SQLite data
  - 2-arity: full details for specific component
  - Returns: `{:tag :doc :attributes :children :category :examples}`
- [ ] Implement `grep` function:
  - Search html.yeah docs via `hy/search-elements`
  - Search SQLite categories via FTS5
  - Merge and dedupe results
- [ ] Implement `props` function:
  - Query html.yeah for components with matching attribute names
  - Use malli schema introspection
- [ ] Export discovery functions from main `ascolais.tsain` namespace

## Phase 5: Migration Tooling

- [ ] Implement `::migrate-from-edn` effect
  - Read existing components.edn
  - Insert each component into SQLite (examples, timestamps)
  - Descriptions: prefer html.yeah `:doc`, fallback to EDN description
  - Report migration summary
- [ ] Add auto-migration logic to `registry`:
  - If `:database-file` missing and `:components-file` exists
  - Auto-migrate on first run
  - Emit deprecation warning via tap>
- [ ] Dogfood: migrate tsain's own `resources/components.edn`
  - Run `[::tsain/migrate-from-edn "resources/components.edn"]`
  - Verify all 9 components appear in SQLite
  - Verify examples render correctly in sandbox
  - Verify `tsain/describe` returns merged data (html.yeah + SQLite)
  - Delete `resources/components.edn` after successful migration
- [ ] Document migration process for consumers

## Phase 6: Documentation & Cleanup

- [ ] Update README.md with:
  - New configuration options (`:database-file`)
  - html.yeah requirement
  - `defelem` examples
- [ ] Update CLAUDE.md with:
  - Discovery API examples
  - `defelem` patterns for component authoring
- [ ] Update hiccup-component skill:
  - Rename to tsain skill (future work)
  - Document html.yeah discoverability workflow
  - Show how to query props for Claude integration
- [ ] Add tests for:
  - Database initialization
  - Migration runner
  - Core effects (commit, uncommit, show)
  - Discovery functions (describe, grep, props)
  - EDN migration
  - html.yeah integration (verify `hy/element` returns expected metadata after `defelem`)
  - FTS5 search accuracy (exact match, partial match, category filtering)
  - Merge logic (html.yeah + SQLite data combination in `tsain/describe`)
- [ ] Remove deprecated `:components-file` handling (or keep with deprecation)

## Files to Create/Modify

| File | Action |
|------|--------|
| `src/clj/ascolais/tsain/db.clj` | Create - database utilities |
| `src/clj/ascolais/tsain/discovery.clj` | Create - discovery API (merges html.yeah + SQLite) |
| `resources/tsain/migrations/*.sql` | Create - SQL migration files |
| `src/clj/ascolais/tsain.clj` | Modify - integrate SQLite, expose discovery |
| `dev/src/clj/sandbox/ui.clj` | Modify - convert to `defelem` |
| `deps.edn` | Modify - add html.yeah, manse, `org.xerial/sqlite-jdbc {:mvn/version "3.51.1.0"}` |
| `tsain.edn` | Modify - add `:database-file` example |

## Rollback Plan

If issues arise:
1. SQLite file can be deleted, reverting to empty library
2. Keep `components.edn` loader as fallback during transition
3. `defelem` compiles to same chassis alias - existing views still work

## Version Compatibility Notes

All 0.x.x versions will:
- Support both `:components-file` (deprecated) and `:database-file`
- Auto-migrate if only `:components-file` is configured
- Maintain SQLite schema compatibility via migrations
- Emit tap> warnings for deprecated config

## html.yeah Integration Notes

### defelem Example

```clojure
(require '[html.yeah :refer [defelem children]])

(defelem button
  [:map {:doc "Primary action button with multiple variants"}
   [:button/label :string]
   [:button/variant {:default :primary} [:enum :primary :secondary :ghost]]
   [:button/size {:default :md} [:enum :sm :md :lg]]
   [:button/icon {:optional true} :string]]
  (let [{:button/keys [label variant size icon]} attrs]
    [:button.btn {:class [(str "btn--" (name variant))
                          (str "btn--" (name size))]}
     (when icon [:span.btn-icon icon])
     label]))
```

### Querying at Runtime

```clojure
(require '[html.yeah :as hy])

;; Get component metadata
(hy/element :myapp.ui/button)
;; => {:tag :myapp.ui/button
;;     :doc "Primary action button..."
;;     :attributes [:map [:button/label :string] ...]
;;     :children ...}

;; Search docs
(hy/search-elements "button")
;; => [{:tag :myapp.ui/button ...} {:tag :myapp.ui/icon-button ...}]
```

### tsain Discovery Merges Both Sources

```clojure
(tsain/describe dispatch :myapp.ui/button)
;; => {:tag :myapp.ui/button
;;     :doc "..."                    ;; from html.yeah
;;     :attributes [...]             ;; from html.yeah
;;     :category :forms              ;; from SQLite
;;     :examples [{:label "..." :hiccup [...]}]}  ;; from SQLite
```
