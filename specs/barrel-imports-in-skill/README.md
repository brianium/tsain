---
title: "Barrel Imports In Skill"
status: planned
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
- Make thresholds configurable via `tsain.edn`
- Update the tsain skill to proactively guide splits
- Ensure the pattern is portable across all tsain-powered projects

## Non-Goals

- Automatic file splitting (Claude suggests, human approves)
- Complex dependency analysis between split files
- Build tooling or bundler configuration
- Handling circular dependencies (keep splits simple and acyclic)

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Default threshold | 1500 lines | Large enough for substantial work, small enough to stay navigable |
| CSS split location | `components/` subdirectory | Groups related styles, mirrors component structure |
| NS split pattern | By category (cards, controls, etc.) | More stable than per-component splits |
| Re-export strategy | Require only (Clojure) | Alias registration is the goal, not re-exporting vars |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [ ] Phase 1: Research and document patterns
- [ ] Phase 2: Update skill instructions
- [ ] Phase 3: Add tsain.edn configuration
- [ ] Phase 4: Document in CLAUDE.md template
