---
title: "Better Component View Header"
status: completed
date: 2026-01-25
priority: 50
---

# Better Component View Header

## Overview

Simplify the component view header by removing redundant navigation and duplicate descriptions. Improve variant/example selection UX with a more modern pattern.

## Goals

- Remove prev/next navigation buttons (sidebar handles this now)
- Eliminate duplicate description (header vs Props tab)
- Improve variant selection UX with visible, clickable options
- Create a cleaner, more focused header layout
- Better use of horizontal space

## Non-Goals

- Changing the tab system (Preview/Code/Props)
- Modifying sidebar behavior
- Adding new metadata displays

## Key Decisions

See [research.md](research.md) for details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Navigation | Remove prev/next | Sidebar provides better navigation |
| Description | Header only | Remove from Props tab to avoid duplication |
| Variant selector | Chip/pill buttons | More discoverable than dropdown, shows all options at once |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Remove redundant elements
- [x] Phase 2: Implement variant chips
- [x] Phase 3: Polish and test
