# Better Component View Header - Implementation Plan

## Overview

Simplify component-detail header: remove prev/next nav, add variant chips, eliminate duplicate description.

## Prerequisites

- [x] Existing tabbed component view working
- [x] Multiple examples per component supported

## Phase 1: Remove Redundant Elements

- [x] Remove prev/next navigation buttons from component-detail
- [x] Remove component-nav wrapper (no longer needed)
- [x] Remove description from props-tab (keep only in header)
- [x] Simplify component-detail layout

## Phase 2: Implement Variant Chips

- [x] Create variant-chips helper function
- [x] Render all examples as clickable chip buttons
- [x] Style active chip with accent color
- [x] Wire up click handlers to switch example
- [x] Handle single-example case (hide chips section)

## Phase 3: Update Header Layout

- [x] Create clean header with:
  - Component name (h2)
  - Description (muted paragraph)
  - Variant chips row (when multiple examples)
- [x] Add CSS for new header structure
- [x] Add CSS for variant chips (pill style, hover, active states)

## Phase 4: Polish

- [x] Test with components having 1, 2, 5+ examples
- [x] Verify description shows in header, not Props tab
- [x] Check responsive behavior
- [x] Visual consistency check

## Rollout Plan

1. Update component-detail function
2. Update CSS
3. Test in browser
4. Commit

## Rollback Plan

All changes are in views.clj and sandbox.css - revert those files.
