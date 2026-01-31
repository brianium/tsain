---
title: "Phandaal Based CSS Actions And Effects"
status: complete
date: 2026-01-31
priority: 50
---

# Phandaal Based CSS Actions And Effects

## Overview

Replace text-based file size prompting with effect-driven CSS writes that return actionable hints. All CSS modifications during tsain development flow through phandaal effects, which track line counts and detect threshold violations. Tsain adds domain-specific hints that guide Claude on when and how to split files.

The core insight: instead of prompting Claude to "remember to check file sizes", make the write results self-documenting. When a CSS write pushes a file over threshold, the result includes a hint with the exact action to take.

## Goals

- All CSS writes return LOC metadata (before, after, delta)
- Threshold violations produce actionable hints (not just warnings)
- Hints include: category, target file, and suggested effect to dispatch
- Split operations are first-class effects, not manual file manipulation
- Same pattern extends to namespace management (Clojure files)

## Non-Goals

- CSS parsing/AST manipulation - splits use category patterns, not selector analysis
- Automatic splitting without Claude involvement - hints guide, Claude decides
- Replacing the existing preview/commit workflow - this enhances file I/O only

## Key Decisions

Summarize important decisions made during research. See [research.md](research.md) for details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| File I/O layer | Phandaal | Provides atomic writes, LOC tracking, formatters, audit logging |
| Hint generation | Interceptor | Inspects phandaal results, adds domain context post-execution |
| Categories | From database | `(tsain/categories)` returns project-specific categories; Claude infers for new components |
| Split trigger | Threshold exceeded | Hints appear only when action needed |
| tsain.edn changes | None | Project root inferable, categories from DB, stylesheet path by convention |
| CSS authoring | Pure actions | Tsain actions return phandaal effect vectors; effects only where side-effects unavoidable |
| CSS formatting | Detect + fallback | Use prettier/biome if available; basic JVM normalizer as fallback |
| CSS parsing | jStyleParser | JVM CSS parser for reliable replace/split operations; simple appends don't need it |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Add phandaal dependency and registry composition
- [x] Phase 2: CSS write actions (`::tsain/write-css`, `::tsain/write-css-to`)
- [x] Phase 3: Hints interceptor for threshold detection
- [x] Phase 4: CSS parser integration (jStyleParser)
- [x] Phase 5: Replace CSS effect (`::tsain/replace-css`)
- [x] Phase 6: Split effect (`::tsain/split-css`)
- [x] Phase 9: Update SKILL.md with new workflow
