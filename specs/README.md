# Specifications

This directory contains living specifications for tsain features and concepts.

## Current Priorities

### Milestone 1: Canvas for HTML updates
**Spec:** [002-dev-sandbox](./002-dev-sandbox.md)

Get to a working sandbox where we can push hiccup updates from the REPL. Done when:
- `(dev)` / `(start)` launches sandbox at localhost:3000
- `(dispatch ...)` with twk effects updates browser instantly
- Multiple browsers/devices receive updates simultaneously

### Milestone 2: CSS hot-reload
**Spec:** [003-css-hot-reload](./003-css-hot-reload.md)

Add file watcher for CSS. Done when:
- Edit `styles.css`, save → browser updates automatically
- Works across all connected devices

### Milestone 3: Component Library
**Spec:** [004-component-library](./004-component-library.md)

Persistent component library with REPL workflow. Done when:
- `(preview!)` / `(commit!)` / `(show!)` work from REPL
- Browser shows uncommitted indicator + Commit button
- `/sandbox/components` renders gallery
- `/sandbox/c/:name` renders single component (deep linkable)
- Components persist to EDN, load on restart

## Spec Index

| Spec | Status | Description |
|------|--------|-------------|
| [001-claude-md-documentation](./001-claude-md-documentation.md) | Complete | Comprehensive CLAUDE.md documentation for sandestin, twk, sfere, and kaiin |
| [002-dev-sandbox](./002-dev-sandbox.md) | Complete | Reloadable dev sandbox for REPL-driven hiccup component development |
| [003-css-hot-reload](./003-css-hot-reload.md) | Complete | Plain CSS file with file watcher and SSE-based hot-reload |
| [004-component-library](./004-component-library.md) | Complete | Persistent component library with browser gallery and REPL workflow |

Status values: Draft, Active, Complete, Archived
