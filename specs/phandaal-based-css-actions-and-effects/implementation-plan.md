# Phandaal Based CSS Actions And Effects - Implementation Plan

## Overview

Step-by-step implementation tasks. Update checkboxes and add commit hashes as you progress.

## Prerequisites

- [x] Phandaal v0.1.0+ available (already in deps.edn)
- [x] Sandestin v0.6.0+ (for interceptor composition)
- [ ] Existing tsain registry functional
- [ ] Add jStyleParser dependency for CSS parsing:
  ```clojure
  net.sf.cssbox/jstyleparser {:mvn/version "4.0.0"}
  ```

## Phase 1: Registry Composition

No dependency or config changes needed - phandaal is already installed and project root is inferable.

- [ ] Implement `detect-css-formatter` function
  - Check for prettier via `which prettier` or similar
  - Check for biome as fallback
  - Return basic JVM normalizer if neither available
- [ ] Implement `basic-css-formatter` (JVM fallback)
  - Normalize newlines around `{` and `}`
  - Trim trailing whitespace
  - Basic indentation normalization
- [ ] Implement `infer-project-root` function
  - Walk up from cwd looking for tsain.edn
  - Fall back to deps.edn / project.clj location
  - Fall back to git root
- [ ] Update `tsain/registry` to compose phandaal registry
  - Use inferred project root
  - Use detected CSS formatter
  - Pass stylesheet path (convention: `dev/resources/public/styles.css`)
- [ ] Verify dispatch includes phandaal effects: `(s/grep (dispatch) "phandaal")`

## Phase 2: CSS Write Actions

Goal: Pure sandestin actions that return phandaal effect vectors. Actions are pure functions; phandaal effects handle the actual I/O.

- [ ] Define `::tsain/write-css` action
  - Schema: `[:tuple [:= ::tsain/write-css] :string]` (content only)
  - Optional second arg: `[:map [:category {:optional true} :string] [:comment {:optional true} :string]]`
  - Returns: `[[::phandaal/append-css {:content content :path stylesheet-path}]]`
  - Category tracked via effect metadata for hints interceptor
- [ ] Define `::tsain/write-css-to` action (for barrel files)
  - Schema: `[:tuple [:= ::tsain/write-css-to] :string :string]` (path, content)
  - Returns: `[[::phandaal/write-css {:content content :path path}]]`
- [ ] Add actions to tsain registry
- [ ] Test via REPL:
  ```clojure
  (dispatch [[::tsain/write-css ".test { color: red; }"]])
  (dispatch [[::tsain/write-css ".card { ... }" {:category "cards"}]])
  ```
- [ ] Verify phandaal result includes `:loc` and `:threshold` keys

## Phase 3: Hints Interceptor

- [ ] Create `css-hints-interceptor` function
  - Accepts: `{:threshold int}` (stylesheet path already known from registry)
  - Returns: interceptor with `:after` handler
- [ ] Interceptor logic:
  - Filter results for phandaal CSS appends
  - Check if `:threshold :exceeded?` is true
  - Extract category from effect metadata (or infer from `(tsain/categories)`)
  - Build hint with `:type`, `:category`, `:target`, `:message`, `:action`
  - Hint includes suggested `::tsain/split-css` invocation
  - Append hint to result's `:hints` vector
- [ ] Add interceptor to tsain registry composition
- [ ] Test threshold detection:
  ```clojure
  ;; Set low threshold for testing
  (dispatch [[::tsain/write-css (apply str (repeat 2000 "x")) {:category "cards"}]])
  ;; Verify :hints contains split suggestion
  ```

## Phase 4: CSS Parser Integration

jStyleParser provides reliable CSS manipulation for replace and split operations.

- [ ] Add jStyleParser wrapper namespace (`ascolais.tsain.css`)
  - `parse-stylesheet` - parse CSS string to jStyleParser StyleSheet
  - `find-rules-by-pattern` - find rules where selector matches pattern (e.g., `.game-card*`)
  - `remove-rules` - remove rules from stylesheet by selector
  - `insert-rules` - add parsed rules at position
  - `serialize` - convert StyleSheet back to CSS string
