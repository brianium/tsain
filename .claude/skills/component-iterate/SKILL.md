---
name: component-iterate
description: Iterate on hiccup components with live preview, CSS styling, and library commits. Supports both static and dynamic Datastar components with multiple example configurations. Use when building UI components, prototyping designs, or adding to the component library. Keywords: component, preview, iterate, css, hiccup, design, ui, commit, datastar, signals, interactive.
---

# Component Iteration Skill

Drive component development through a REPL-powered iteration loop with live browser preview, CSS hot-reload, and persistent component library commits. Supports both static components and interactive Datastar components with signal testing from the REPL.

## Prerequisites

1. **nREPL running** on port 7888 (use `clj-nrepl-eval --discover-ports` to verify)
2. **Sandbox started** - if not running, evaluate `(dev)` then `(start)` in the REPL
3. **Browser open** at `http://localhost:3000/sandbox`

## Core Workflow

### 1. Preview a Component

Start with a rough sketch using inline styles if needed for rapid exploration:

```bash
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div.my-component
   [:h2 \"Title\"]
   [:p \"Description text\"]])"
```

The component appears instantly in all connected browsers.

### 2. Iterate Based on Feedback

Make changes and re-preview. Each `preview!` call replaces the previous content:

```bash
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div.my-component
   [:h2 \"Better Title\"]
   [:p \"Improved description\"]
   [:button \"Take Action\"]])"
```

### 3. Add CSS to styles.css

**Important:** Inline styles are for exploration only. Before committing, extract styles to CSS.

Edit `dev/resources/public/styles.css` to add component styles:

```css
/* My component */
.my-component {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  max-width: 400px;

  & h2 {
    margin: 0 0 0.5rem 0;
    font-size: 1.25rem;
  }

  & p {
    color: #666;
    margin: 0 0 1rem 0;
  }

  & button {
    background: #3b82f6;
    color: white;
    border: none;
    padding: 0.5rem 1rem;
    border-radius: 4px;
    cursor: pointer;
  }
}
```

CSS changes hot-reload automatically - no need to refresh or re-preview.

### 4. Refactor Component to Use Classes

Update the component to use CSS classes instead of inline styles:

```bash
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div.my-component
   [:h2 \"Better Title\"]
   [:p \"Improved description\"]
   [:button \"Take Action\"]])"
```

### 5. Commit the Component

When satisfied, commit to the library:

```bash
clj-nrepl-eval -p 7888 "(dev/commit! :my-component \"Short description of the component\")"
```

This saves to:
- **Memory:** Available via `(dev/components)` and gallery view
- **Disk:** Persisted to `resources/components.edn`

## REPL API Reference

| Function | Purpose |
|----------|---------|
| `(dev/preview! hiccup)` | Replace preview with new content |
| `(dev/preview-append! hiccup)` | Append to existing preview |
| `(dev/preview-clear!)` | Clear the preview area |
| `(dev/commit! :name)` | Save preview to library |
| `(dev/commit! :name "desc")` | Save with description |
| `(dev/commit! :name {:description "..." :examples [...]})` | Save with multiple examples |
| `(dev/uncommit! :name)` | Remove from library |
| `(dev/show! :name)` | View single component |
| `(dev/show! :name idx)` | View specific example (0-indexed) |
| `(dev/show-all!)` | View gallery |
| `(dev/components)` | List committed component names |
| `(dev/patch-signals! {:key val})` | Patch Datastar signals on all clients |

## File Locations

| File | Purpose |
|------|---------|
| `dev/resources/public/styles.css` | Component CSS (hot-reloads) |
| `dev/src/clj/sandbox/ui.clj` | Chassis alias definitions (structure only) |
| `resources/components.edn` | Persisted component library |

## Best Practices

1. **Start rough, refine iteratively** - Use inline styles initially to explore, then extract to CSS
2. **CSS classes over inline styles** - Committed components should use CSS classes for reusability and hot-reload
3. **Use nested CSS** - The stylesheet supports CSS nesting (`& .child`) for cleaner organization
4. **Descriptive names** - Use kebab-case names like `:user-card`, `:pricing-table`
5. **Add descriptions** - Help future you remember what the component is for
6. **BEM-like naming** - Use `.component-name`, `.component-name-element`, `.component-name--modifier`
7. **Use CSS custom properties** - Leverage theme variables for colors that vary by theme

