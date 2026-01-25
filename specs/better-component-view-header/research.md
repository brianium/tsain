# Better Component View Header - Research

## Problem Statement

The current component view header has several UX issues:
1. **Duplicate description** - Shows in header AND in Props tab
2. **Redundant navigation** - Prev/next buttons unnecessary with good sidebar
3. **Hidden variants** - Dropdown hides available examples until clicked
4. **Cluttered layout** - Too many elements competing for attention

## Requirements

### Functional Requirements

1. Component name must be prominently displayed
2. Description shown once (not duplicated)
3. All variant/example options visible and selectable
4. Clear visual indication of currently selected variant

### Non-Functional Requirements

- Performance: No additional API calls
- Accessibility: Keyboard navigable variant selection
- Responsiveness: Works at various widths

## Options Considered

### Option A: Compact Header with Inline Dropdown

```
component-name                    [Example ▼]
Description here...

[Preview] [Code] [Props]
```

**Pros:**
- Minimal changes
- Familiar pattern

**Cons:**
- Still hides variants in dropdown
- Doesn't solve discoverability

### Option B: Variant Chips/Pills

```
component-name
Description here...

[Default] [Dark] [Large] [Disabled]  ← clickable chips

[Preview] [Code] [Props]
```

**Pros:**
- All variants immediately visible
- Easy comparison between variants
- Modern pattern (Storybook, Figma, design system docs)
- Quick single-click switching

**Cons:**
- Takes more vertical space
- May wrap awkwardly with many variants

### Option C: Integrated Tab Row

```
component-name
Description here...

[Preview] [Code] [Props]  |  Default · Dark · Large

```

**Pros:**
- Compact, single row for tabs + variants
- Clear separation of concerns

**Cons:**
- May get crowded
- Mixing different selection types

## Recommendation

**Option B: Variant Chips** is the best approach because:

1. **Discoverability** - Users immediately see all available examples
2. **Speed** - Single click to switch (no dropdown open/select/close)
3. **Comparison** - Easy to quickly flip between variants
4. **Industry standard** - Matches Storybook, Bit, Figma patterns

## Proposed Layout

```
┌─────────────────────────────────────────────────────────────┐
│  component-name                                             │
│  Description text here (muted, smaller)                     │
│                                                             │
│  Examples: [Default] [Dark] [Compact]  ← active has accent  │
├─────────────────────────────────────────────────────────────┤
│  [Preview]  [Code]  [Props]                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    Tab content area                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Open Questions

- [x] Where to show description? → Header only, remove from Props
- [x] How to handle many variants (5+)? → Wrap to next line, or show first N with "more"
- [x] Keyboard navigation? → Arrow keys between chips when focused

## References

- [Storybook Controls](https://storybook.js.org/docs/essentials/controls) - uses tabs for stories
- [Figma Dev Mode](https://www.figma.com/dev-mode/) - variant chips
- [Bit.dev](https://bit.dev/) - composition pills
