# Html Yeah Component Writing Actions - Research

## Problem Statement

The tsain SKILL.md currently explains file size management in prose—telling Claude to run `wc -l`, check thresholds, and manually split files. This approach has problems:

1. **Prose gets ignored** - Long instructions are easily skipped or forgotten
2. **Manual checking is error-prone** - Claude may forget to check before adding components
3. **Split procedures are complex** - Multi-step manual processes are easy to get wrong
4. **Inconsistent with CSS workflow** - CSS now uses effect-based writes with hints; components should match

The phandaal-based CSS actions solved this for stylesheets. The same pattern should apply to `hy/defelem` component definitions.

## Requirements

### Functional Requirements

1. `::tsain/write-component` writes a complete `hy/defelem` form to the UI namespace
2. Results include LOC metadata (before, after, delta) from phandaal
3. When threshold exceeded, results include actionable hints with suggested effect
4. `::tsain/split-namespace` extracts components by category to sub-namespaces
5. Split creates barrel requires in the parent namespace
6. Category can be inferred from component name or specified explicitly

### Non-Functional Requirements

- **Consistency**: Same hint structure as CSS (`{:type :split-suggested :category ... :action ...}`)
- **Configurability**: Threshold and paths from `tsain.edn`
- **Library-friendly**: Works when tsain is installed as a dependency, not just for sandbox

## Options Considered

### Option A: Direct File Writes

**Description:** Write component code directly via phandaal's `::phandaal/append`, tracking LOC and checking thresholds inline.

**Pros:**
- Simple implementation
- Consistent with CSS approach
- Phandaal handles formatting and LOC tracking

**Cons:**
- Need to construct valid Clojure code strings
- Must handle namespace declarations carefully

### Option B: Template-Based Generation

**Description:** Use templates to generate `hy/defelem` forms, filling in component name, schema, and body.

**Pros:**
- Structured output
- Less chance of malformed code

**Cons:**
- More complex API (multiple parameters vs code string)
- Templates may be too rigid for varied component structures

### Option C: Hybrid - Code String with Validation

**Description:** Accept code as a string (like CSS) but validate it parses as valid Clojure before writing.

**Pros:**
- Flexible like Option A
- Catches obvious errors
- Claude can write natural `hy/defelem` code

**Cons:**
- Slight overhead for parse check
- May reject valid but unusual code

## Recommendation

**Option A: Direct File Writes** - Keep it simple and consistent with CSS. Claude writes the `hy/defelem` form as a string, and phandaal handles the I/O with LOC tracking. The interceptor adds hints when needed.

Validation isn't critical because:
- Claude writes Clojure code regularly
- REPL errors provide immediate feedback
- The `(reload)` call will fail on syntax errors

## Design Details

### Action Schema

```clojure
;; Write a new component
[::tsain/write-component "(hy/defelem my-card ...)" {:category "cards"}]

;; Category inferred from component name if not specified
[::tsain/write-component "(hy/defelem game-card ...)"]
;; Infers category "cards" from "game-card"
```

### Category Inference

Map component name suffixes/patterns to categories:

| Pattern | Category |
|---------|----------|
| `*-card`, `*-tile`, `*-panel` | cards |
| `*-btn`, `*-button`, `*-input`, `*-select` | controls |
| `*-toast`, `*-alert`, `*-loader` | feedback |
| `*-nav`, `*-menu`, `*-tab` | navigation |
| `*-badge`, `*-avatar`, `*-text` | display |
| `*-modal`, `*-popover`, `*-tooltip` | overlays |

### Split Mechanics

```clojure
[::tsain/split-namespace "cards"]
```

1. Read UI namespace file
2. Find all `hy/defelem` forms where component name matches category patterns
3. Extract matched forms to new file `<ui-namespace>/cards.clj`
4. Add `(:require [<ui-namespace>.cards])` to parent if not present
5. Remove extracted forms from parent
6. Write both files via phandaal

### Hint Structure

```clojure
{:type :split-suggested
 :severity :warning
 :category "cards"
 :target "sandbox/ui/cards.clj"
 :loc {:before 450 :after 480 :delta 30}
 :message "UI namespace exceeds 500 lines. Extract cards components to sandbox.ui.cards"
 :action {:effect ::tsain/split-namespace
          :args ["cards"]}}
```

## Open Questions

- [x] Should we validate Clojure syntax before writing? → No, rely on reload errors
- [ ] How to handle components that span multiple categories? → Allow explicit `:category` override
- [ ] Should split preserve order of components? → Yes, maintain definition order

## References

- [Phandaal CSS Actions Spec](../phandaal-based-css-actions-and-effects/README.md) - Pattern for effect-based writes with hints
- [html.yeah defelem](https://github.com/brianium/html.yeah) - Component definition macro
- Clojure barrel imports pattern - requiring sub-namespaces for organization
