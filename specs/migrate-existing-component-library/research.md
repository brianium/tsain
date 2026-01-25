# Migrate Existing Component Library - Research

## Problem Statement

Projects like `spectacular` have mature component libraries with:
- 40+ components defined as `defmethod c/resolve-alias`
- Rich examples stored in `components.edn`
- CSS styles in a monolithic stylesheet
- No prop schemas or discovery metadata

Migrating to tsain's new architecture requires:
1. **Code transformation**: chassis → defelem with malli schemas
2. **Namespace restructuring**: monolithic → barrel imports by category
3. **CSS restructuring**: monolithic → barrel imports by category
4. **Data migration**: EDN → SQLite (handled by existing effect)

This is substantial work that benefits from automation while requiring human review.

## Requirements

### Functional Requirements

1. Agent must transform one component at a time (reviewable chunks)
2. Agent must generate correct malli schemas from prop usage analysis
3. Agent must categorize components (cards, controls, feedback, etc.)
4. Agent must extract component-specific CSS
5. Existing `migrate-from-edn` must work after code transformation
6. Components must render identically before and after transformation

### Non-Functional Requirements

- Incremental: Can stop/resume migration at any point
- Reversible: Git history allows rollback per component
- Testable: Each transformation can be verified independently

## Agent Design

### Location

```
.claude/agents/migrate-component.md
```

### Invocation

```bash
# From project root
claude --agent migrate-component "button"
```

### Agent Responsibilities

1. **Locate** the component in the monolithic namespace
2. **Analyze** prop usage to infer schema types
3. **Generate** defelem with:
   - Malli schema for all props
   - `:doc` string from existing description or inference
   - Proper destructuring via `:keys`
4. **Categorize** the component (ask user if unclear)
5. **Extract** to category namespace (e.g., `brianium.views.components.controls`)
6. **Extract CSS** to category stylesheet (e.g., `components/controls.css`)
7. **Update** barrel namespace requires
8. **Verify** rendering still works
9. **Delete** old defmethod from monolithic namespace

### Agent Context Needs

The agent needs access to:
- Source component code (Read)
- Target namespace structure (Glob, Read)
- CSS stylesheet (Read)
- REPL for verification (Bash with nREPL)
- File editing (Edit, Write)

### Sample Agent Prompt Structure

```markdown
# Component Migration Agent

You are migrating a component from legacy chassis aliases to html.yeah defelem.

## Source Analysis

Read the component at `::{{component-name}}` in `{{source-namespace}}`.
Identify:
- All props accessed via destructuring
- Prop types from usage patterns
- Whether it uses children/content
- Associated CSS classes

## Target Generation

Generate a `hy/defelem` with:
- Malli schema for all identified props
- `:doc` from component description or inferred purpose
- `:keys` destructuring matching existing patterns

## Categorization

Assign to a category:
- `cards` - Container/card components
- `controls` - Buttons, inputs, toggles, selects
- `feedback` - Toasts, alerts, modals, progress
- `navigation` - Tabs, breadcrumbs, pagination, steppers
- `data` - Tables, lists, timelines
- `layout` - Drawers, accordions, grids
- `display` - Icons, avatars, badges, skeletons

## Extraction

1. Write defelem to `{{ui-namespace}}.{{category}}`
2. Add require to `{{ui-namespace}}` barrel
3. Extract CSS to `components/{{category}}.css`
4. Add @import to `components/index.css`
5. Delete old defmethod

## Verification

Render the component with sample props and verify output matches.
```

## Schema Inference Patterns

### From Destructuring

```clojure
;; Source
(let [{:button/keys [label variant size icon]} attrs]
  ...)

;; Inferred schema
[:map {:keys [button/label button/variant button/size button/icon]}
 [:button/label :string]
 [:button/variant {:optional true} [:enum :primary :secondary :ghost]]
 [:button/size {:optional true} [:enum :sm :md :lg]]
 [:button/icon {:optional true} :keyword]]
```

### From Usage Patterns

| Usage Pattern | Inferred Type |
|---------------|---------------|
| `(name x)` | `:keyword` |
| `(str x)` | `:string` |
| `(:key x)` | `:map` |
| `(for [item x] ...)` | `[:vector :any]` |
| `(if x ...)` | `:boolean` |
| `"prefix-" x` (in class) | `:string` or `:keyword` |

### From Example Data

