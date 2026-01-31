# Html Yeah Component Writing Actions - Implementation Plan

## Overview

Step-by-step implementation tasks. Update checkboxes and add commit hashes as you progress.

## Prerequisites

- [x] Phandaal CSS actions working (provides pattern)
- [x] html.yeah integrated in sandbox
- [x] `tsain.edn` has `:ui-namespace` and `:split-threshold` config

## Phase 1: Research & Design

- [ ] Review existing CSS action implementation in `src/clj/ascolais/tsain.clj`
- [ ] Understand phandaal's Clojure file handling (namespace inference, reload tracking)
- [ ] Design category inference function (component name → category)
- [ ] Document the action/effect schemas in research.md

## Phase 2: Clojure Formatter Detection

- [ ] Implement `detect-clj-formatter` function in `ascolais.tsain.css` (or new ns)
  - Try-require `cljfmt.core` to check availability
  - If available, return formatter fn that calls `cljfmt.core/reformat-string`
  - If unavailable, return `identity` (no-op)
- [ ] Implement `load-cljfmt-config` function
  - Look for `.cljfmt.edn` in project root
  - Fall back to cljfmt defaults if not found
- [ ] Update `create-phandaal-registry` in `sandbox/app.clj`
  - Add `.clj` and `.cljc` to formatters map
  ```clojure
  {:formatters {".css" css-formatter
                ".clj" clj-formatter
                ".cljc" clj-formatter}}
  ```
- [ ] Add cljfmt to tsain's own `:dev` deps for testing
- [ ] Test: write a .clj file via phandaal, verify formatting applied

## Phase 3: `::tsain/write-component` Action

- [ ] Create `infer-category-from-component-name` function
  - Map patterns like `*-card` → "cards", `*-btn` → "controls"
  - Return nil for unknown patterns (requires explicit category)
- [ ] Define `::tsain/write-component` action
  - Schema: `[:tuple [:= ::write-component] :string [:map {:optional true} [:category {:optional true} :string]]]`
  - Parse component name from defelem form (regex for `(hy/defelem NAME`)
  - Infer category if not provided
  - Return `[[::phandaal/append {:path ui-namespace-path :content code}]]`
- [ ] Add action to tsain registry
- [ ] Test via REPL:
  ```clojure
  (dispatch [[::tsain/write-component "(hy/defelem test-card ...)" {:category "cards"}]])
  ```

## Phase 4: Component Hints Interceptor

- [ ] Extend `css-hints-interceptor` or create `component-hints-interceptor`
  - Filter for phandaal writes to `.clj` files in UI namespace path
  - Check `:threshold :exceeded?`
  - Extract category from effect args
  - Build hint with suggested `::tsain/split-namespace` action
- [ ] Add interceptor to registry composition
- [ ] Test threshold detection:
  ```clojure
  ;; With low threshold for testing
  (dispatch [[::tsain/write-component "(hy/defelem big-component ...)"]])
  ;; Verify :hints contains split suggestion
  ```

## Phase 5: `::tsain/split-namespace` Effect

- [ ] Create `find-defelem-forms` function
  - Read Clojure file
  - Find all `(hy/defelem NAME ...)` forms via regex
  - Return list of `{:name :start-line :end-line :content}`
- [ ] Create `patterns-for-category` for components (like CSS version)
  - "cards" → matches `card`, `cards`, `*-card`, `*-tile`, `*-panel`
  - "controls" → matches `btn`, `button`, `input`, `select`, `toggle`
- [ ] Define `::tsain/split-namespace` effect
  - Schema: `[:tuple [:= ::split-namespace] :string [:map {:optional true} [:patterns {:optional true} [:vector :string]]]]`
  - Read UI namespace file
  - Find forms matching category patterns
  - Extract to new namespace file with proper `(ns ...)` declaration
  - Add `(:require [ui-ns.category])` to parent if not present
  - Write both files via phandaal
- [ ] Handle edge cases:
  - Require already exists → skip adding duplicate
  - No matching forms → return warning, no changes
  - Sub-namespace already exists → append to it
- [ ] Test split:
  ```clojure
  (dispatch [[::tsain/split-namespace "cards"]])
  ;; Verify sandbox/ui/cards.clj created
  ;; Verify sandbox/ui.clj has require
  ```

## Phase 6: Update SKILL.md

- [ ] Remove "File Size Management" prose about manual `wc -l` checks
- [ ] Add "Component Writing Effects" section (parallel to CSS Write Effects)
  - Document `::tsain/write-component` with examples
  - Document `::tsain/split-namespace` with examples
  - Explain hint structure and how to respond
- [ ] Update "Alias-First Workflow" to use effects
  - Step 1: Define alias via `::tsain/write-component`
  - Step 2: Preview
  - Step 3: Add CSS via `::tsain/write-css`
  - Step 4: Commit to library
- [ ] Remove manual threshold checking instructions

## Phase 7: Update CLAUDE.md

- [ ] Add "Component Authoring via Effects (Required)" section
  - Parallel structure to "CSS Authoring via Effects"
  - Mandate using `::tsain/write-component` for all component writes
  - Never use Edit/Write tools directly on UI namespace
  - Always check results for hints and act on them
- [ ] Update Development Workflow to reference effect-based authoring

## Phase 8: Testing & Polish

- [ ] Unit tests for category inference
- [ ] Unit tests for defelem form extraction
- [ ] Integration test: full write → hint → split flow
- [ ] Verify reload works after phandaal writes
- [ ] Test with tsain as installed library (not just sandbox)

## Rollout Plan

1. Implement phases 1-5 (core functionality)
2. Test in tsain development for 1-2 sessions
3. Update documentation (phases 6-7)
4. Commit with feature flag if needed

## Rollback Plan

If issues arise:
1. Component effects are additive - existing manual workflow still works
2. Remove action/effect from registry to revert
3. SKILL.md/CLAUDE.md changes are documentation-only, easily reverted
