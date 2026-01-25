---
title: "Hiccup Component To Tsain"
status: completed
date: 2026-01-25
priority: 20
---

# Hiccup Component To Tsain

## Overview

Extract and distribute the `hiccup-component` skill as part of the tsain library. This skill encodes the "happy path" workflow for REPL-driven component development with tsain, including alias-first iteration, CSS extraction, and library commits.

Currently this skill lives in individual projects (originated in an app using tsain). By shipping it with tsain, users get the workflow automatically and it stays in sync with API changes.

## Goals

- Ship a Claude Code skill with tsain that documents the component development workflow
- Parameterize all project-specific paths via `tsain.edn`
- Keep the skill focused on core workflows (`iterate`, `implement`)
- Provide clear documentation for users to discover and use the skill

## Non-Goals

- The `auto` command (autonomous loop with chrome screenshots) - too advanced/opinionated for a library skill
- Forcing a specific file organization on users
- Replacing project-specific customizations users might want

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Skill name | `tsain` | Commands become `/tsain iterate`, `/tsain implement` - namespaced to the library |
| Skill location | `.claude/skills/tsain/` | Tsain dogfoods its own skill; users copy from repo like `sample/` |
| Commands included | `iterate`, `implement` | Core workflows; `auto` is too opinionated |
| Configuration | Read from `tsain.edn` | Already the config source; no new files needed |
| Discovery API | Use new default-registry arities | `(tsain/describe)` instead of `(describe (dispatch))` |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Create skill structure in tsain
- [x] Phase 2: Adapt skill content for generic use
- [x] Phase 3: Documentation
