---
title: "Arity Unpleasantness"
status: completed
date: 2026-01-31
priority: 10
---

# Arity Unpleasantness

## Overview

Sandestin effect handlers with optional trailing arguments fail when those arguments are omitted, despite being marked `{:optional true}` in the schema. The handler receives fewer arguments than expected, causing an arity error.

This is a **sandestin issue**, not a tsain issue. The fix belongs in the sandestin library.

## Goals

- Effects with optional trailing arguments should work when called without them
- No workarounds required (passing empty maps)
- Schema documentation should match runtime behavior

## Non-Goals

- Changing how required arguments work
- Adding new schema features
- Backward-incompatible changes to existing effects

## Key Decisions

See [research.md](research.md) for detailed analysis.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Fix location | sandestin | This is a dispatch-time argument resolution issue |
| Approach | Pad optional args with nil/defaults | Cleanest fix that matches schema semantics |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [ ] Phase 1: Reproduce in sandestin test suite
- [ ] Phase 2: Implement fix in sandestin dispatch
- [ ] Phase 3: Verify tsain effects work without workarounds
