---
title: "Sandbox Theme System"
status: completed
date: 2026-01-31
priority: 50
---

# Sandbox Theme System

## Overview

Add a theme system to the tsain sandbox UI that allows toggling between light and dark themes. The sandbox chrome (header, sidebar, controls) must have its own isolated theme that doesn't conflict with component library styles being previewed in the main content area.

## Goals

- Theme dropdown in sandbox top bar (alongside preview/components toggle and grid background selector)
- Two polished themes: an attractive light theme and an attractive dark theme
- Complete isolation between sandbox UI theming and component preview theming
- Works correctly when tsain is installed as a companion library in another project
- Themes persist across browser sessions

## Non-Goals

- Theming the component preview area (that's the consumer's responsibility)
- User-customizable theme colors (just the two pre-packaged themes)
- Theme API for programmatic switching (UI-only for now)

## Key Decisions

Summarize important decisions made during research. See [research.md](research.md) for details.

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Scoping strategy | `[data-sandbox-theme]` attribute selector | Clean separation without Shadow DOM complexity, integrates with Datastar |
| Theme storage | localStorage + Datastar signal | localStorage for persistence, signal for reactive binding |
| CSS architecture | CSS custom properties with `--sandbox-` prefix | Standard theming approach, all variables prefixed to avoid conflicts |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [x] Phase 1: CSS Architecture Refactor - scoped with `[data-sandbox-theme]`, prefixed variables
- [x] Phase 2: Theme Infrastructure - dropdown, signals, localStorage persistence
- [x] Phase 3: Dark Theme Design - polished dark theme with amber accents
- [x] Phase 4: Light Theme Design - cohesive light theme
- [x] Phase 5: Polish & Edge Cases - smooth transitions, persistence verified

## Development Approach

This spec will use:
- **Chrome extension** for visual validation and screenshots
- **frontend-design skill** for creating attractive, polished theme designs

Claude will perform its own visual validation during development, iterating on designs until they meet quality standards.
