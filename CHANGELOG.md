# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.6.0] - 2026-01-31

### Added

- Light/dark theme system for sandbox with `.theme-light` and `.theme-dark` classes
- CSS custom properties that adapt to theme context

### Changed

- Removed unused kaiin dependency

## [0.5.0] - 2026-01-31

### Added

- `::tsain/write-component` action for authoring components via effects with threshold hints
- `::tsain/write-css` action for CSS authoring via effects with category tracking
- `::tsain/replace-css` action for replacing existing CSS rules by selector prefix
- `::tsain/split-css` and `::tsain/split-namespace` actions for barrel import extraction
- Phandaal-based CSS parsing for intelligent style management
- Status indicator component
- cljfmt integration for automatic code formatting

### Changed

- All CSS and component modifications now required to go through the effect system
- Added category inference for component writes based on naming conventions

## [0.4.0] - 2026-01-25

### Added

- Component view tabs (Preview/Code/Props) in sandbox
- Sidebar category grouping for better organization
- Category selection in commit form
- Variant chips replacing the example dropdown
- Sidebar collapse state persistence in localStorage

### Fixed

- Component tags now properly qualified for html.yeah metadata lookup
- CSS hot reload works correctly with `@import` statements

### Changed

- Extracted card stat styles to barrel import as proof of concept

## [0.3.0] - 2026-01-25

### Added

- html.yeah integration with `defelem` macro for schema-driven components
- SQLite storage for component library (replaces EDN)
- Discovery API: `describe`, `grep`, `props`, `categories`, `by-category`
- EDN to SQLite migration effect `::tsain/migrate-from-edn`
- Claude Code skill (`/tsain`) for component development workflow
- Component migration agent for converting legacy chassis aliases
- Barrel import guidance for managing large files
- Cyberpunk accordion component

### Changed

- Default registry stored in atom for simplified discovery function calls
- Replaced manse with direct next.jdbc dependency

## [0.2.1] - 2026-01-24

### Fixed

- Consolidated sandbox.css to single distributed location in `resources/tsain/`

## [0.2.0] - 2026-01-24

### Added

- Lucide icons throughout sandbox UI
- Component search functionality
- Color picker for interactive styling
- Commit feedback messages
- Independent sidebar scrolling

### Changed

- Polished sandbox UI with refined developer tool aesthetic

## [0.1.0] - 2026-01-21

Initial release.

### Added

- REPL-driven component sandbox for hiccup development
- Live browser preview with SSE updates via Datastar
- CSS hot reload with file watcher
- Component library with persistence (EDN format)
- Storybook-style sidebar navigation
- Copy hiccup button for easy component extraction
- Light/dark mode variant support
- Portable library distribution via `resources/tsain/`
- Discoverable dev registry with sandestin effects
- Alias-first component workflow with chassis
- Built-in components: event-modal, toast, playing-card, pricing-card, stats-card, feature-card, cta

### Effects

- `::tsain/preview` - Replace preview with hiccup content
- `::tsain/preview-append` - Append content to preview
- `::tsain/preview-clear` - Clear preview area
- `::tsain/patch-signals` - Update Datastar signals
- `::tsain/commit` - Save component to library
- `::tsain/show-components` - Display component(s) in sandbox

[0.6.0]: https://github.com/brianium/tsain/compare/v0.5.0...v0.6.0
[0.5.0]: https://github.com/brianium/tsain/compare/v0.4.0...v0.5.0
[0.4.0]: https://github.com/brianium/tsain/compare/v0.3.0...v0.4.0
[0.3.0]: https://github.com/brianium/tsain/compare/v0.2.1...v0.3.0
[0.2.1]: https://github.com/brianium/tsain/compare/v0.2.0...v0.2.1
[0.2.0]: https://github.com/brianium/tsain/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/brianium/tsain/releases/tag/v0.1.0
