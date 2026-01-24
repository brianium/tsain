---
title: "CSS Extraction Workflow"
status: completed
date: 2024-01-15
priority: 40
---

# CSS Extraction Workflow

## Overview

Establish a workflow where committed components use CSS classes from `styles.css` rather than inline styles. This ensures copied hiccup is clean, reusable, and maintainable.

## Goals

1. Inline styles for exploration, CSS classes for commits
2. Clean hiccup that's copy-paste ready
3. Hot-reload friendly CSS
4. Theme variants via CSS custom properties

## Non-Goals

- CSS-in-JS solutions
- Build-time CSS extraction

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Exploration | Inline styles OK | Rapid iteration |
| Commit | CSS classes required | Clean copied hiccup |
| Theming | CSS custom properties | Single hiccup, multiple themes |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Workflow updates
- [x] Phase 2: CSS foundation
- [x] Phase 3: Component refactoring
- [x] Phase 4: Verification
