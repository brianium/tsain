# Barrel Imports In Skill - Implementation Plan

## Overview

Add file-splitting guidance to the tsain skill so Claude proactively suggests barrel imports when CSS or component files grow large. This involves updating skill documentation, adding configuration options, and establishing the category-based split pattern as a portable convention.

## Prerequisites

- [x] Review current tsain skill structure at `.claude/skills/tsain/`
- [x] Review current `tsain.edn` configuration options
- [x] Identify where in the skill workflow splits should be suggested

## Phase 1: Define the Pattern Documentation

Document the barrel import pattern so it can be referenced by the skill and copied to project CLAUDE.md files.

- [x] Write CSS barrel import pattern documentation
  - Directory structure for split files
  - @import syntax and ordering
  - When to split (threshold)
  - Category naming conventions
- [x] Write Clojure namespace barrel pattern documentation
  - Directory structure for sub-namespaces
  - Require syntax for alias registration
  - File naming (kebab to snake_case)
  - Category conventions matching CSS
- [x] Define the category taxonomy
  - Default categories with descriptions
  - Guidelines for adding new categories
  - Examples of what belongs where

## Phase 2: Update tsain.edn Configuration

Add minimal configuration - paths are derived by convention.

- [x] Add `:split-threshold` option (default 1500, nil to disable)
- [x] Document conventions in skill (not config):
  - CSS splits always go to `components/` subdirectory
  - Namespace splits derive from `:ui-namespace` (e.g., `sandbox.ui` → `sandbox/ui/`)
- [x] Update skill to read threshold option

**Example tsain.edn addition:**
```clojure
{:database-file "tsain.db"
 :ui-namespace 'sandbox.ui

 ;; Barrel import configuration (paths are convention, not config)
 :split-threshold 1500}  ;; lines before suggesting split (nil to disable)
```

**Conventions (not configurable):**
- CSS: `styles.css` → `components/<category>.css`
- Clojure: `sandbox.ui` → `sandbox/ui/<category>.clj` (standard namespace path rules)

## Phase 3: Update Skill Instructions

Modify the tsain skill to include barrel import guidance.

- [x] Add "File Size Management" section to skill prompt
  - When to check file sizes (before commit)
  - How to suggest splits
  - How to execute splits step-by-step
- [x] Add split workflow to `/tsain iterate` flow
  - Check current file sizes
  - If approaching threshold, suggest split before continuing
  - Guide category selection for new components
- [x] Add split guidance to `/tsain implement` flow
  - Consider file organization during implementation
  - Suggest splits when adding multiple components
- [x] Create example split scenarios in skill docs

**Skill instruction snippet:**
```markdown
## File Size Management

Before committing components, check if files are approaching the split threshold
(default: 1500 lines, configured in tsain.edn as `:split-threshold`).

### Conventions

These paths are conventions, not configurable:
- CSS splits → `components/` subdirectory (relative to main stylesheet)
- Namespace splits → standard Clojure path from `:ui-namespace`

### When to Split

- `styles.css` exceeds threshold → split into `components/<category>.css`
- UI namespace exceeds threshold → split into sub-namespaces by category

### CSS Split Procedure

1. Create `components/` directory next to main stylesheet
2. Move related styles to `components/<category>.css`
3. Add `@import "./components/<category>.css";` to main stylesheet
4. Verify hot-reload still works

### Namespace Split Procedure

Given `:ui-namespace 'sandbox.ui`:

1. Create directory matching namespace path (e.g., `sandbox/ui/`)
2. Create `sandbox.ui.<category>` namespace in `sandbox/ui/<category>.clj`
3. Move `defelem` definitions to the new namespace
4. Add `(:require [sandbox.ui.<category>])` to main UI namespace
5. Verify aliases still resolve
```

## Phase 4: Update CLAUDE.md Template

Add barrel import conventions to the CLAUDE.md section that tsain projects use.

- [x] Add "File Organization" section to project CLAUDE.md conventions
- [x] Document the split threshold and how to configure it
- [x] Include examples of split file structures
- [x] Reference skill for split execution

**CLAUDE.md addition:**
```markdown
## File Organization

### Split Thresholds

When files exceed ~1500 lines, split them using barrel imports.

**Conventions (not configurable):**

| File | Split To | Import Style |
|------|----------|--------------|
| Main stylesheet | `components/<category>.css` | `@import "./components/cards.css";` |
| UI namespace | Sub-namespace by category | `(:require [<ui-ns>.cards])` |

### Categories

Organize splits by semantic category:
- `cards` - Card-based layouts, tiles, panels
- `controls` - Buttons, inputs, selects, toggles
- `layout` - Grids, containers, spacing
- `feedback` - Toasts, alerts, loaders
- `display` - Text, badges, avatars

### Configuration

Only the threshold is configurable in `tsain.edn`:
```clojure
{:split-threshold 1500}  ;; nil to disable checking
```

Paths are derived by convention - CSS always splits to `components/`,
namespaces split according to Clojure's standard namespace-to-path rules.
```

## Phase 5: Testing & Validation

- [x] Test split workflow on a project with large files
  - Verified tsain project has CSS at 2895 lines (above threshold)
  - UI namespace at 495 lines (below threshold, but can grow)
- [x] Verify CSS @imports work with hot-reload
  - Standard CSS @import is supported by browser
  - Hot-reload watches the stylesheet directory
- [x] Verify namespace requires register aliases correctly
  - html.yeah `defelem` auto-registers aliases when namespace is required
- [x] Test skill prompts suggest splits at appropriate times
  - Added Step 4 to iterate workflow for file size checking
  - Added step to implement workflow
- [x] Validate configuration options are respected
  - `:split-threshold` added to tsain.edn

## Phase 6: Documentation & Cleanup

- [x] Update main tsain README with file organization section
  - Added to CLAUDE.md in Chassis Alias Conventions section
- [x] Add migration notes for existing large projects
  - Skill documents full split procedures
- [x] Review and clean up skill prompt language
  - Reviewed File Size Management section
- [x] Final review of all documentation
  - SKILL.md, CLAUDE.md, tsain.edn all updated

## Rollout Plan

1. Implement in tsain's own `.claude/skills/tsain/` first
2. Test on tsain project itself (dogfooding)
3. Update ascolais-template to include new conventions
4. Apply to other ascolais projects as needed

## Rollback Plan

If issues arise:
1. Configuration is opt-in, so existing projects unaffected
2. Remove `:split-threshold` from tsain.edn to disable
3. Barrel imports are valid CSS/Clojure, so split files continue working even without skill guidance
