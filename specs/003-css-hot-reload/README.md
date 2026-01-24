---
title: "CSS Hot Reload"
status: completed
date: 2024-01-15
priority: 20
---

# CSS Hot Reload

## Overview

Enable hot-reloading of CSS during development. The watcher monitors file changes and broadcasts effects to all sandbox connections via the existing SSE connection.

## Goals

1. Vanilla modern CSS file that works with standard tooling
2. Automatic hot-reload when watched files change
3. Extensible architecture for watching multiple file types
4. Broadcast to existing sandbox connections (no separate watch endpoint)
5. Clean integration with sandestin dispatch

## Non-Goals

- CSS preprocessing (SASS, PostCSS)
- Build-time bundling

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| File watcher | beholder | Simple API, proven library |
| Reload mechanism | Cache-busting query param | Works without full page reload |
| Extension mapping | ext-fx map | Easily add new file types |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Watcher implementation
- [x] Phase 2: CSS reload broadcast
- [x] Phase 3: System integration
