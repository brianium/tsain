# Phandaal Based CSS Actions And Effects - Research

## Problem Statement

The tsain skill currently relies on text-based prompting to enforce file size limits during component development. Claude is instructed to check `wc -l` before commits and split files when they exceed a threshold. This approach is brittle:

1. **Manual checks are easy to forget** - Claude must remember to run `wc -l` at the right time
2. **Text prompts don't guarantee action** - Instructions in SKILL.md are suggestions, not enforcement
3. **No feedback loop** - Claude doesn't know if a write pushed a file over threshold until manually checking
4. **Split decisions lack context** - Which category? What patterns to extract? Claude must figure this out each time

The result is inconsistent file organization and occasional large monolithic stylesheets that accumulate technical debt.

## Requirements

### Functional Requirements

1. All CSS writes during tsain development must flow through tracked effects
2. Effects must return LOC metadata (before, after, delta) for every write
3. When a file exceeds the configured threshold, the result must include actionable hints
4. Hints must be domain-specific: suggest exact target files, categories, and next actions
5. Split operations must be available as first-class effects
6. Configuration is minimal: threshold and stylesheet path from `tsain.edn` or convention; categories from database

### Non-Functional Requirements

- **Ergonomics**: Results should be self-documenting; Claude shouldn't need separate instructions
- **Composability**: Actions should compose with existing sandestin dispatch patterns
- **Auditability**: All CSS modifications should be trackable via phandaal's audit system
- **Hot-reload compatibility**: CSS writes must not break the existing hot-reload workflow

## Options Considered

### Option A: Enhanced Text Prompting

**Description:** Improve the SKILL.md instructions with more specific guidance, examples, and reminders at key workflow points.

**Pros:**
- Zero implementation effort
- No new dependencies

**Cons:**
- Still relies on Claude following instructions
- No automatic threshold detection
- No structured feedback in results
- Doesn't scale - more text = more likely to be skipped

### Option B: Custom File Effects in Tsain

**Description:** Build file I/O effects directly in tsain with LOC tracking and hint generation.

**Pros:**
- Full control over implementation
- No external dependencies

**Cons:**
- Duplicates phandaal's core functionality (atomic writes, LOC tracking, formatters)
- Must maintain two file I/O systems
- Misses phandaal's audit logging, reload tracking, and future features

### Option C: Phandaal Integration with Domain Actions

**Description:** Use phandaal as the file I/O layer. Tsain provides pure actions that wrap phandaal effects and add domain-specific hints based on phandaal's threshold detection.

**Architecture:**

```
┌─────────────────────────────────────────────────────────────────┐
│                    Claude dispatches                            │
│         [::tsain/write-css ".card { ... }" {:category "cards"}] │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Tsain Action (pure)                           │
│  - Resolves stylesheet path from config                         │
│  - Attaches category metadata                                   │
│  - Returns phandaal effect vector                               │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Phandaal Effect                               │
│  - Atomic file write                                            │
│  - LOC tracking (before/after/delta)                            │
│  - Threshold detection                                          │
│  - Formatter execution                                          │
│  - Returns result with empty hints[]                            │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Tsain Interceptor                             │
│  - Inspects phandaal result                                     │
│  - If threshold exceeded, adds domain hints                     │
│  - Hints include: category, target file, suggested action       │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Result to Claude                              │
│  {:status :ok                                                   │
│   :loc {:before 1480 :after 1520 :delta 40}                     │
│   :threshold {:exceeded? true :remaining -20}                   │
│   :hints [{:type :split-suggested                               │
│            :category :cards                                     │
│            :target "components/cards.css"                       │
│            :message "Extract card styles..."}]}                 │
└─────────────────────────────────────────────────────────────────┘
```

**Pros:**
- Leverages phandaal's battle-tested file I/O
- Clean separation: phandaal = infrastructure, tsain = domain logic
- Hints are structured data, not text instructions
- Results guide next actions without external prompting
- Audit logging comes free
- Future phandaal features (reload tracking) available automatically

**Cons:**
- Adds phandaal as a dependency
- Requires interceptor coordination between libraries

### Option D: Phandaal with Callback Hooks

**Description:** Similar to Option C, but instead of interceptors, phandaal accepts a callback function that receives results and can augment hints.

**Pros:**
- Simpler than interceptor coordination
- Callback is explicitly configured per-registry

**Cons:**
- Less composable than interceptors
- Callback runs synchronously in effect handler
- Harder to test in isolation

## Recommendation

**Option C: Phandaal Integration with Domain Actions** is recommended because:

