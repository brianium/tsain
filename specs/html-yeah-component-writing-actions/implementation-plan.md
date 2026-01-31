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

- [x] Implement `detect-clj-formatter` function in `ascolais.tsain.clj` (new namespace)
  - Try-require `cljfmt.core` to check availability
  - If available, return formatter fn that calls `cljfmt.core/reformat-string`
  - If unavailable, return `identity` (no-op)
- [x] Implement `load-cljfmt-config` function
  - Look for `.cljfmt.edn` in project root
  - Fall back to cljfmt defaults if not found
- [x] Update `create-phandaal-registry` in `sandbox/app.clj`
  - Add `.clj` and `.cljc` to formatters map
  ```clojure
  {:formatters {".css" css-formatter
                ".clj" clj-formatter
                ".cljc" clj-formatter}}
  ```
- [x] Add cljfmt to tsain's own `:dev` deps for testing (already present)
- [ ] Test: write a .clj file via phandaal, verify formatting applied

## Phase 3: `::tsain/write-component` Action

- [x] Create `infer-category-from-component-name` function in `ascolais.tsain.clj`
  - Map patterns like `*-card` → "cards", `*-btn` → "controls"
  - Return nil for unknown patterns (requires explicit category)
- [x] Define `::tsain/write-component` action
  - Schema: `[:tuple [:= ::write-component] :string [:map {:optional true} [:category {:optional true} :string]]]`
  - Parse component name from defelem form (regex for `(hy/defelem NAME`)
  - Infer category if not provided
  - Return `[[::phandaal/append {:path ui-namespace-path :content code}]]`
- [x] Add action to tsain registry
- [x] Test via REPL - works and formats with cljfmt

## Phase 4: Component Hints Interceptor

- [x] Create `component-hints-interceptor`
  - Filter for phandaal writes to `.clj` files in UI namespace path
  - Check `:threshold :exceeded?`
  - Extract category from effect args
  - Build hint with suggested `::tsain/split-namespace` action
- [x] Add interceptor to registry composition
- [ ] Test threshold detection (when file exceeds limit)

## Phase 5: `::tsain/split-namespace` Effect

- [x] Create `find-defelem-forms` function in `ascolais.tsain.clj`
  - Uses paren matching to find complete forms
  - Returns list of `{:name :start-line :end-line :content}`
- [x] Create `patterns-for-category` for components
  - Maps categories to suffix patterns
- [x] Create `filter-forms-by-category`, `generate-sub-namespace`, `add-require-to-source`
- [x] Define `::tsain/split-namespace` effect
  - Reads UI namespace file
  - Finds forms matching category patterns
  - Extracts to new namespace file with proper `(ns ...)` declaration
  - Adds `(:require [ui-ns.category])` to parent
  - Writes both files via phandaal
- [x] Handle edge cases:
  - Require already exists → skip adding duplicate
  - No matching forms → return warning, no changes
- [x] Tested: successfully extracted "display" category (badge, status-indicator)

## Phase 6: Update SKILL.md

- [x] Add "Component Write Effects" section (parallel to CSS Write Effects)
  - Document `::tsain/write-component` with examples
  - Document `::tsain/split-namespace` with examples
  - Explain hint structure and category inference
- [x] Update "Step 4" in Alias-First Workflow to use effects
  - Now references effect-based writes instead of manual file editing
- [x] Removed manual `wc -l` checking instructions from Step 4

## Phase 7: Update CLAUDE.md

- [x] Add "Component Authoring via Effects (Required)" section
  - Parallel structure to "CSS Authoring via Effects"
  - Mandate using `::tsain/write-component` for all component writes
  - Never use Edit/Write tools directly on UI namespace
  - Category inference table
  - Hint structure and split response
- [x] Placed between CSS section and Development Workflow

## Phase 8: Testing & Polish

- [x] Tested category inference (game-card -> "cards", action-btn -> "controls", etc.)
- [x] Tested defelem form extraction (found 21 forms in sandbox/ui.clj)
- [x] Integration test: full write → hint → split flow tested with "display" category
- [x] Verified reload works after phandaal writes (clj-reload triggers correctly)
- [ ] Unit tests in test namespace (future work)

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
