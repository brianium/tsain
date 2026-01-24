---
title: "Chassis Alias Workflow"
status: completed
date: 2024-01-15
priority: 40
---

# Chassis Alias Workflow

## Overview

When Claude iterates on components, the full verbose hiccup gets committed to components.edn. This results in bloated files, duplicate markup for variants, and less portable assets.

The solution: Component structure lives in sandbox/ui.clj as chassis aliases; components.edn stores lean alias invocations with config props.

## Goals

1. Component structure in sandbox/ui.clj as chassis aliases
2. Component examples in components.edn use aliases with config props
3. Copied hiccup is the lean alias invocation, not expanded markup

## Non-Goals

- Automatic alias generation
- Alias validation

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Config props | Namespaced keywords (:game-card/title) | Self-documenting, auto-elided by Chassis |
| Structure location | sandbox/ui.clj | Single source of truth |
| Library format | Alias invocations | Portable, lean |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Define alias pattern
- [x] Phase 2: Migrate components
- [x] Phase 3: Update copy button
- [x] Phase 4: Update skill/docs