The `components.edn` examples provide concrete values that clarify types:
```clojure
{:button/variant :primary}  ;; → [:enum ...] with :primary as known value
{:button/label "Save"}      ;; → :string
{:button/icon :check}       ;; → :keyword
```

## CSS Extraction Strategy

### Identifying Component CSS

1. Find all CSS classes used in the component hiccup
2. Search stylesheet for those class selectors
3. Include related selectors (variants, states, children)

### Example

```clojure
;; Component uses these classes
[:button.btn {:class (str "btn--" (name variant))}]

;; Extract these CSS blocks
.btn { ... }
.btn--primary { ... }
.btn--secondary { ... }
.btn--ghost { ... }
.btn:hover { ... }
.btn:disabled { ... }
```

## Category Assignment Heuristics

| Component Pattern | Category |
|-------------------|----------|
| `card`, `panel`, `box` | cards |
| `button`, `input`, `toggle`, `checkbox`, `radio`, `select` | controls |
| `toast`, `alert`, `modal`, `progress`, `skeleton` | feedback |
| `tabs`, `breadcrumb`, `pagination`, `stepper`, `nav` | navigation |
| `table`, `list`, `timeline` | data |
| `drawer`, `accordion`, `grid` | layout |
| `icon`, `avatar`, `badge`, `tag` | display |

## Migration Order Recommendation

1. **Display components first** (icon, avatar, badge) - simplest, fewest dependencies
2. **Controls** (button, input, toggle) - commonly composed into others
3. **Feedback** (toast, alert, modal) - often use controls
4. **Navigation** (tabs, breadcrumb) - mid-complexity
5. **Data** (table, timeline) - complex, may compose many others
6. **Cards/Layout** - containers that compose everything

## Parallel Orchestration

Claude Code's Task tool supports spawning multiple agents in a single message. This enables natural language orchestration:

**User prompt:**
> "Migrate all components from `src/clj/brianium/views/components.clj`"

**Claude's orchestration steps:**
1. Read the source namespace file
2. Parse all `defmethod c/resolve-alias ::component-name` definitions
3. Optionally sort by dependency order (display → controls → feedback → etc.)
4. Spawn N parallel Task agents, each with the migration prompt for one component
5. Monitor completion and report results

**Parallel vs Sequential:**

For components with no dependencies on each other, spawn in parallel:
```
Task: migrate icon      ─┐
Task: migrate badge     ─┼─► all run concurrently
Task: migrate avatar    ─┘
```

For components with dependencies, use `run_in_background` and sequence:
```
Task: migrate button (wait)
Task: migrate popover (uses button, can start after)
```

**Batching considerations:**
- Parallel is faster but uses more resources
- Could batch by category (all display components together)
- Could respect dependency order within batches

## Open Questions

- [x] Does migrate-from-edn work with new namespace structure? Yes, it reads from EDN and writes to SQLite; namespace structure is irrelevant
- [x] Can we orchestrate parallel migrations? Yes, via Task tool parallel spawning
- [ ] How to handle components that compose other components? (e.g., popover uses button) - Use dependency-aware sequencing
- [ ] How to handle CSS custom properties that span components? - Keep shared properties in `components/_variables.css`

## Alternatives Considered

### Alternative A: Full automation with batch processing

**Description:** Single agent invocation transforms all components.

**Pros:**
- Faster overall
- Consistent decisions

**Cons:**
- Too much to review at once
- Hard to debug failures
- Can't pause/resume

**Verdict:** Rejected - human review is essential for quality

### Alternative B: Manual transformation with templates

**Description:** Provide templates but have humans do the work.

**Pros:**
- Maximum control
- No agent complexity

**Cons:**
- Very slow
- Repetitive and error-prone
- Humans will make inconsistent decisions

**Verdict:** Rejected - too slow for 40+ components

### Alternative C: Agent per component with human review (Recommended)

**Description:** Agent transforms one component, human reviews, repeat.

**Pros:**
- Reviewable chunks
- Can course-correct early
- Git history shows clear progression
- Can parallelize with multiple terminals

**Cons:**
- More invocations
- Slower than batch

**Verdict:** Accepted - best balance of speed and quality

## References

- [barrel-imports-in-skill spec](../barrel-imports-in-skill/README.md)
- [html.yeah documentation](../../CLAUDE.md#htmlyeah-schema-driven-components)
- [tsain discovery API](../../CLAUDE.md#tsain-discovery-api)
