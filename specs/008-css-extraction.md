# 008: CSS Extraction Workflow

## Status: Complete

## Overview

Establish a workflow where committed components use CSS classes from `styles.css` rather than inline styles. This ensures copied hiccup is clean, reusable, and maintainable.

## Problem Statement

Current state:
- All 6 components in `components.edn` use massive inline styles (game-card alone is 200+ lines)
- `styles.css` is empty
- Copying component hiccup yields unmanageable inline style blobs
- The component-iterate skill mentions CSS extraction but treats it as optional

Desired state:
- Committed components use CSS classes, not inline styles
- `styles.css` contains all component styles with clear organization
- Copied hiccup is clean and readable
- The workflow enforces CSS extraction before commit

## Design Principles

1. **Inline styles for exploration, CSS classes for commits** - Use inline styles during rapid iteration, extract to CSS before committing
2. **Clean hiccup** - Committed components should be copy-paste ready without style bloat
3. **Hot-reload friendly** - CSS in `styles.css` hot-reloads, enabling continued iteration after commit
4. **Theme variants via CSS** - Use CSS custom properties or modifier classes for dark/light variants instead of duplicating entire hiccup structures

## Architecture Changes

### 1. Update component-iterate Skill

Add explicit "CSS Extraction" phase before commit:

```markdown
### 5. Extract Styles to CSS (Required Before Commit)

Before committing, move inline styles to `styles.css`:

1. **Identify repeated patterns** - Buttons, cards, containers get classes
2. **Name classes semantically** - `.game-card`, `.game-card-header`, `.stat-badge`
3. **Use CSS nesting** - Group related styles under parent selector
4. **Use CSS custom properties for themes** - `--bg-primary`, `--accent-color`

Example extraction:

**Before (inline):**
```clojure
[:div {:style "background: #0a0a12; padding: 16px; border: 1px solid #0ff;"}
 [:h2 {:style "color: #0ff; font-size: 14px;"} "Title"]]
```

**After (CSS classes):**
```clojure
[:div.game-card
 [:h2.game-card-title "Title"]]
```

```css
.game-card {
  background: var(--bg-primary, #0a0a12);
  padding: 16px;
  border: 1px solid var(--accent-primary, #0ff);
}

.game-card-title {
  color: var(--accent-primary, #0ff);
  font-size: 14px;
}
```
```

### 2. Update CLAUDE.md

Add section on component styling conventions:

```markdown
## Component Styling Conventions

When building UI components for the sandbox:

1. **Exploration phase** - Use inline styles for rapid iteration
2. **Before commit** - Extract styles to `dev/resources/public/styles.css`
3. **Naming convention** - BEM-like: `.component-name`, `.component-name-element`, `.component-name--modifier`
4. **Theme support** - Use CSS custom properties for colors that vary by theme
5. **Clean hiccup** - Committed components should have minimal/no inline styles

This ensures copied hiccup is immediately usable without style extraction.
```

### 3. Refactor Existing Components

For each component in `components.edn`:

1. Extract styles to `styles.css` with proper class names
2. Update hiccup to use CSS classes
3. Consolidate Dark/Light variants where possible using CSS custom properties
4. Test in browser to verify appearance matches

### 4. CSS Organization in styles.css

```css
/* ==========================================================================
   Component Library Styles
   ========================================================================== */

/* --------------------------------------------------------------------------
   CSS Custom Properties (Theme Variables)
   -------------------------------------------------------------------------- */
:root {
  /* Dark theme (default) */
  --bg-primary: #0a0a12;
  --bg-secondary: #12081f;
  --accent-cyan: #0ff;
  --accent-magenta: #ff00ff;
  --text-primary: #fff;
  --text-secondary: #ddd;
  --text-muted: #666;
}

.theme-light {
  --bg-primary: #ffffff;
  --bg-secondary: #f0f4f8;
  --accent-cyan: #00cccc;
  --accent-magenta: #cc00cc;
  --text-primary: #333;
  --text-secondary: #555;
  --text-muted: #888;
}

/* --------------------------------------------------------------------------
   Game Card
   -------------------------------------------------------------------------- */
.game-card { ... }
.game-card-header { ... }
.game-card-art { ... }
.game-card-stats { ... }

/* --------------------------------------------------------------------------
   Combat Log
   -------------------------------------------------------------------------- */
.combat-log { ... }
.combat-log-header { ... }
.combat-log-entry { ... }

/* ... etc for each component ... */
```

## Implementation Plan

### Phase 1: Workflow Updates
- [x] Update component-iterate skill with required CSS extraction step
- [x] Add component styling conventions to CLAUDE.md
- [x] Document CSS organization pattern

### Phase 2: CSS Foundation
- [x] Set up CSS custom properties for theme variables in styles.css
- [x] Add section headers for component organization

### Phase 3: Component Refactoring
Refactor each component (extract CSS, update hiccup, verify appearance):
- [x] game-card
- [x] combat-log
- [x] card-type-badges
- [x] player-hud
- [x] action-buttons
- [x] resource-display

### Phase 4: Verification
- [x] All components render correctly in Dark variant
- [x] All components render correctly in Light variant
- [x] Copied hiccup is clean (no inline styles)
- [x] CSS hot-reload works for all components

## Success Criteria

- [x] `styles.css` contains all component styles with clear organization
- [x] All components in `components.edn` use CSS classes instead of inline styles
- [x] Theme variants work via CSS custom properties (`.theme-light` wrapper)
- [x] component-iterate skill documents CSS extraction as required step
- [x] CLAUDE.md includes component styling conventions
- [x] Copying any component yields clean, readable hiccup

## File Changes

```
.claude/skills/component-iterate/SKILL.md  # Add CSS extraction phase
CLAUDE.md                                   # Add styling conventions
dev/resources/public/styles.css             # All component styles
resources/components.edn                    # Refactored components
specs/README.md                             # Add spec to index
```

## Theme Variant Approach

Instead of duplicating entire hiccup for Dark/Light:

**Current (duplicated):**
```clojure
{:examples
 [{:label "Dark"
   :hiccup [:div {:style "background: #0a0a12; color: #0ff;"} ...]}
  {:label "Light"
   :hiccup [:div {:style "background: #fff; color: #009999;"} ...]}]}
```

**Proposed (CSS-driven):**
```clojure
{:examples
 [{:label "Dark"
   :hiccup [:div.game-card ...]}
  {:label "Light"
   :hiccup [:div.game-card.theme-light ...]}]}
```

Or even simpler - single hiccup with theme toggle in sandbox UI.

## Open Questions

1. Should we add a theme toggle to the sandbox chrome (affects all components)?
2. Should variant examples be hiccup-only, or can they include CSS class modifiers?
3. Do we want a lint/validation step that warns about inline styles on commit?

## Relationship to Other Specs

- **006-dynamic-components**: Introduced `:examples` structure this builds on
- **007-copy-hiccup**: Copy feature that benefits from clean hiccup
- **003-css-hot-reload**: CSS infrastructure this leverages
