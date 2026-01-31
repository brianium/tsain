---
title: "Sandbox Theme System"
status: planned
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
| Scoping strategy | TBD | Must prevent sandbox styles from leaking into component preview |
| Theme storage | TBD | localStorage vs cookie vs Datastar signal |
| CSS architecture | TBD | CSS custom properties, separate stylesheets, or both |

## Implementation Status

See [implementation-plan.md](implementation-plan.md) for detailed task breakdown.

- [ ] Phase 1: Research scoping strategies and current CSS architecture
- [ ] Phase 2: Design and implement theme infrastructure
- [ ] Phase 3: Create light and dark themes with frontend-design skill
- [ ] Phase 4: Visual validation with chrome extension
- [ ] Phase 5: Testing & polish

## Development Approach

This spec will use:
- **Chrome extension** for visual validation and screenshots
- **frontend-design skill** for creating attractive, polished theme designs

Claude will perform its own visual validation during development, iterating on designs until they meet quality standards.
