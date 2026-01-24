---
title: "Development Sandbox"
status: completed
date: 2024-01-15
priority: 10
---

# Development Sandbox

## Overview

Create a reloadable development sandbox for rapidly iterating on hiccup components. The sandbox provides a browser-based UI that can be updated in real-time from the REPL by dispatching twk effects via sandestin.

This enables a tight feedback loop for component development:
1. Write hiccup in the REPL
2. Dispatch a twk effect
3. See the result instantly in the browser
4. Iterate without page refreshes

## Goals

1. Set up a reloadable dev environment following the kaiin demo pattern
2. Establish a persistent SSE connection for REPL-driven UI updates
3. Create a minimal component sandbox page
4. Use standard sandestin dispatch for all REPL interactions
5. Lay groundwork for a hiccup component library

## Non-Goals

- Production-ready UI design
- Component persistence (covered in spec 004)

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| CSS approach | Vanilla modern CSS | No frameworks, native features (nesting, container queries) |
| Connection model | Single SSE per tab | Reuse for both REPL and file watcher |
| State management | Sandestin dispatch | Consistent with ecosystem |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Project setup
- [x] Phase 2: SSE connection
- [x] Phase 3: REPL dispatch
- [x] Phase 4: Multi-device sync
