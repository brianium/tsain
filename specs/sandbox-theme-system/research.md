# Sandbox Theme System - Research

## Problem Statement

The tsain sandbox currently has a single dark theme baked into its styles. Users may prefer a light theme, and more importantly, the current CSS architecture doesn't properly isolate sandbox chrome styles from the component preview area. When tsain is installed as a library in another project, there's potential for style conflicts between the sandbox UI and the host project's styles.

## Requirements

### Functional Requirements

1. Theme dropdown in the sandbox top bar that toggles between "Light" and "Dark"
2. Theme selection persists across browser sessions
3. Sandbox UI (header, sidebar, controls) themed independently from preview content
4. Both themes must be visually polished and attractive

### Non-Functional Requirements

- **Isolation:** Sandbox styles must not leak into component preview area
- **Portability:** Works when tsain is installed as a library in any Clojure project
- **Performance:** Theme switching should be instant (no flash of unstyled content)
- **Maintainability:** Theme definitions should be easy to understand and modify

## Current Architecture

### Files to Examine

- `resources/tsain/sandbox.css` - Sandbox chrome styles (distributed with library)
- `dev/resources/public/styles.css` - Component styles (project-specific)
- `src/clj/ascolais/tsain/views.clj` - View rendering, HTML structure

### Questions to Answer

- [ ] How is the current dark theme implemented?
- [ ] What CSS custom properties exist?
- [ ] How is the sandbox HTML structured (where could scoping be applied)?
- [ ] What Datastar signals are already in use?

## Options Considered

### Option A: CSS Custom Properties with Data Attribute

**Description:** Use `[data-sandbox-theme="dark"]` on the sandbox root element. Define all theme values as CSS custom properties scoped to this attribute.

```css
[data-sandbox-theme="dark"] {
  --sandbox-bg: #1a1a2e;
  --sandbox-text: #e0e0e0;
}
[data-sandbox-theme="light"] {
  --sandbox-bg: #f5f5f5;
  --sandbox-text: #1a1a1a;
}
```

**Pros:**
- Clean separation via attribute selector
- Easy to switch themes (just change the attribute)
- CSS custom properties cascade naturally
- Works with Datastar signals

**Cons:**
- Requires prefixing all sandbox CSS properties with `--sandbox-`
- Need to audit existing CSS for conflicts

### Option B: Shadow DOM Isolation

**Description:** Render sandbox chrome inside a Shadow DOM to completely isolate its styles.

**Pros:**
- Complete style isolation
- No risk of conflicts

**Cons:**
- Complex to implement with server-side hiccup rendering
- Datastar may have issues crossing shadow boundary
- Overkill for this use case

### Option C: BEM + Prefix Scoping

**Description:** Prefix all sandbox classes with `tsain-sandbox-` and use BEM naming. Theme classes modify the root.

```css
.tsain-sandbox--dark .tsain-sandbox-header { }
.tsain-sandbox--light .tsain-sandbox-header { }
```

**Pros:**
- Simple, well-understood pattern
- No special browser features needed

**Cons:**
- Verbose class names
- Requires renaming all existing classes
- Less elegant than custom properties for theming

### Option D: CSS Layers + Custom Properties

**Description:** Use CSS `@layer` to establish cascade priority, combined with custom properties for theme values.

```css
@layer sandbox-base, sandbox-theme;

@layer sandbox-theme {
  [data-sandbox-theme="dark"] { ... }
}
```

**Pros:**
- Modern CSS feature for managing cascade
- Clear separation of concerns
- Combined with Option A for maximum control

**Cons:**
- Requires understanding of `@layer` semantics

## Recommendation

Based on the analysis above, we recommend **Option A (CSS Custom Properties with Data Attribute)** potentially combined with **Option D (CSS Layers)** because:

1. Custom properties are the standard way to implement theming
2. Data attributes integrate well with Datastar signals
3. Scoping via attribute selector prevents leakage without Shadow DOM complexity
4. CSS layers provide additional cascade control if needed

## Open Questions

- [ ] Does the current sandbox.css use any non-prefixed properties that could conflict?
- [ ] What's the current HTML structure of the sandbox chrome?
- [ ] Are there any existing Datastar signals for UI state?
- [ ] Should the theme dropdown be a new component or inline?

## References

- [CSS Custom Properties Guide](https://developer.mozilla.org/en-US/docs/Web/CSS/Using_CSS_custom_properties)
- [CSS Cascade Layers](https://developer.mozilla.org/en-US/docs/Learn/CSS/Building_blocks/Cascade_layers)
- [Datastar Signals](https://data-star.dev/)
- Current sandbox: `resources/tsain/sandbox.css`
