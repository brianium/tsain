---
title: "Barrel Imports In Skill"
status: completed
date: 2026-01-25
priority: 40
---

# Barrel Imports In Skill

## Overview

As tsain becomes the standard component authoring tool across projects, we need guidance for managing file growth. When CSS or component namespaces exceed ~1500 lines, they become unwieldy - hard to navigate, slow to parse, and prone to merge conflicts.

This spec defines a "barrel import" pattern where large files are split into logical sub-files, with a main file that imports/requires them. The tsain skill should know when to suggest this split and how to execute it consistently across projects.

## Goals

- Define clear thresholds for when to split files (default: 1500 lines)
- Establish consistent patterns for CSS `@import` composition
- Establish consistent patterns for Clojure namespace composition
- Update the tsain skill to proactively guide splits
- Use convention over configuration - minimal tsain.edn additions

## Non-Goals

- Automatic file splitting (Claude suggests, human approves)
- Complex dependency analysis between split files
- Build tooling or bundler configuration
- Handling circular dependencies (keep splits simple and acyclic)

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Default threshold | 1500 lines | Large enough for substantial work, small enough to stay navigable |
| CSS split location | `components/` (convention) | Always use `components/` subdirectory - no config needed |
| NS split location | Derived from `:ui-namespace` | `sandbox.ui` → `sandbox/ui/` per Clojure's namespace rules |
| NS split pattern | By category (cards, controls, etc.) | More stable than per-component splits |
| Re-export strategy | Require only (Clojure) | Alias registration is the goal, not re-exporting vars |
| Configuration | Convention over config | Only `:split-threshold` is configurable; paths are derived |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Research and document patterns
- [x] Phase 2: Update tsain.edn configuration
- [x] Phase 3: Update skill instructions
- [x] Phase 4: Update CLAUDE.md template
- [x] Phase 5: Testing & Validation
- [x] Phase 6: Documentation & Cleanup
