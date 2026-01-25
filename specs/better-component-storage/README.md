---
title: "Better Component Storage"
status: in-progress
date: 2026-01-24
priority: 50
---

# Better Component Storage

## Overview

Replace the flat `components.edn` file with SQLite-backed storage using manse, and pivot to html.yeah for component definitions. This enables scalable component libraries with rich metadata, full-text search, and a discovery API that makes tsain an authoritative source for Claude when working with hiccup components.

**Key insight:** Props/attributes should be defined in code (via html.yeah's `defelem`), not stored in the database. SQLite stores only what code can't provide: examples and categories.

## Goals

- Scale component libraries to 100+ components without performance degradation
- Provide discovery API (`describe`, `grep`, `props`) that merges html.yeah schemas with SQLite examples
- Leverage html.yeah for authoritative prop schemas (no drift from code)
- Maintain backwards compatibility within 0.x.x versions
- Clean migration path from existing `components.edn` files
- Make the component library self-documenting for Claude integration

## Non-Goals

- Server-based component sharing (this is local-first, per-project storage)
- Complex versioning/branching (use git for that)
- Storing prop schemas in database (html.yeah is the source of truth)

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Storage Backend | SQLite via manse | FTS5 search, standard tooling, single-file portability |
| Prop Schemas | html.yeah `defelem` | Authoritative, queryable, runtime-validated via malli |
| SQLite Contents | Examples + categories only | Props come from html.yeah; no duplication |
| Discovery API | Merge html.yeah + SQLite | `tsain/describe` combines both sources |
| Component Definitions | Pivot to `defelem` | Replaces raw `c/resolve-alias`; compiles to same output |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [ ] Phase 1: html.yeah integration
- [ ] Phase 2: Database foundation
- [ ] Phase 3: Core effects migration
- [ ] Phase 4: Discovery API
- [ ] Phase 5: Migration tooling
- [ ] Phase 6: Documentation & polish
