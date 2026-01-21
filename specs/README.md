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

### Milestone 4: Component Navigation
**Spec:** [005-component-navigation](./005-component-navigation.md) ✓

Previous/Next navigation on single component views. Done when:
- Previous/Next buttons on `/sandbox/c/:name` view
- Navigation wraps around (last → first)
- Alphabetical ordering matches gallery

### Milestone 5: Dynamic Components
**Spec:** [006-dynamic-components](./006-dynamic-components.md) ✓

Support for stateful Datastar components. Done when:
- Chassis aliases in `sandbox/ui.clj` define structure (attrs pass-through)
- `components.edn` supports `:examples` with multiple configs per component
- Sandbox UI shows dropdown to switch between examples
- `patch-signals!` broadcasts signal patches for REPL testing
- Component-iterate skill documents the full workflow

### Milestone 6: Copy Hiccup
**Spec:** [007-copy-hiccup](./007-copy-hiccup.md) ✓

One-click copy of component hiccup to clipboard. Done when:
- Copy button on single component view (`/sandbox/c/:name`)
- Copies formatted hiccup for currently visible variant
- Visual "Copied!" feedback after successful copy
- Works for components with and without variants

### Milestone 7: CSS Extraction Workflow
**Spec:** [008-css-extraction](./008-css-extraction.md) ✓

Establish workflow where committed components use CSS classes. Done when:
- `styles.css` contains all component styles with clear organization
- All components in `components.edn` use CSS classes instead of inline styles
- Theme variants work via CSS custom properties
- component-iterate skill documents CSS extraction as required step
- CLAUDE.md includes component styling conventions
- Copied hiccup is clean and readable

### Milestone 8: Storybook Sidebar ✓
**Spec:** [009-storybook-sidebar](./009-storybook-sidebar.md)

Replace gallery grid with storybook-style sidebar navigation. Done when:
- Collapsible sidebar lists all components
- Clicking sidebar entry loads component in main area
- Sidebar state persists across view changes
- Preview tab preserved for REPL-driven development
- Deep links (`/sandbox/c/:name`) highlight correct sidebar item

### Milestone 9: Chassis Alias Workflow ✓
**Spec:** [010-chassis-alias-workflow](./010-chassis-alias-workflow.md)

Enforce alias-first component development. Done when:
- Component structure lives in `sandbox/ui.clj` as chassis aliases
- `components.edn` uses alias invocations with config props (not raw hiccup)
- Copy button copies the lean alias form
- component-iterate skill documents alias-first workflow
- CLAUDE.md has chassis alias conventions for namespaced config props

### Milestone 10: Discoverable Dev Registry
**Spec:** [011-discoverable-dev-registry](./011-discoverable-dev-registry.md)

Make sandbox API discoverable via sandestin. Done when:
- All effects use `::tsain/` namespace with rich schemas and descriptions
- dev.clj simplified to dispatch + lifecycle (no ad-hoc wrapper functions)
- `(s/describe dispatch)` shows useful API overview
- `(s/sample dispatch ::tsain/preview)` generates usable examples
- component-iterate skill uses discovery workflow
- Unit tests pass for registry factory, config loading, and schema validation

### Milestone 11: Portable Library ✓
**Spec:** [012-portable-library](./012-portable-library.md)

Package tsain for distribution to any sandestin/twk project. Done when:
- Library exports registry factory + route factory
- `sample/CLAUDE.md` contains ecosystem docs + tsain workflow
- `sample/tsain.edn` and `sample/ui.clj` provide starter templates
- `.claude/skills/component-iterate/` uses discovery-first approach
- Fresh project can integrate tsain following setup guide
- Tsain dogfoods its own library exports

## Spec Index

| Spec | Status | Description |
|------|--------|-------------|
| [001-claude-md-documentation](./001-claude-md-documentation.md) | Complete | Comprehensive CLAUDE.md documentation for sandestin, twk, sfere, and kaiin |
| [002-dev-sandbox](./002-dev-sandbox.md) | Complete | Reloadable dev sandbox for REPL-driven hiccup component development |
| [003-css-hot-reload](./003-css-hot-reload.md) | Complete | Plain CSS file with file watcher and SSE-based hot-reload |
| [004-component-library](./004-component-library.md) | Complete | Persistent component library with browser gallery and REPL workflow |
| [005-component-navigation](./005-component-navigation.md) | Complete | Previous/Next navigation on single component views |
| [006-dynamic-components](./006-dynamic-components.md) | Complete | Stateful Datastar components with multiple example configs |
| [007-copy-hiccup](./007-copy-hiccup.md) | Complete | Copy hiccup to clipboard from single component view |
| [008-css-extraction](./008-css-extraction.md) | Complete | CSS extraction workflow for clean, reusable component hiccup |
| [009-storybook-sidebar](./009-storybook-sidebar.md) | Complete | Replace gallery grid with storybook-style sidebar navigation |
| [010-chassis-alias-workflow](./010-chassis-alias-workflow.md) | Complete | Enforce alias-first component development for lean components.edn |
| [011-discoverable-dev-registry](./011-discoverable-dev-registry.md) | Complete | Make sandbox API discoverable via sandestin registry |
| [012-portable-library](./012-portable-library.md) | Complete | Package tsain for distribution to any sandestin/twk project |

Status values: Draft, Active, Complete, Archived
