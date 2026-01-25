# Migrate Existing Component Library - Implementation Plan

## Overview

Implementation plan for the component migration agent. This agent ships with tsain and can be used by any project adopting the modern architecture.

## Prerequisites

- [ ] barrel-imports-in-skill spec implemented
- [ ] Target project has tsain.edn configured
- [ ] Target project has barrel import structure set up
- [ ] REPL available for verification

## Phase 1: Design Agent Interface

- [ ] Define agent file location: `.claude/agents/migrate-component.md`
- [ ] Define invocation pattern: `claude --agent migrate-component "component-name"`
- [ ] Define agent prompt structure with placeholders
- [ ] Define agent tool permissions (Read, Write, Edit, Bash, Glob, Grep)
- [ ] Document expected inputs and outputs

### Agent Prompt Template

```markdown
# Component Migration Agent

## Context
- Source namespace: {{source-namespace}} (from tsain.edn :ui-namespace)
- Component name: {{component-name}} (from invocation argument)
- Barrel structure: {{ui-namespace}}.{category}
- CSS barrel: components/{category}.css

## Task
Transform the legacy chassis alias `::{{component-name}}` into an html.yeah
defelem, extract to the appropriate category, and verify rendering.

## Steps
1. Read source component from {{source-namespace}}
2. Analyze props and infer malli schema
3. Determine category (ask if unclear)
4. Generate defelem code
5. Write to category namespace
6. Update barrel requires
7. Extract CSS to category stylesheet
8. Update CSS barrel imports
9. Verify rendering via REPL
10. Delete old defmethod
11. Report completion

## Schema Inference Rules
[Include rules from research.md]

## Category Heuristics
[Include heuristics from research.md]
```

## Phase 2: Implement Code Transformation

- [ ] Create schema inference logic documentation
- [ ] Document prop type inference from:
  - Destructuring patterns
  - Function usage (name, str, if, for)
  - Example data in components.edn
- [ ] Handle special cases:
  - Optional props (used with `when` or `or`)
  - Enum props (used with `case` or known values from examples)
  - Nested maps (accessed with `(:key prop)`)
  - Vectors (iterated with `for` or `map`)
- [ ] Handle children/content parameter → `(hy/children)`

### Transformation Checklist (per component)

```
[ ] Read defmethod source
[ ] Identify all :component/keys in destructuring
[ ] Map each prop to malli type
[ ] Generate defelem with:
    [ ] :doc string
    [ ] :keys destructuring
    [ ] :as attrs binding
    [ ] Proper optional markers
[ ] Replace content parameter with (hy/children) if used
[ ] Test schema validates against example data
```

## Phase 3: Integrate with Barrel Structure

- [ ] Agent reads tsain.edn for :ui-namespace
- [ ] Agent derives barrel paths from namespace:
  - `sandbox.ui` → `src/clj/sandbox/ui.clj` (barrel)
  - `sandbox.ui` → `src/clj/sandbox/ui/` (category namespaces)
- [ ] Agent maintains category → namespace mapping:
  ```clojure
  {:cards     "sandbox.ui.cards"
   :controls  "sandbox.ui.controls"
   :feedback  "sandbox.ui.feedback"
   :navigation "sandbox.ui.navigation"
   :data      "sandbox.ui.data"
   :layout    "sandbox.ui.layout"
   :display   "sandbox.ui.display"}
  ```
- [ ] Agent adds require to barrel namespace if not present
- [ ] Agent creates category namespace if not present

### Namespace Template

```clojure
(ns {{ui-namespace}}.{{category}}
  "{{Category}} components - migrated from legacy chassis aliases"
  (:require [html.yeah :as hy]))

;; Components will be added here by migration agent
```

## Phase 4: Add CSS Extraction

- [ ] Agent reads stylesheet path from tsain.edn
- [ ] Agent identifies CSS classes used in component hiccup
- [ ] Agent searches stylesheet for matching selectors
- [ ] Agent extracts related selectors (variants, states, children)
- [ ] Agent writes to `components/{category}.css`
- [ ] Agent adds @import to `components/index.css` if not present
- [ ] Agent removes extracted CSS from source stylesheet

### CSS Extraction Rules

```
1. Find all class names in component hiccup
2. For each class, extract:
   - Base selector: .class-name
   - Variant selectors: .class-name--variant
   - State selectors: .class-name:hover, :focus, :disabled
   - Child selectors: .class-name > *, .class-name .child
3. Preserve selector order from original
4. Include any CSS custom properties defined in extracted rules
```

## Phase 5: Parallel Orchestration

- [ ] Document orchestration pattern in CLAUDE.md
- [ ] Add instructions for parsing `defmethod c/resolve-alias` definitions
- [ ] Define dependency detection heuristics (which components use which)
- [ ] Document parallel Task spawning pattern
- [ ] Add progress tracking/reporting guidance

### Orchestration Prompt Template (for CLAUDE.md)

When user asks to migrate all components from a namespace:

1. Read the source file and extract component names:
   ```clojure
   ;; Look for patterns like:
   (defmethod c/resolve-alias ::button ...)
   (defmethod c/resolve-alias ::input ...)
   ```

2. Categorize by likely dependencies:
   - **Tier 1 (no deps):** icon, badge, skeleton
   - **Tier 2 (may use tier 1):** button, input, toggle, avatar
   - **Tier 3 (may use tier 1-2):** toast, alert, card
   - **Tier 4 (may use anything):** modal, popover, table

3. Spawn Task agents:
   - Run tier 1 in parallel (all at once)
   - Wait for completion
   - Run tier 2 in parallel
   - Continue through tiers

4. After all complete, run data migration:
   ```clojure
   (dispatch [[::tsain/migrate-from-edn "path/to/components.edn"]])
   ```

## Phase 6: Testing and Documentation

- [ ] Test single component migration (badge or icon)
- [ ] Test parallel migration of tier 1 components
- [ ] Test full orchestrated migration on tsain's 9 components
- [ ] Document edge cases and workarounds
- [ ] Add orchestration instructions to CLAUDE.md
- [ ] Create example migration walkthrough

### Verification Steps

After each component migration:
```clojure
;; 1. Verify defelem is registered
(hy/element :namespace/component-name)
;; Should return schema + metadata

;; 2. Verify rendering works
(dispatch [[::tsain/preview [:namespace/component-name {...sample-props}]]])
;; Should render without errors

;; 3. Verify examples migrate
(dispatch [[::tsain/migrate-from-edn "path/to/components.edn"]])
(tsain/describe :namespace/component-name)
;; Should show merged html.yeah + SQLite data
```

## Rollout Plan

1. Implement agent in tsain project
2. Test on tsain's own components (9 components)
3. Document lessons learned
4. Ship with next tsain release
5. Use on spectacular (40+ components)
6. Iterate based on real-world usage

## Rollback Plan

Each component transformation is a separate commit:
```bash
git revert <commit-hash>  # Undo single component
```

Full rollback:
```bash
git checkout <pre-migration-branch>  # Return to legacy state
```

## Success Criteria

- [ ] Agent successfully transforms 90%+ of components without manual fixes
- [ ] Transformed components render identically to originals
- [ ] Discovery API works for all transformed components
- [ ] CSS extraction preserves all visual styling
- [ ] Migration can be paused/resumed at any point
