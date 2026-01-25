# Barrel Imports In Skill - Implementation Plan

## Overview

Add file-splitting guidance to the tsain skill so Claude proactively suggests barrel imports when CSS or component files grow large. This involves updating skill documentation, adding configuration options, and establishing the category-based split pattern as a portable convention.

## Prerequisites

- [ ] Review current tsain skill structure at `.claude/skills/tsain/`
- [ ] Review current `tsain.edn` configuration options
- [ ] Identify where in the skill workflow splits should be suggested

## Phase 1: Define the Pattern Documentation

Document the barrel import pattern so it can be referenced by the skill and copied to project CLAUDE.md files.

- [ ] Write CSS barrel import pattern documentation
  - Directory structure for split files
  - @import syntax and ordering
  - When to split (threshold)
  - Category naming conventions
- [ ] Write Clojure namespace barrel pattern documentation
  - Directory structure for sub-namespaces
  - Require syntax for alias registration
  - File naming (kebab to snake_case)
  - Category conventions matching CSS
- [ ] Define the category taxonomy
  - Default categories with descriptions
  - Guidelines for adding new categories
  - Examples of what belongs where

## Phase 2: Update tsain.edn Configuration

Add configuration options for split behavior.

- [ ] Add `:split-threshold` option (default 1500, nil to disable)
- [ ] Add `:css-split-dir` option (default "components")
- [ ] Add `:ns-split-dir` option (default "ui")
- [ ] Document new options in tsain.edn comments
- [ ] Update skill to read and respect these options

**Example tsain.edn addition:**
```clojure
{:database-file "tsain.db"
 :ui-namespace 'sandbox.ui

 ;; Barrel import configuration
 :split-threshold 1500        ;; lines before suggesting split (nil to disable)
 :css-split-dir "components"  ;; subdirectory for split CSS files
 :ns-split-dir "ui"           ;; subdirectory for split namespaces
 :default-categories ["cards" "controls" "layout" "feedback" "display"]}
```

## Phase 3: Update Skill Instructions

Modify the tsain skill to include barrel import guidance.

- [ ] Add "File Size Management" section to skill prompt
  - When to check file sizes (before commit)
  - How to suggest splits
  - How to execute splits step-by-step
- [ ] Add split workflow to `/tsain iterate` flow
  - Check current file sizes
  - If approaching threshold, suggest split before continuing
  - Guide category selection for new components
- [ ] Add split guidance to `/tsain implement` flow
  - Consider file organization during implementation
  - Suggest splits when adding multiple components
- [ ] Create example split scenarios in skill docs

**Skill instruction snippet:**
```markdown
## File Size Management

Before committing components, check if files are approaching the split threshold
(default: 1500 lines, configured in tsain.edn as `:split-threshold`).

### When to Split

- `styles.css` exceeds threshold → split into `components/<category>.css`
- `ui.clj` exceeds threshold → split into `ui/<category>.clj`

### CSS Split Procedure

1. Create `dev/resources/public/components/` directory
2. Move related styles to `components/<category>.css`
3. Add `@import "./components/<category>.css";` to main `styles.css`
4. Verify hot-reload still works

### Namespace Split Procedure

1. Create `dev/src/clj/sandbox/ui/` directory
2. Create `sandbox.ui.<category>` namespace
3. Move `defelem` definitions to the new namespace
4. Add `(:require [sandbox.ui.<category>])` to main `sandbox.ui`
5. Verify aliases still resolve
```

## Phase 4: Update CLAUDE.md Template

Add barrel import conventions to the CLAUDE.md section that tsain projects use.

- [ ] Add "File Organization" section to project CLAUDE.md conventions
- [ ] Document the split threshold and how to configure it
- [ ] Include examples of split file structures
- [ ] Reference skill for split execution

**CLAUDE.md addition:**
```markdown
## File Organization

### Split Thresholds

When files exceed ~1500 lines, split them using barrel imports:

| File | Split To | Import Style |
|------|----------|--------------|
| `styles.css` | `components/<category>.css` | `@import "./components/cards.css";` |
| `ui.clj` | `ui/<category>.clj` | `(:require [sandbox.ui.cards])` |

### Categories

Organize splits by semantic category:
- `cards` - Card-based layouts, tiles, panels
- `controls` - Buttons, inputs, selects, toggles
- `layout` - Grids, containers, spacing
- `feedback` - Toasts, alerts, loaders
- `display` - Text, badges, avatars

Configure threshold in `tsain.edn`:
```clojure
{:split-threshold 1500}  ;; nil to disable
```
```

## Phase 5: Testing & Validation

- [ ] Test split workflow on a project with large files
- [ ] Verify CSS @imports work with hot-reload
- [ ] Verify namespace requires register aliases correctly
- [ ] Test skill prompts suggest splits at appropriate times
- [ ] Validate configuration options are respected

## Phase 6: Documentation & Cleanup

- [ ] Update main tsain README with file organization section
- [ ] Add migration notes for existing large projects
- [ ] Review and clean up skill prompt language
- [ ] Final review of all documentation

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
