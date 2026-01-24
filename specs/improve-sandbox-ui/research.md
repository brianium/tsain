# Improve Sandbox UI - Research

## Problem Statement

The tsain sandbox UI has functional controls but lacks visual polish. Buttons and controls look dated, the component preview background doesn't adapt to different themes, and navigation is limited (no search, no scrolling sidebar). We want to improve the developer experience without disrupting the core contract that allows tsain to plug into existing apps.

## Current State

### What Works Well
- Sandbox theme adapts to component library theme
- Preview commit status UI with manual naming
- Theme variant switcher in components view
- Copy/delete buttons in single component view

### Pain Points
- Ugly buttons and controls (text-only, no icons)
- Fixed white grid background in component preview doesn't work with dark themes
- Component sidebar doesn't scroll
- No way to search/filter components

## Requirements

### Functional Requirements

1. Add icon support to sandbox buttons (back, copy, delete, commit, clear, etc.)
2. Add runtime color picker for preview background (persist in localStorage)
3. Make component sidebar scrollable
4. Add text search to filter components in sidebar

### Non-Functional Requirements

- **Minimal footprint:** Don't bloat the sandbox with heavy dependencies
- **Server-side compatible:** Everything must work with hiccup (no React/JS frameworks)
- **Non-invasive:** Don't change the core tsain plugin contract
- **Self-contained:** Sandbox styling stays in sandbox CSS, not component styles

## Options Considered

### Option A: Lucide Icons (Inline SVG)

**Description:** Use [Lucide](https://lucide.dev/icons/) icons as inline SVG hiccup. Create a small `sandbox/icons.clj` namespace with ~10-15 icons we need, stored as hiccup vectors.

**Pros:**
- Zero runtime JS dependency
- Icons are just data (hiccup vectors)
- `stroke="currentColor"` inherits text color automatically
- Tree-shaken by nature (only include what we use)
- Beautiful, consistent icon set (fork of Feather)

**Cons:**
- Manual copy/paste of SVG paths into Clojure
- Need to maintain icon definitions

**Example:**
```clojure
(def icon-copy
  [:svg {:xmlns "http://www.w3.org/2000/svg"
         :width "16" :height "16"
         :viewBox "0 0 24 24"
         :fill "none" :stroke "currentColor"
         :stroke-width "2" :stroke-linecap "round" :stroke-linejoin "round"}
   [:rect {:x "9" :y "9" :width "13" :height "13" :rx "2" :ry "2"}]
   [:path {:d "M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"}]])
```

### Option B: Open Props CSS Variables

**Description:** Use [Open Props](https://open-props.style/) for design tokens (spacing, colors, shadows, easings). Include via CDN in sandbox page only.

**Pros:**
- Just CSS custom properties (no JS)
- Comprehensive design tokens
- JIT compiler available to only ship used props
- Can cherry-pick just what we need

**Cons:**
- Another CDN dependency
- May overlap with existing theme variables
- Overkill if we only need a few tokens

### Option C: Minimal Custom Approach

**Description:** Skip external libraries. Hand-craft the ~5-10 icons we need as inline SVG and enhance existing CSS.

**Pros:**
- Zero dependencies
- Full control
- Smallest footprint

**Cons:**
- More manual work
- Icons may be inconsistent if designed ad-hoc

## Recommendation

**Use Option A (Lucide inline SVG) + enhanced sandbox CSS.**

Skip Open Props for now - our existing CSS variables are sufficient and adding another token system creates confusion. Instead:

1. Create `dev/src/clj/sandbox/icons.clj` with ~15 Lucide icons as hiccup
2. Update sandbox buttons to use icons + text labels
3. Add CSS for scrollable sidebar
4. Add Datastar-powered search filter (signals + data-show)
5. Add background color picker using native `<input type="color">` + localStorage

This keeps the sandbox self-contained while improving UX significantly.

## Icons Needed

| Icon | Use Case |
|------|----------|
| `chevron-left` | Back navigation |
| `chevron-right` | Forward navigation |
| `copy` | Copy button |
| `trash-2` | Delete button |
| `save` | Commit button |
| `x` | Clear/close |
| `search` | Search input |
| `chevrons-left` | Collapse sidebar |
| `chevrons-right` | Expand sidebar |
| `palette` | Color picker toggle |
| `check` | Success indicator |
| `eye` | Preview tab |
| `grid-3x3` | Components tab |

## Open Questions

- [x] Can we use Lucide without JS? → Yes, inline SVG as hiccup
- [x] Is Open Props worth adding? → No, stick with existing CSS vars
- [ ] Should color picker be in navbar or floating?
- [ ] Default background color for preview area?

## References

- [Lucide Icons](https://lucide.dev/icons/) - Icon library
- [Lucide Static Package](https://lucide.dev/guide/packages/lucide-static) - No-JS usage
- [Open Props](https://open-props.style/) - CSS custom properties (not using)
- [Open Props UI](https://open-props-ui.netlify.app/) - Component patterns for inspiration
