# Sandbox Theme System - Implementation Plan

## Overview

Step-by-step implementation tasks. Update checkboxes and add commit hashes as you progress.

**Development Tools:**
- Use **chrome extension** for screenshots and visual validation
- Use **frontend-design skill** for creating polished theme designs
- Claude performs visual validation at each design phase

## Prerequisites

- [x] Review current `resources/tsain/sandbox.css` architecture
- [x] Review `src/clj/ascolais/tsain/views.clj` HTML structure
- [x] Identify all CSS custom properties currently in use
- [x] Understand current Datastar signal usage in sandbox

## Phase 1: CSS Architecture Refactor

Prepare the CSS for theming by establishing proper scoping.

- [x] Audit `sandbox.css` for any selectors that could conflict with component styles
- [x] Add `[data-sandbox-theme]` attribute to sandbox root element in views.clj
- [x] Rename/prefix any problematic selectors (e.g., generic `.header`, `.sidebar`)
- [x] Extract current color values into CSS custom properties with `--sandbox-` prefix
- [x] Wrap sandbox styles in `[data-sandbox-theme]` scope
- [x] Verify component preview area is unaffected by sandbox styles

## Phase 2: Theme Infrastructure

Build the switching mechanism.

- [x] Add `sandboxTheme` Datastar signal (default: "dark" for backward compat)
- [x] Create theme dropdown component in sandbox header
  - Position: after grid background selector
  - Options: "Light", "Dark"
  - Use `data-bind` for two-way binding to signal
- [x] Wire `data-sandbox-theme` attribute to signal value
- [x] Add localStorage persistence for theme preference
  - Load on page init
  - Save on change
- [x] Test theme switching works (even with placeholder colors)

## Phase 3: Dark Theme Design

Create an attractive dark theme using frontend-design skill.

- [x] Design dark theme color palette
  - Background colors (primary, secondary, tertiary)
  - Text colors (primary, secondary, muted)
  - Accent colors (interactive elements, highlights)
  - Border colors
  - Shadow/glow effects
- [x] Implement dark theme CSS custom properties
- [x] Apply to all sandbox chrome elements:
  - Header bar
  - Sidebar (component list)
  - Control buttons
  - Dropdown menus
  - Grid background selector
  - Theme dropdown itself
- [x] Take screenshots with chrome extension
- [x] Iterate on design until visually polished

## Phase 4: Light Theme Design

Create an attractive light theme using frontend-design skill.

- [x] Design light theme color palette
  - Should feel cohesive with dark theme (same design language)
  - Appropriate contrast for readability
  - Professional, modern aesthetic
- [x] Implement light theme CSS custom properties
- [x] Test all sandbox chrome elements in light mode
- [x] Take screenshots with chrome extension
- [x] Iterate on design until visually polished
- [x] Compare dark/light side by side for consistency

## Phase 5: Polish & Edge Cases

- [x] Test theme persistence across page reloads
- [x] Test theme switching with component preview loaded
- [x] Verify no style leakage into preview area (both themes)
- [ ] Test when tsain installed as library in fresh project
- [x] Add smooth transition between themes (optional, subtle)
- [x] Ensure dropdown is keyboard accessible

## Phase 6: Documentation

- [ ] Update CLAUDE.md with theme system info (if relevant to development)
- [ ] Add theme info to README.md feature list
- [ ] Document CSS custom property naming convention

## Rollout Plan

1. Implement in dev sandbox first
2. Visual validation with chrome extension
3. Test as installed library in separate project
4. Commit and tag release

## Rollback Plan

If issues arise:
1. Theme system is additive - existing dark styles preserved
2. Can revert to single theme by removing dropdown and data attribute
3. CSS changes are isolated to sandbox.css (not component styles)
