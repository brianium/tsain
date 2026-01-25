# Barrel Imports In Skill - Research

## Problem Statement

Component libraries grow over time. A project that starts with 5 components in `ui.clj` and 200 lines in `styles.css` eventually reaches 50+ components and thousands of lines. At this scale:

- **Navigation suffers**: Finding a specific component or style rule takes longer
- **Mental overhead increases**: Harder to hold the file structure in your head
- **Merge conflicts multiply**: Multiple contributors touching the same large file
- **Editor performance degrades**: Some editors struggle with very large files
- **Code review becomes painful**: Reviewing changes in a 3000-line file is tedious

The "barrel import" pattern addresses this by splitting large files into logical chunks while maintaining a single entry point that imports everything.

## Requirements

### Functional Requirements

1. The skill must know the configured threshold (default 1500 lines)
2. The skill must recognize when files approach the threshold
3. The skill must suggest appropriate split strategies for CSS and Clojure
4. The skill must guide the split execution with consistent naming/structure
5. Threshold configurable via `tsain.edn`; paths are convention-based (not configurable)

### Non-Functional Requirements

- **Portability**: Pattern works identically across all tsain projects
- **Simplicity**: Splits should be obvious and easy to understand
- **Backward compatibility**: Existing projects continue working; splits are opt-in
- **IDE friendliness**: Split structure should work well with editor navigation

## Options Considered

### Option A: Category-Based Splits

**Description:** Group components/styles by semantic category (cards, controls, layout, feedback, etc.)

```
dev/resources/public/
  styles.css              # @import "./components/*.css"
  components/
    cards.css
    controls.css
    layout.css
    feedback.css

dev/src/clj/sandbox/
  ui.clj                  # requires ui.cards, ui.controls, etc.
  ui/
    cards.clj
    controls.clj
    layout.clj
    feedback.clj
```

**Pros:**
- Stable groupings that don't change often
- Easy to find related components
- Natural for teams to own categories
- Scales well (add new categories as needed)

**Cons:**
- Requires judgment about which category a component belongs to
- Some components span categories (where does a "card with controls" go?)

### Option B: Per-Component Splits

**Description:** One file per component, barrel imports everything

```
dev/resources/public/
  styles.css              # @import "./components/*.css"
  components/
    game-card.css
    player-hud.css
    action-button.css
    ...

dev/src/clj/sandbox/
  ui.clj                  # requires all ui.* namespaces
  ui/
    game_card.clj
    player_hud.clj
    action_button.clj
    ...
```

**Pros:**
- No ambiguity about where a component lives
- Maximum granularity
- Easy automated tooling (one component = one file)

**Cons:**
- Many small files (50+ components = 50+ files)
- More requires/imports to manage
- Harder to see related components together
- Overkill for simple components

### Option C: Hybrid (Category with Per-Component for Complex)

**Description:** Default to categories, but allow individual component files for complex cases

```
dev/src/clj/sandbox/
  ui.clj                  # main barrel
  ui/
    cards.clj             # simple card components
    controls.clj          # buttons, inputs, etc.
    game_card.clj         # complex component gets own file
```

**Pros:**
- Flexibility where needed
- Simpler components stay grouped
- Complex components get space to breathe

**Cons:**
- Inconsistent structure
- Harder to document clear rules
- "When is something complex enough?" is subjective

## Recommendation

**Option A: Category-Based Splits** is the recommended approach because:

1. **Predictable structure** - New contributors know where to look
2. **Stable over time** - Categories change less often than individual components
3. **Right granularity** - Not too many files, not too few
4. **Matches mental model** - Developers think in terms of "I need a card" or "I need a button"

For the rare case where a component is truly complex (200+ lines of styles, multiple sub-components), it can graduate to its own file within the category directory.

### Suggested Categories

| Category | Contains |
|----------|----------|
| `cards` | Card-based layouts, tiles, panels |
| `controls` | Buttons, inputs, selects, toggles |
| `layout` | Grids, containers, spacing utilities |
| `feedback` | Toasts, alerts, loaders, progress |
| `navigation` | Menus, tabs, breadcrumbs, pagination |
| `display` | Text treatments, badges, avatars, icons |
| `overlays` | Modals, popovers, tooltips, drawers |

Projects add categories as needed. Start with what you have, split when a category grows.

## Convention Over Configuration

Since Claude is the only consumer of this guidance, we favor convention over configuration:

| Aspect | Convention | Rationale |
|--------|------------|-----------|
| CSS split directory | Always `components/` | Universal, no thought required |
| Namespace split path | Derived from `:ui-namespace` | Follows Clojure's namespace→path rules |
| Category naming | Semantic (cards, controls, etc.) | Discoverable without docs |

**Only configurable:** `:split-threshold` (default 1500, nil to disable)

This eliminates two config keys (`:css-split-dir`, `:ns-split-dir`) that would add cognitive overhead without meaningful flexibility.

## Open Questions

- [x] What's the right default threshold? → 1500 lines
- [x] Should we track line counts automatically? → No, Claude checks on commit
- [x] Should split directories be configurable? → No, use conventions
- [ ] How do we handle CSS custom properties (variables)? → Keep in main `styles.css` or dedicated `variables.css`
- [ ] Should the skill auto-detect current file sizes? → Nice to have, not required

## References

- [CSS @import MDN](https://developer.mozilla.org/en-US/docs/Web/CSS/@import)
- [Clojure namespace organization](https://clojure.org/guides/ns_form)
- Barrel pattern from TypeScript/JavaScript ecosystem (index.ts re-exports)
