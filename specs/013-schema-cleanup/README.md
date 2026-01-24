---
title: "Schema Cleanup"
status: completed
date: 2024-01-15
priority: 50
---

# Schema Cleanup

## Overview

Tsain has two issues with schema and namespace design:

1. State key uses wrong namespace (::s/state instead of ::state)
2. Duplicate schema definitions (inline vs twk.schema)

Solution: Use tsain's own namespace for state, and import canonical schemas from twk.

## Goals

1. Use ::state instead of ::s/state in tsain registry
2. Use ascolais.twk.schema/Hiccup as base for HiccupSchema
3. Use ascolais.twk.schema/Signals as base for SignalMapSchema
4. Preserve generation hints for s/sample

## Non-Goals

- Schema validation changes
- New schema types

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| State namespace | ::tsain/state | Tsain-specific, not sandestin framework |
| Schema source | twk.schema | Canonical, well-structured |
| Gen hints | mu/update-properties | Preserve sampling capability |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Update namespace
- [x] Phase 2: Use twk schemas
- [x] Phase 3: Update consumers
- [x] Phase 4: Update tests
