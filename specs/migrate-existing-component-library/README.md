---
title: "Migrate Existing Component Library"
status: planned
date: 2026-01-25
priority: 50
---

# Migrate Existing Component Library

## Overview

Design a migration agent that transforms legacy component libraries to the modern tsain/html.yeah architecture. The agent handles **code transformation** (chassis aliases → defelem with schemas), while the existing `::tsain/migrate-from-edn` effect handles **data migration** (examples → SQLite).

This spec depends on [barrel-imports-in-skill](../barrel-imports-in-skill/README.md) being implemented first, as the agent will extract components into the barrel import structure.

## Goals

- Ship a reusable migration agent in `.claude/agents/`
- Transform `defmethod c/resolve-alias` → `hy/defelem` with malli schemas
- Extract components into barrel import namespace structure
- Extract component CSS into barrel import file structure
- Enable incremental migration (one component at a time)
- Work with existing `migrate-from-edn` for example data

## Non-Goals

- Migrating non-chassis component systems
- Modifying example data structure (EDN → SQLite handles this)

## Orchestration

The migration supports natural language coordination. You can say:

> "Migrate all components from `src/clj/brianium/views/components.clj`"

Claude will:
1. Read the source namespace
2. Extract all `defmethod c/resolve-alias` definitions
3. Spawn parallel Task agents (one per component)
4. Report progress as each completes

This uses Claude Code's Task tool which supports parallel agent spawning.

## Key Decisions

See [research.md](research.md) for details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Agent granularity | One component per invocation | Allows review between transformations |
| Code vs data | Agent handles code, effect handles data | Separation of concerns |
| Barrel structure | By category | Matches barrel-imports-in-skill spec |
| Dependencies | Requires barrel-imports-in-skill first | Agent needs target structure to exist |
| Validation | Render test after each transform | Catch regressions immediately |

## Migration Flow

```
┌─────────────────────────────────────────────────────────────┐
│  1. SETUP (manual, once)                                    │
│     - Implement barrel-imports-in-skill                     │
│     - Create category directories (cards/, controls/, etc.) │
│     - Set up barrel CSS structure                           │
└─────────────────────────────────┬───────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────┐
│  2. CODE TRANSFORMATION (agent, per component)              │
│     - Read chassis alias from monolithic namespace          │
│     - Generate defelem with malli schema                    │
│     - Write to appropriate category namespace               │
│     - Extract CSS to category stylesheet                    │
│     - Add require to barrel namespace                       │
│     - Delete old defmethod                                  │
│     - Verify render still works                             │
└─────────────────────────────────┬───────────────────────────┘
                                  │
                                  ▼ (repeat for each component)
                                  │
┌─────────────────────────────────▼───────────────────────────┐
│  3. DATA MIGRATION (effect, once per project)               │
│     (dispatch [[::tsain/migrate-from-edn "path/to/edn"]])   │
│     - Moves examples to SQLite                              │
│     - Preserves descriptions, hiccup, created-at            │
└─────────────────────────────────┬───────────────────────────┘
                                  │
┌─────────────────────────────────▼───────────────────────────┐
│  4. CLEANUP (manual)                                        │
│     - Archive/delete components.edn                         │
│     - Remove empty monolithic namespace                     │
│     - Update any direct namespace references                │
└─────────────────────────────────────────────────────────────┘
```

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [ ] Phase 1: Design agent interface and prompts
- [ ] Phase 2: Implement code transformation logic
- [ ] Phase 3: Integrate with barrel import structure
- [ ] Phase 4: Add CSS extraction
- [ ] Phase 5: Parallel orchestration support
- [ ] Phase 6: Testing and documentation
