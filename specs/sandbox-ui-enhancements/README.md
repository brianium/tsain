---
title: "Sandbox UI Enhancements"
status: completed
date: 2026-01-25
priority: 50
---

# Sandbox UI Enhancements

## Overview

Enhance the sandbox UI to better surface component metadata exposed by html.yeah's `defelem`. Add category-based sidebar grouping, tabbed metadata views (Preview/Code/Props), and category selection during commit.

## Goals

- Surface component metadata (description, schema, examples) in a discoverable way
- Group sidebar components by category for easier navigation
- Provide Storybook-style tabs for viewing code and prop documentation
- Allow category assignment when committing components
- Keep component preview as the primary focus
- Implement all interactivity via twk/datastar (no custom JS beyond Datastar expressions)

## Non-Goals

- Editing component category after commit (separate spec)
- Datastar Pro features (no `data-persist`)
- Complex schema visualization (nested maps, etc. can show as "complex")

## Key Decisions

See [research.md](research.md) for details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Tab rendering | Eager | Negligible cost for single view, simpler implementation |
| Props detail | Name + type + required | Covers 90% of cases, complex types show simplified |
| Sidebar state | localStorage via Datastar expressions | Persist collapse state without Datastar Pro |
| Description placement | Muted line under component name | Visible but unobtrusive, keeps focus on component |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Sidebar category grouping
- [x] Phase 2: Component view tabs (Preview/Code/Props)
- [x] Phase 3: Category selection on commit
- [x] Phase 4: localStorage persistence for sidebar
