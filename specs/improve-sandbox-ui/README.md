---
title: "Improve Sandbox UI"
status: completed
date: 2026-01-24
priority: 30
---

# Improve Sandbox UI

## Overview

Polish the tsain sandbox developer experience with better icons, improved navigation, and flexible theming options. The sandbox is a REPL-driven component preview tool - we want it to feel professional without adding heavy dependencies.

## Goals

- Add Lucide icons to buttons/controls for visual clarity
- Make preview background color configurable (for testing components against different backgrounds)
- Fix sidebar scrolling for large component libraries
- Add search/filter for quick component lookup

## Non-Goals

- Changing the core tsain plugin architecture
- Adding React or other JS frameworks
- Replacing the existing theme system
- Making the sandbox a "design tool" (it's a dev tool)

## Key Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Icon library | Lucide (inline SVG) | Zero JS, just hiccup data, beautiful consistent icons |
| Design tokens | Existing CSS vars | Open Props is overkill, we already have a theme system |
| Color picker | Native `<input type="color">` | No dependencies, persists to localStorage |
| Search | Datastar signals + data-show | Already using Datastar, simple filter pattern |

## Implementation Status

See `implementation-plan.md` for detailed task breakdown.

- [x] Phase 1: Icons infrastructure
- [x] Phase 2: UI improvements (scrolling, search)
- [x] Phase 3: Background color picker
- [x] Phase 4: Polish and testing
