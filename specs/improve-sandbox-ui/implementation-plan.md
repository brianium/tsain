# Improve Sandbox UI - Implementation Plan

## Overview

Step-by-step implementation tasks. Update checkboxes and add commit hashes as you progress.

## Prerequisites

- [ ] Review current sandbox CSS structure
- [ ] Identify all buttons/controls that need icons

## Phase 1: Icons Infrastructure

- [ ] Create `dev/src/clj/sandbox/icons.clj` namespace
- [ ] Add Lucide icons as hiccup (copy SVG paths from lucide.dev):
  - [ ] `chevron-left`, `chevron-right` (navigation)
  - [ ] `chevrons-left`, `chevrons-right` (sidebar toggle)
  - [ ] `copy` (copy button)
  - [ ] `trash-2` (delete button)
  - [ ] `save` (commit button)
  - [ ] `x` (clear/close)
  - [ ] `search` (search input)
  - [ ] `palette` (color picker)
  - [ ] `check` (success feedback)
  - [ ] `eye` (preview tab)
  - [ ] `layout-grid` (components tab)
- [ ] Create icon helper function `(icon name & [opts])` for easy use
- [ ] Require icons namespace in `sandbox.views` or views file

## Phase 2: Apply Icons to UI

- [ ] Update navbar tabs (Preview, Components) with icons
- [ ] Update commit form button with save icon
- [ ] Update clear button with x icon
- [ ] Update sidebar toggle with chevrons icons
- [ ] Update component actions (copy, delete, back) with icons
- [ ] Update component navigation (prev/next) with chevron icons
- [ ] Style icon buttons in `sandbox.css`

## Phase 3: Sidebar Improvements

- [ ] Add `overflow-y: auto` to sidebar list
- [ ] Add max-height constraint or flex layout for scrolling
- [ ] Add search input at top of sidebar
- [ ] Add Datastar signal for search filter (`data-signals:searchQuery`)
- [ ] Add `data-show` to sidebar items based on search match
- [ ] Style search input to match sandbox theme

## Phase 4: Background Color Picker

- [ ] Add color picker control to preview area (or navbar)
- [ ] Use native `<input type="color">` element
- [ ] Add Datastar signal for background color
- [ ] Add `data-on:change` to update preview area background
- [ ] Add `data-init` to load from localStorage on page load
- [ ] Add script to persist color to localStorage on change
- [ ] Default to neutral color (e.g., `#1a1a2e` dark, `#f0f4f8` light)

## Phase 5: Polish & Testing

- [ ] Test all buttons have proper hover/active states
- [ ] Verify icons inherit correct colors from theme
- [ ] Test sidebar scrolling with 20+ components
- [ ] Test search filtering
- [ ] Test color picker persistence across page reloads
- [ ] Test in both dark and light component themes
- [ ] Update sandbox.css comments/organization

## Files to Modify

| File | Changes |
|------|---------|
| `dev/src/clj/sandbox/icons.clj` | New file - icon definitions |
| `src/clj/ascolais/tsain/views.clj` | Add icons to buttons |
| `dev/resources/public/sandbox.css` | Icon button styles, scrolling sidebar |

## Rollback Plan

All changes are in dev-only files. If issues arise:
1. Revert `sandbox/icons.clj`
2. Revert view changes
3. Revert CSS changes

No impact on core tsain library.
