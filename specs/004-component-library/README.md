---
title: "Component Library"
status: completed
date: 2024-01-15
priority: 20
---

# Component Library

## Overview

Extend the REPL-driven workflow with persistent component storage. Components iterated on in the preview area can be committed to a browsable library that persists across restarts.

## Goals

1. Server-side state tracking for preview and library
2. Sandestin registry for all component operations
3. Persist committed components to EDN file
4. Browser UI with uncommitted indicator and commit button
5. URL-routable gallery and component views
6. Everything reloadable at the REPL

## Non-Goals

- Version history for components
- Component export/publishing

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| State location | Server-side atom | Server is source of truth |
| Persistence | EDN file | Simple, human-readable |
| View routing | URL-routable | Deep links, multi-tab support |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: State management
- [x] Phase 2: Sandestin registry
- [x] Phase 3: Browser UI
- [x] Phase 4: Persistence
