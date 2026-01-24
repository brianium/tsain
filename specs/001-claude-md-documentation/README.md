---
title: "CLAUDE.md Documentation"
status: completed
date: 2024-01-15
priority: 10
---

# CLAUDE.md Documentation

## Overview

This spec defines the comprehensive CLAUDE.md documentation for the tsain project. The documentation enables Claude to effectively work with the sandestin ecosystem: creating registries, building dispatch functions, using kaiin for declarative routing, and leveraging REPL-driven discovery.

## Goals

1. Provide clear mental model of the sandestin effect dispatch system
2. Document registry authoring patterns with concrete examples
3. Explain dispatch creation and the execution flow
4. Teach REPL-based discovery using sandestin's introspection API
5. Document kaiin's declarative routing approach
6. Show complete working patterns for twk (Datastar) and sfere (connections)

## Non-Goals

- Documenting implementation details of libraries (sandestin, twk, sfere internals)
- Covering all edge cases and error handling

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Code-first docs | Examples as primary teaching tool | Clojure developers learn by example |
| REPL discovery | Teach describe/sample/grep | Self-documenting systems reduce doc maintenance |
| Single reference | Unified CLAUDE.md | One source of truth for AI assistant |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Architecture diagram
- [x] Phase 2: Registry authoring docs
- [x] Phase 3: Discovery API docs
- [x] Phase 4: Integration patterns