1. **Structured feedback** - Results include actionable hints, not just data
2. **Single source of truth** - Phandaal handles all file I/O concerns
3. **Domain separation** - Tsain adds CSS/component-specific knowledge
4. **Extensibility** - Same pattern works for namespace splitting (Clojure files)
5. **No duplication** - Leverages existing infrastructure

## Technical Design

### Dependency Setup

Add phandaal to tsain's dependencies:

```clojure
;; deps.edn
{:deps {io.github.brianium/phandaal {:git/tag "v0.1.0" :git/sha "a51d91e"}}}
```

### Registry Composition

Tsain registry includes phandaal registry. Project root is inferred from tsain.edn location, deps.edn, or git root:

```clojure
(defn registry
  [{:keys [database-file stylesheet split-threshold ui-namespace]
    :or {split-threshold 1500
         stylesheet "dev/resources/public/styles.css"}}]
  (let [project-root (infer-project-root)  ;; Find tsain.edn, deps.edn, or git root
        phandaal-reg (phandaal/registry
                       {:project-root project-root
                        :source-paths ["src/clj" "dev/src/clj"]
                        :formatters {".css" (detect-css-formatter)}})]
    {::s/registries [phandaal-reg]
     ::s/interceptors [(css-hints-interceptor {:threshold split-threshold
                                                :stylesheet stylesheet})]
     ::s/actions (css-actions {:stylesheet stylesheet
                               :threshold split-threshold
                               :ui-namespace ui-namespace})
     ;; ... existing tsain effects
     }))
```

### CSS Formatter Detection

Formatting is optional with graceful degradation. Tsain detects available formatters at registry creation:

```clojure
(defn detect-css-formatter
  "Detect available CSS formatter, with JVM fallback."
  []
  (cond
    (sh-available? "prettier") (fmt/sh "prettier --write {path}")
    (sh-available? "biome")    (fmt/sh "biome format --write {path}")
    :else                      (fmt/basic-css)))  ;; Simple JVM normalizer

(defn basic-css-formatter
  "Basic JVM CSS normalizer - not full formatting, just readable.
   - Consistent newlines after { and before }
   - Trim trailing whitespace
   - Normalize indentation"
  [content]
  (-> content
      (str/replace #"\{\s*" "{\n  ")
      (str/replace #"\s*\}" "\n}")
      (str/replace #";\s*" ";\n  ")
      (str/trim)))
```

This approach:
- Uses prettier/biome if available (most frontend projects have these)
- Falls back to basic whitespace normalization for JVM-only environments
- Never blocks writes due to missing formatter

### CSS Actions Summary

| Action | Parser needed | Description |
|--------|--------------|-------------|
| `::tsain/write-css` | No | Append new styles to stylesheet |
| `::tsain/write-css-to` | No | Write to specific barrel file |
| `::tsain/replace-css` | Yes | Replace rules matching selector pattern |
| `::tsain/split-css` | Yes | Extract category rules to barrel file |

Simple appends don't need parsing. Operations that must understand document structure (replace, split) use jStyleParser for reliability.

### CSS Actions

#### `::tsain/write-css`

Append CSS content with category tracking. Categories are project-specific strings from `(tsain/categories)`:

```clojure
{:tsain/write-css
 {::s/description "Append CSS to stylesheet with threshold-aware hints"
  ::s/schema [:tuple
              [:= :tsain/write-css]
              :string  ;; content
              [:map {:optional true}
               [:category {:optional true} :string]  ;; Project-specific, from database
               [:comment {:optional true} :string]]] ;; Optional CSS comment header
  ::s/handler
  (fn [{:keys [stylesheet threshold]} content opts]
    (let [{:keys [category comment]} opts
          content' (if comment
                     (str "\n/* " comment " */\n" content)
                     content)]
      [[::phandaal/append
        {:path stylesheet
         :content content'
         :threshold threshold}
        ;; Metadata for interceptor
        {::tsain/category category}]]))}}
```

#### `::tsain/write-css-file`

Write to a specific CSS file (for barrel imports):

```clojure
{:tsain/write-css-file
 {::s/description "Write CSS to a specific file (for barrel splits)"
  ::s/schema [:tuple
              [:= :tsain/write-css-file]
              [:map
               [:path :string]
               [:content :string]
               [:create-dirs? {:optional true} :boolean]]]
  ::s/handler
  (fn [{:keys [threshold]} {:keys [path content create-dirs?]}]
    [[::phandaal/write
      {:path path
       :content content
       :create-dirs? (or create-dirs? true)
       :threshold threshold}]])}}
```

### CSS Parser Integration

For operations that require understanding CSS structure (replace, split), we use jStyleParser - a pure Java CSS parser.

```clojure
;; deps.edn
{:deps {net.sf.cssbox/jstyleparser {:mvn/version "4.0.0"}}}
```

