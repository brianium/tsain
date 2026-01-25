# Sandbox UI Enhancements - Implementation Plan

## Overview

Implement category grouping, tabbed metadata views, and commit form enhancements. All changes are in sandbox views and CSS.

## Prerequisites

- [x] html.yeah defelem metadata available via `hy/element`
- [x] tsain discovery API: `categories`, `by-category`, `describe`
- [x] Datastar signals working in sandbox

## Phase 1: Sidebar Category Grouping

- [ ] Add `grouped-components` helper that returns `{category -> [components]}`
- [ ] Update sidebar view to render collapsible category sections
- [ ] Add CSS for category headers and collapse indicators
- [ ] Handle "Other" category for uncategorized components
- [ ] Add signals for collapse state: `$sidebarState`

## Phase 2: Component View Tabs

- [ ] Add tab bar to component view header (Preview | Code | Props)
- [ ] Add signal for active tab: `$activeTab`
- [ ] Implement Preview tab (existing component render)
- [ ] Implement Code tab:
  - [ ] Format hiccup for display (pprint or custom formatter)
  - [ ] Move copy button here
  - [ ] Move delete button here
- [ ] Implement Props tab:
  - [ ] Add `extract-props` function for malli schema parsing
  - [ ] Render props table (Name, Type, Required columns)
  - [ ] Handle enum types (show values)
  - [ ] Handle complex types (show "complex" or simplified)
- [ ] Add description line under component name in header
- [ ] Add CSS for tabs (match cyberpunk aesthetic)

## Phase 3: Category Selection on Commit

- [ ] Update commit form to include category select
- [ ] Populate select with existing categories from `tsain/categories`
- [ ] Add "+ New category..." option that reveals text input
- [ ] Wire up signals: `$commitCategory`, `$newCategory`, `$showNewCategory`
- [ ] Pass category to `::tsain/commit` effect
- [ ] Update `::tsain/commit` handler to accept category parameter

## Phase 4: localStorage Persistence

- [ ] Add `data-on-load` to initialize `$sidebarState` from localStorage
- [ ] Add `data-effect` to persist `$sidebarState` changes
- [ ] Test persistence across page reloads
- [ ] Handle missing/corrupt localStorage gracefully

## Phase 5: Testing & Polish

- [ ] Test tab switching on all components
- [ ] Test category grouping with various component sets
- [ ] Test commit flow with new/existing categories
- [ ] Test localStorage persistence
- [ ] Verify hot reload still works
- [ ] Visual polish and consistency check

## Rollout Plan

1. Implement phases incrementally, commit after each
2. Test in sandbox after each phase
3. Update CLAUDE.md if new patterns emerge

## Rollback Plan

All changes are additive to views/CSS. To rollback:
1. Revert view changes in `sandbox/views.clj`
2. Revert CSS additions
3. Sidebar and component view return to previous behavior
