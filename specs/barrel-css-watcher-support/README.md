---
title: "Barrel CSS Watcher Support"
status: completed
date: 2026-01-25
priority: 50
---

# Barrel CSS Watcher Support

## Overview

The current CSS hot reload mechanism uses cache-busting query parameters on `<link rel="stylesheet">` elements. However, when stylesheets use CSS `@import` statements (barrel imports), the imported files may still be served from browser cache, breaking hot reload for modular CSS architectures.

## Goals

- Ensure CSS hot reload works correctly with `@import`-based barrel imports
- Maintain the existing cache-busting approach (no full page reloads)
- Keep the solution simple and development-focused

## Non-Goals

- Production caching strategies (this is dev-only)
- Complex CSS dependency graph tracking
- Supporting other CSS import mechanisms (CSS modules, bundlers, etc.)

## Key Decisions

See [research.md](research.md) for details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Approach | Cache-Control headers | Let browser handle cascaded re-fetching |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Add no-cache middleware for CSS files
- [x] Phase 2: Testing & verification