- [ ] Test parser functions:
  ```clojure
  (let [css (parse-stylesheet ".card { color: red; } .card-header { font-size: 16px; }")
        rules (find-rules-by-pattern css ".card")]
    (count rules)) ;; => 2
  ```

## Phase 5: Replace CSS Effect

- [ ] Define `::tsain/replace-css` effect
  - Schema: `[:tuple [:= ::tsain/replace-css] :string :string]` (selector-pattern, new-content)
  - Optional third arg: `[:map [:path {:optional true} :string]]` (defaults to main stylesheet)
- [ ] Implementation steps:
  1. Read current stylesheet
  2. Parse with jStyleParser
  3. Find all rules matching selector pattern (e.g., `.game-card` matches `.game-card`, `.game-card-header`, `.game-card:hover`)
  4. Remove matched rules
  5. Parse new content
  6. Insert new rules at original position (or end if none existed)
  7. Serialize and write via phandaal
- [ ] Test via REPL:
  ```clojure
  ;; Replace all game-card related styles
  (dispatch [[::tsain/replace-css ".game-card"
              ".game-card { background: blue; }
               .game-card-header { font-weight: bold; }"]])
  ```
- [ ] Return result includes which rules were replaced

## Phase 6: Split Effect

- [ ] Design `::tsain/split-css` effect (uses jStyleParser)
  - Schema: `[:tuple [:= ::tsain/split-css] :string]` (category name)
  - Optional second arg for custom selector patterns
- [ ] Implementation steps:
  1. Read current stylesheet
  2. Parse with jStyleParser
  3. Find rules matching category pattern (e.g., `"cards"` matches `.card*`, `.cards*`)
  4. Extract matched rules to new content string
  5. Remove matched rules from source stylesheet
  6. Write extracted content to `components/<category>.css`
  7. Add `@import "./components/<category>.css";` to source (if not present)
  8. Serialize and write source via phandaal
- [ ] Handle edge cases:
  - `@import` already exists → skip adding duplicate
  - No matching blocks → return warning, no changes
  - Target file exists → append to it
- [ ] Test full split workflow:
  ```clojure
  (dispatch [[::tsain/split-css "cards"]])
  ```

## Phase 8: Namespace Actions (Extension)

- [ ] Define `::tsain/write-component` action
  - Similar pattern to write-css but for Clojure files
  - Uses phandaal with `:reload` metadata
- [ ] Define `::tsain/split-namespace` effect
  - Extract components by category to sub-namespace
  - Update barrel requires in main UI namespace
- [ ] Add namespace hints interceptor
  - Triggered when UI namespace exceeds threshold

## Phase 9: Documentation & SKILL.md Update

- [ ] Update SKILL.md "Alias-First Workflow" to use new effects
- [ ] Remove manual `wc -l` instructions
- [ ] Add "CSS Write Effects" section with examples
- [ ] Document hint structure and how to respond to hints
- [ ] Add troubleshooting for common issues

## Phase 10: Testing & Polish

- [ ] Write unit tests for interceptor hint generation
- [ ] Write integration test for full write → hint → split flow
- [ ] Test with real component development session
- [ ] Verify hot-reload still works after phandaal writes
- [ ] Test formatter integration (prettier for CSS)

## Rollout Plan

1. Implement phases 1-3 (write actions with hints)
2. Test in tsain development for 1-2 sessions
3. Implement phase 4 (split action) based on real usage
4. Update SKILL.md (phase 6)
5. Namespace actions (phase 5) as follow-up spec

## Rollback Plan

If issues arise:
1. Phandaal effects are additive - existing dispatch still works
2. Remove phandaal from registry composition to revert to manual workflow
3. SKILL.md changes are documentation-only, easily reverted