## Example Session: Static Component

```bash
# Start with rough component
clj-nrepl-eval -p 7888 "(dev/preview! [:div.alert [:p \"Something happened\"]])"

# User: "add an icon and make it a warning style"

# Iterate
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div.alert.alert-warning
   [:span.alert-icon \"⚠️\"]
   [:p \"Something happened\"]])"

# Add CSS to styles.css (Edit tool)
# ... add .alert, .alert-warning, .alert-icon styles ...

# Verify it looks right (CSS hot-reloads)

# Commit when satisfied
clj-nrepl-eval -p 7888 "(dev/commit! :alert-warning \"Warning alert with icon\")"
```

## Dynamic Components with Datastar

For interactive components using Datastar signals:

### 1. Start with Structure

```bash
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div.accordion
   [:button.accordion-header \"Section Title\"]
   [:div.accordion-content \"Hidden content\"]])"
```

### 2. Add CSS for States

Edit `dev/resources/public/styles.css`:

```css
.accordion-content {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.accordion-content.open {
  max-height: 200px;
}
```

### 3. Add Datastar Attributes

```bash
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div.accordion {:data-signals:open \"false\"}
   [:button.accordion-header {:data-on:click \"\\$open = !\\$open\"}
    \"Section Title\"]
   [:div.accordion-content {:data-class:open \"\\$open\"}
    \"Hidden content\"]])"
```

### 4. Test Interactivity from REPL

```bash
# Open the accordion programmatically
clj-nrepl-eval -p 7888 "(dev/patch-signals! {:open true})"

# Close it
clj-nrepl-eval -p 7888 "(dev/patch-signals! {:open false})"
```

### 5. Commit with Multiple Examples

```bash
clj-nrepl-eval -p 7888 "(dev/commit! :accordion
  {:description \"Expandable content section\"
   :examples
   [{:label \"Static\"
     :hiccup [:div.accordion
              [:button.accordion-header \"Title\"]
              [:div.accordion-content \"Content\"]]}
    {:label \"Interactive\"
     :hiccup [:div.accordion {:data-signals:open \"false\"}
              [:button.accordion-header {:data-on:click \"$open = !$open\"} \"Title\"]
              [:div.accordion-content {:data-class:open \"$open\"} \"Content\"]]}]})"
```

The sandbox UI will show a dropdown to switch between Static and Interactive examples.

## Chassis Aliases

For reusable component structure, define aliases in `dev/src/clj/sandbox/ui.clj`:

```clojure
(defmethod c/resolve-alias ::counter
  [_ attrs _content]
  [:div.counter attrs
   [:button.counter-btn "−"]
   [:span.counter-value "0"]
   [:button.counter-btn "+"]])
```

Use the alias with Datastar attributes passed through:

```bash
clj-nrepl-eval -p 7888 "(require '[sandbox.ui])"
clj-nrepl-eval -p 7888 "(dev/preview!
  [:sandbox.ui/counter {:data-signals:count \"0\"}
   [:button {:data-on:click \"\\$count--\"} \"−\"]
   [:span {:data-text \"\\$count\"}]
   [:button {:data-on:click \"\\$count++\"} \"+\"]])"
```

## Creating Theme Variants (Dark/Light Mode)

When building components that need multiple theme variants, establish a color palette first, then create each variant and commit together:

### 1. Define Your Palette

```
Dark Mode:
- Backgrounds: #0a0a12, #1a1d23
- Primary accent: #0ff, #00cccc
- Secondary accent: #ff00ff, #ff0066
- Text: #fff, #ddd, #999

Light Mode:
- Backgrounds: #ffffff, #f0f4f8
- Primary accent: #00cccc, #009999
- Secondary accent: #cc00cc, #cc0066
- Text: #333, #555, #888
```

### 2. Create Dark Variant First

```bash
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div {:style \"background: #0a0a12; padding: 2rem; color: #0ff;\"}
   [:h2 \"Title\"]
   [:p {:style \"color: #ddd;\"} \"Content here\"]])"
```

### 3. Create Light Variant

```bash
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div {:style \"background: #f0f4f8; padding: 2rem; color: #007777;\"}
   [:h2 \"Title\"]
   [:p {:style \"color: #555;\"} \"Content here\"]])"
```

