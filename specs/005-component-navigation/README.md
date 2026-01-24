---
title: "Component Navigation"
status: completed
date: 2024-01-15
priority: 30
---

# Component Navigation

## Overview

Add "previous" and "next" navigation buttons to the single component view. When viewing a component in isolation, users should be able to quickly navigate through the library without returning to the gallery.

## Goals

1. Previous/Next buttons on single component view
2. Wrap-around navigation (last → first, first → last)
3. Keyboard shortcuts for navigation
4. Consistent ordering (alphabetical by name)

## Non-Goals

- Custom sorting options
- Search/filter in navigation

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Ordering | Alphabetical by name | Matches gallery grid order |
| Wrap-around | Yes | No dead ends in navigation |
| Keyboard | Arrow keys (optional) | Power user feature |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Navigation UI
- [x] Phase 2: Wrap-around logic
- [x] Phase 3: Broadcast sync
- [ ] Phase 4: Keyboard shortcuts (optional)
