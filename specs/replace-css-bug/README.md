---
title: "Replace CSS Bug Fix"
status: completed
date: 2026-01-31
priority: 10
---

# Replace CSS Bug Fix

## Overview

Fix two bugs in the `::tsain/replace-css` effect discovered during testing in the callit project:

1. **Appends instead of replacing** - The effect appends new CSS to the end of the stylesheet instead of replacing matched rules in-place, causing duplicate rules
2. **Requires empty options map** - Calling without the third argument causes an arity error despite the schema marking it as optional

## Goals

- Fix `remove-rules` to actually remove matched rules from the CSS string
- Support 2-arity invocations (without options map)
- Maintain correct return value format `{:replaced [...] :path "..."}`
- Add tests to prevent regression

## Non-Goals

- Changing the effect's semantics (it should truly replace, not append-override)
- Adding new features to the CSS manipulation system
- Refactoring beyond what's needed to fix the bugs

## Key Decisions

See [research.md](research.md) for detailed analysis.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Removal strategy | Line-range based | jStyleParser's `.toString()` normalizes CSS, so string matching fails against original source |
| Arity handling | Optional opts destructuring | Cleaner than multi-arity, matches schema definition |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: Fix arity issue (already done with variadic args)
- [x] Phase 2: Fix removal logic (infinite loop + regex anchor)
- [x] Phase 3: Testing (27 tests, 207 assertions pass)