Wrapper functions in `ascolais.tsain.css`:

```clojure
(ns ascolais.tsain.css
  (:import [cz.vutbr.web.css CSSFactory]))

(defn parse-stylesheet
  "Parse CSS string to jStyleParser StyleSheet."
  [css-string]
  (CSSFactory/parseString css-string nil))

(defn find-rules-by-pattern
  "Find all rules where any selector matches the pattern.
   Pattern '.card' matches '.card', '.card-header', '.card:hover', etc."
  [stylesheet pattern]
  ;; Implementation filters rules by selector prefix
  ...)

(defn remove-rules
  "Remove rules from stylesheet, return [remaining-stylesheet removed-rules]."
  [stylesheet rules-to-remove]
  ...)

(defn serialize
  "Convert StyleSheet back to CSS string."
  [stylesheet]
  (.toString stylesheet))
```

#### `::tsain/replace-css`

Replace existing CSS rules by selector pattern. Uses jStyleParser for reliable matching:

```clojure
{:tsain/replace-css
 {::s/description "Replace CSS rules matching selector pattern"
  ::s/schema [:tuple
              [:= :tsain/replace-css]
              :string  ;; selector pattern (e.g., ".game-card")
              :string  ;; new CSS content
              [:map {:optional true}
               [:path {:optional true} :string]]]  ;; defaults to main stylesheet
  ::s/handler
  (fn [{:keys [stylesheet]} pattern new-content opts]
    (let [path (or (:path opts) stylesheet)]
      ;; This is an effect - needs to read, parse, transform, write
      ;; 1. Read current stylesheet
      ;; 2. Parse with jStyleParser
      ;; 3. Find rules matching pattern
      ;; 4. Remove matched rules
      ;; 5. Parse new content
      ;; 6. Insert at original position (or end)
      ;; 7. Serialize and write via phandaal
      ...))}}
```

**Pattern matching behavior:**
- `".game-card"` matches `.game-card`, `.game-card-header`, `.game-card:hover`, `.game-card > *`
- Matches any selector that starts with the pattern
- This allows replacing all styles for a component family in one operation

**Example usage:**
```clojure
;; Replace all game-card styles
(dispatch [[::tsain/replace-css ".game-card"
            ".game-card { background: var(--bg-primary); }
             .game-card-header { display: flex; }
             .game-card:hover { transform: scale(1.02); }"]])

;; Result includes what was replaced
;; => {:replaced [".game-card" ".game-card-title"]
;;    :loc {:before 1200 :after 1180 :delta -20}
;;    ...}
```

#### `::tsain/split-css`

Extract styles matching a category to a barrel file. Uses jStyleParser for reliable extraction:

```clojure
{:tsain/split-css
 {::s/description "Extract category styles to barrel import file"
  ::s/schema [:tuple
              [:= :tsain/split-css]
              :string  ;; category name
              [:map {:optional true}
               [:patterns {:optional true} [:vector :string]]]]  ;; custom selector patterns
  ::s/handler
  (fn [{:keys [stylesheet]} category opts]
    ;; 1. Read current stylesheet
    ;; 2. Parse with jStyleParser
    ;; 3. Find rules matching category (e.g., "cards" → ".card*")
    ;; 4. Extract matched rules to new content
    ;; 5. Remove from source stylesheet
    ;; 6. Write extracted to components/<category>.css
    ;; 7. Add @import to source (if not present)
    ;; 8. Serialize and write source via phandaal
    ...)}}
```

**Category to pattern mapping:**
- `"cards"` → matches `.card*`, `.cards*`
- `"controls"` → matches `.btn*`, `.input*`, `.select*`, `.toggle*`
- Custom patterns can override via `:patterns` option

**Example usage:**
```clojure
;; Split all card-related styles to components/cards.css
(dispatch [[::tsain/split-css "cards"]])

;; With custom patterns
(dispatch [[::tsain/split-css "game" {:patterns [".game-card" ".player-hud"]}]])
```

### Hints Interceptor

The interceptor inspects phandaal results and adds tsain-specific hints. Categories come from effect metadata or can be looked up from the database:

