---
name: component-iterate
description: Iterate on hiccup components with live preview, CSS styling, and library commits. Use when building UI components, prototyping designs, or adding to the component library. Keywords: component, preview, iterate, css, hiccup, design, ui, commit.
---

# Component Iteration Skill

Drive component development through a REPL-powered iteration loop with live browser preview, CSS hot-reload, and persistent component library commits.

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
| `(dev/uncommit! :name)` | Remove from library |
| `(dev/show! :name)` | View single component |
| `(dev/show-all!)` | View gallery |
| `(dev/components)` | List committed component names |

## File Locations

| File | Purpose |
|------|---------|
| `dev/resources/public/styles.css` | Component CSS (hot-reloads) |
| `resources/components.edn` | Persisted component library |

## Best Practices

1. **Start rough, refine iteratively** - Use inline styles initially to explore, then extract to CSS
2. **CSS classes over inline styles** - Committed components should use CSS classes for reusability and hot-reload
3. **Use nested CSS** - The stylesheet supports CSS nesting (`& .child`) for cleaner organization
4. **Descriptive names** - Use kebab-case names like `:user-card`, `:pricing-table`
5. **Add descriptions** - Help future you remember what the component is for

## Example Session

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

## Related Skills

- **clojure-eval**: General REPL evaluation
- **fx-explore**: Discover sandbox effects with describe/grep/sample