### 4. Commit Both as Examples

```bash
clj-nrepl-eval -p 7888 "
(let [dark-hiccup [:div {:style \"background: #0a0a12; ...\"} ...]
      light-hiccup [:div {:style \"background: #f0f4f8; ...\"} ...]]
  (dev/commit! :my-component
    {:description \"Component with theme variants\"
     :examples [{:label \"Dark\" :hiccup dark-hiccup}
                {:label \"Light\" :hiccup light-hiccup}]}))"
```

The sandbox dropdown lets you toggle between variants.

## CSS Extraction (Required Before Commit)

**IMPORTANT:** All committed components must use CSS classes, not inline styles. This ensures:
- Copied hiccup is clean and immediately usable
- Styles hot-reload during continued iteration
- Consistent theming via CSS custom properties

### Extraction Process

1. **Identify repeated patterns** - Buttons, cards, containers get classes
2. **Name classes semantically** - `.game-card`, `.game-card-header`, `.stat-badge`
3. **Use BEM-like naming** - `.component-name-element--modifier`
4. **Use CSS custom properties for themes** - Colors should reference `--accent-cyan`, `--bg-primary`, etc.
5. **Move inline styles to CSS** - One section per component in `styles.css`

### Example Extraction

**Before (inline styles - exploration phase):**
```clojure
[:div {:style "background: #0a0a12; padding: 16px; border: 1px solid #0ff;"}
 [:h2 {:style "color: #0ff; font-size: 14px;"} "Title"]]
```

**After (CSS classes - commit-ready):**
```clojure
[:div.game-card
 [:h2.game-card-title "Title"]]
```

```css
/* In styles.css */
.game-card {
  background: var(--bg-primary);
  padding: 16px;
  border: 1px solid var(--accent-cyan);
}

.game-card-title {
  color: var(--accent-cyan);
  font-size: 14px;
}
```

### Theme Variants with CSS

Use the `.theme-light` class to switch themes. CSS custom properties automatically update:

```clojure
;; Dark variant (default)
[:div.game-card ...]

;; Light variant
[:div.theme-light
 [:div.game-card ...]]
```

### Available Theme Variables

The stylesheet defines these CSS custom properties in `:root` (dark) and `.theme-light`:

| Variable | Dark | Light | Purpose |
|----------|------|-------|---------|
| `--bg-primary` | `#0a0a12` | `#ffffff` | Main background |
| `--bg-secondary` | `#12081f` | `#f8f4ff` | Secondary background |
| `--accent-cyan` | `#0ff` | `#00cccc` | Primary accent |
| `--accent-magenta` | `#ff00ff` | `#cc00cc` | Secondary accent |
| `--text-primary` | `#fff` | `#333` | Main text |
| `--text-muted` | `#666` | `#888` | Subdued text |

### Utility Classes

The stylesheet includes reusable utility classes:

| Class | Effect |
|-------|--------|
| `.clip-corners-lg` | Cut corner clip path (12px) |
| `.clip-corners-md` | Cut corner clip path (10px) |
| `.clip-corners-sm` | Cut corner clip path (8px) |
| `.clip-hexagon` | Hexagon shape |
| `.clip-diamond` | Diamond shape |
| `.clip-badge` | Elongated hexagon for tags |
| `.clip-octagon` | Octagon for avatars |
| `.clip-asymmetric` | Asymmetric corner cut |
| `.scanline-overlay` | CRT scanline effect |

### When to Use Inline Styles

After CSS extraction, minimal inline styles may remain for:
- Dynamic/data-driven values (e.g., `width: 80%` for progress bars)
- Container padding/layout for demo wrappers
- One-off positioning adjustments

**Avoid inline styles for:**
- Colors (use CSS custom properties)
- Typography (use classes)
- Borders and shadows (use classes)
- Anything that should change with themes

## Browser Verification

Use the Chrome extension to visually verify components during iteration:

1. Take screenshots to see current state
2. Test interactive elements (dropdowns, buttons)
3. Verify responsive behavior by resizing
4. Check both theme variants render correctly

## Related Skills

- **clojure-eval**: General REPL evaluation
- **fx-explore**: Discover sandbox effects with describe/grep/sample