```clojure
(defn css-hints-interceptor
  [{:keys [threshold stylesheet]}]
  {:id ::css-hints
   :after
   (fn [ctx]
     (let [results (get-in ctx [:response :results])]
       (update-in ctx [:response :results]
         (fn [results]
           (mapv (fn [{:keys [effect res] :as result}]
                   (if (and (= (first effect) ::phandaal/append)
                            (= (:path (second effect)) stylesheet)
                            (get-in res [:threshold :exceeded?]))
                     (let [category (get-in effect [2 ::tsain/category])  ;; From effect metadata
                           target (when category
                                    (str "components/" category ".css"))]
                       (update res :hints conj
                         {:type :split-suggested
                          :severity :warning
                          :category category
                          :target target
                          :loc (:loc res)
                          :message (cond
                                     category
                                     (format "Stylesheet exceeds %d lines. Extract %s styles to %s"
                                             threshold category target)

                                     :else
                                     (format "Stylesheet exceeds %d lines. Consider categorizing and splitting."
                                             threshold))
                          :action (when category
                                    {:effect :tsain/split-css
                                     :args [category]})}))
                     result))
                 results)))))})
```

### Result Shape

After a `::tsain/write-css` dispatch, Claude receives:

```clojure
;; Under threshold - clean result
{:path "/project/dev/resources/public/styles.css"
 :status :ok
 :loc {:before 450 :after 490 :delta 40}
 :threshold {:limit 1500 :exceeded? false :remaining 1010}
 :hints []
 :formatted? true}

;; Over threshold - actionable hints
{:path "/project/dev/resources/public/styles.css"
 :status :ok
 :loc {:before 1480 :after 1520 :delta 40}
 :threshold {:limit 1500 :exceeded? true :remaining -20}
 :hints [{:type :split-suggested
          :severity :warning
          :category "cards"
          :target "components/cards.css"
          :loc {:before 1480 :after 1520 :delta 40}
          :message "Stylesheet exceeds 1500 lines. Extract cards styles to components/cards.css"
          :action {:effect :tsain/split-css
                   :args ["cards"]}}]
 :formatted? true}
```

### Namespace Actions (Future Extension)

The same pattern applies to Clojure namespace management:

```clojure
;; Write component definition with threshold tracking
{:tsain/write-component
 {::s/description "Write defelem to UI namespace with threshold hints"
  ::s/schema [:tuple
              [:= :tsain/write-component]
              :keyword     ;; component name
              :string      ;; definition code
              [:map {:optional true}
               [:category {:optional true} :string]]]
  ::s/handler
  (fn [{:keys [ui-namespace threshold]} name definition opts]
    (let [ns-path (namespace->path ui-namespace)
          category (:category opts)]
      [[::phandaal/append
        {:path ns-path
         :content (str "\n" definition "\n")
         :threshold threshold}
        {::tsain/component-name name
         ::tsain/category category}]]))}}
```

### Configuration in tsain.edn

Minimal configuration - project root is inferred, categories come from the database:

```clojure
{:ui-namespace sandbox.ui
 :database-file "tsain.db"
 :stylesheet "dev/resources/public/styles.css"  ;; Optional, has convention default
 :split-threshold 1500}                          ;; Optional, defaults to 1500
```

Categories are project-specific and stored in SQLite. Query with `(tsain/categories)` to see existing categories. Claude infers appropriate categories based on component names and docs.

## Workflow Example

### Before (Text-Based)

```
1. Claude writes CSS with Bash echo >> styles.css
2. Claude may or may not check wc -l
3. SKILL.md says "split at 1500" but Claude might miss it
4. File grows to 2000+ lines before anyone notices
```

### After (Effect-Based)

```clojure
;; 1. Claude dispatches CSS write
(dispatch [[::tsain/write-css
            ".game-card { ... }"
            {:category "cards" :comment "Game card component"}]])

;; 2. Result includes threshold status
;; => {:loc {:after 1520} :threshold {:exceeded? true} :hints [...]}

;; 3. Claude sees the hint in the result
;; => {:message "Extract cards styles to components/cards.css"
;;    :action {:effect :tsain/split-css :args ["cards"]}}

;; 4. Claude dispatches the suggested action
(dispatch [[::tsain/split-css "cards"]])

;; 5. CSS is automatically organized
```

## Open Questions

- [x] Should hints include the exact CSS selectors to extract? → No, categories are sufficient granularity
- [ ] How to handle CSS that doesn't fit a category? → Allow `:category nil` with generic hint
- [ ] Should `::tsain/split-css` be fully automatic or require pattern hints? → Start automatic, add patterns later if needed
- [ ] Integration with existing `::tsain/commit` workflow? → Commit should check hints and warn before proceeding
- [ ] Should phandaal be a required or optional dependency? → Required for new installs, graceful degradation for existing

## References

- [Phandaal README](https://github.com/brianium/phandaal) - Effect library for file modifications
- [Sandestin](https://github.com/brianium/sandestin) - Effect dispatch system
- [Tsain SKILL.md](/.claude/skills/tsain/SKILL.md) - Current file size management section
- [CSS-in-JS to CSS extraction patterns](https://css-tricks.com/) - Prior art on style organization
