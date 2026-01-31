---
title: "Html Yeah Component Writing Actions"
status: completed
date: 2026-01-31
priority: 40
---

# Html Yeah Component Writing Actions

## Overview

Extend the phandaal-based effect pattern to component authoring. Just as `::tsain/write-css` tracks line counts and suggests splits, `::tsain/write-component` tracks namespace size and suggests barrel imports. Claude learns when to split from REPL hints, not from reading documentation about thresholds.

The core insight: component iteration happens at the REPL. When a namespace grows too large, the write result includes a hint with the exact action to take—no need to articulate file size management in skill documentation.

## Goals

- `::tsain/write-component` action writes `hy/defelem` definitions to the configured UI namespace
- Threshold violations produce actionable hints suggesting `::tsain/split-namespace`
- `::tsain/split-namespace` extracts category components to sub-namespaces with barrel requires
- SKILL.md relies on hints from REPL rather than explaining thresholds in prose
- CLAUDE.md mandates this approach for tsain's own sandbox development
- Tsain remains installable as a library; all paths configurable via `tsain.edn`

## Non-Goals

- AST manipulation of Clojure code—splits use category patterns and grep, not parsing
- Automatic splitting without Claude involvement—hints guide, Claude decides
- Replacing the existing preview/commit workflow—this enhances component authoring only
- Changes to html.yeah itself—tsain wraps it

## Key Decisions

Summarize important decisions made during research. See [research.md](research.md) for details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Component I/O layer | Phandaal | Already integrated for CSS; provides LOC tracking, formatters, audit logging |
| Hint generation | Interceptor | Same pattern as CSS; inspect phandaal results, add domain hints |
| Category detection | From component name | `::game-card` → category "cards"; explicit `:category` override supported |
| Split trigger | Threshold exceeded | Hints appear only when action needed |
| Namespace structure | Barrel imports | `sandbox.ui` requires `sandbox.ui.cards`, etc. |
| Clojure formatting | Detect cljfmt | Check classpath at runtime; use if available with project's `.cljfmt.edn`; no-op otherwise |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Research namespace patterns and phandaal integration
- [x] Phase 2: Clojure formatter detection (cljfmt)
- [x] Phase 3: `::tsain/write-component` action
- [x] Phase 4: Component hints interceptor
- [x] Phase 5: `::tsain/split-namespace` effect
- [x] Phase 6: Update SKILL.md to remove prose-based file size guidance
- [x] Phase 7: Update CLAUDE.md for tsain's own development
- [x] Phase 8: Testing & polish
