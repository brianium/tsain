# Sandbox UI Enhancements - Implementation Plan

## Overview

Implement category grouping, tabbed metadata views, and commit form enhancements. All changes are in sandbox views and CSS.

## Prerequisites

- [x] html.yeah defelem metadata available via `hy/element`
- [x] tsain discovery API: `categories`, `by-category`, `describe`
- [x] Datastar signals working in sandbox

## Phase 1: Sidebar Category Grouping

- [x] Add `grouped-components` helper that returns `{category -> [components]}` (commit: 2f7dcae)
- [x] Update sidebar view to render collapsible category sections (commit: 2f7dcae)
- [x] Add CSS for category headers and collapse indicators (commit: 2f7dcae)
- [x] Handle "Other" category for uncategorized components (commit: 2f7dcae)
- [x] Add signals for collapse state: `$sidebarState` (commit: 2f7dcae)

## Phase 2: Component View Tabs

- [x] Add tab bar to component view header (Preview | Code | Props) (commit: 8b651a7)
- [x] Add signal for active tab: `$activeTab` (commit: 8b651a7)
- [x] Implement Preview tab (existing component render) (commit: 8b651a7)
- [x] Implement Code tab: (commit: 8b651a7)
  - [x] Format hiccup for display (pprint or custom formatter)
  - [x] Move copy button here
  - [x] Move delete button here
- [x] Implement Props tab: (commit: 8b651a7)
  - [x] Add `extract-props` function for malli schema parsing
  - [x] Render props table (Name, Type, Required columns)
  - [x] Handle enum types (show values)
  - [x] Handle complex types (show "complex" or simplified)
- [x] Add description line under component name in header (commit: 8b651a7)
- [x] Add CSS for tabs (match cyberpunk aesthetic) (commit: 8b651a7)

## Phase 3: Category Selection on Commit

- [x] Update commit form to include category select (commit: cf38016)
- [x] Populate select with existing categories from `tsain/categories` (commit: cf38016)
- [x] Add "+ New category..." option that reveals text input (commit: cf38016)
- [x] Wire up signals: `$commitCategory`, `$newCategory`, `$showNewCategory` (commit: cf38016)
- [x] Pass category to `::tsain/commit` effect (commit: cf38016)
- [x] Update `::tsain/commit` handler to accept category parameter (commit: cf38016)

## Phase 4: localStorage Persistence

- [x] Add `data-on-load` to initialize `$sidebarState` from localStorage (commit: c8e943a)
- [x] Add `data-effect` to persist `$sidebarState` changes (commit: c8e943a)
- [x] Test persistence across page reloads (commit: c8e943a)
- [x] Handle missing/corrupt localStorage gracefully (commit: c8e943a)

## Phase 5: Testing & Polish

- [x] Test tab switching on all components
- [x] Test category grouping with various component sets
- [x] Test commit flow with new/existing categories
- [x] Test localStorage persistence
- [x] Verify hot reload still works
- [x] Visual polish and consistency check

## Rollout Plan

1. Implement phases incrementally, commit after each
2. Test in sandbox after each phase
3. Update CLAUDE.md if new patterns emerge

## Rollback Plan

All changes are additive to views/CSS. To rollback:
1. Revert view changes in `sandbox/views.clj`
2. Revert CSS additions
3. Sidebar and component view return to previous behavior
