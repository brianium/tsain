---
title: "Copy Hiccup to Clipboard"
status: completed
date: 2024-01-15
priority: 30
---

# Copy Hiccup to Clipboard

## Overview

Add a copy button to the single component view that copies the hiccup for the currently visible variant to the clipboard. This makes it easy to extract component code for use elsewhere.

## Goals

1. One-click copy of visible variant's hiccup
2. Works with/without variants (legacy and new format)
3. Visual feedback ("Copied!" briefly)
4. Formatted output (pretty-printed)

## Non-Goals

- Copy CSS alongside hiccup
- Export to file

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Copy mechanism | Server-side fetch + clipboard | Pretty-printed, accurate |
| Feedback | Button text change | Simple, no toast needed |
| Format | Pretty-printed hiccup | Human-readable |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Copy route
- [x] Phase 2: Button UI
- [x] Phase 3: Feedback animation
