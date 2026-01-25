---
name: tsain
description: REPL-driven component development with live preview, CSS styling, and library commits. Supports spec-driven implementation workflow. Keywords: component, preview, iterate, css, hiccup, design, ui, commit, datastar, signals, interactive, alias, implement, spec.
---

# Tsain Component Development

Drive component development through an **alias-first** REPL-powered iteration loop. Component structure lives in a UI namespace as chassis aliases, while the tsain library stores lean alias invocations with config props.

## Commands

This skill supports commands via arguments:

- `/tsain` - Show available commands
- `/tsain implement` - Spec-driven implementation workflow
- `/tsain iterate` - Direct component iteration

If no argument is provided, show available commands.

---

## `/tsain implement`

End-to-end workflow for implementing components from specs to production.

### Steps

1. **Run `/specs implement`** to identify the next spec to work on
2. **Use `/tsain iterate`** to develop the component
3. **Use `/clojure-eval`** for REPL interaction
4. **Check file sizes** before committing - see File Size Management section
5. **Update CLAUDE.md** with component reference when done
6. **Commit** means: commit to component library with tsain AND git

Do not assume a REPL connection needs to be restarted. Always run `clj-nrepl-eval --discover-ports` before your first REPL expression. The REPL is likely still running. If it is not, stop and ask the user what they want you to do.

---

## `/tsain iterate`

Direct component iteration workflow for developing a single component.

### Configuration

Read `tsain.edn` at project root for file locations:

```clojure
;; tsain.edn
{:ui-namespace myapp.ui        ;; Where chassis aliases live
 :database-file "tsain.db"     ;; Component library (SQLite)
 :stylesheet "resources/styles.css"  ;; CSS for hot reload
 :port 3000
 :split-threshold 1500}        ;; Lines before suggesting barrel splits (nil to disable)
```

Alternative legacy configuration:
```clojure
{:ui-namespace myapp.ui
 :components-file "resources/components.edn"  ;; EDN-based storage
 :stylesheet "resources/styles.css"}
```

### Prerequisites

1. **Discover nREPL port first** - run `clj-nrepl-eval --discover-ports` before your first REPL expression
2. **Sandbox started** - if not running, evaluate `(dev)` then `(start)` in the REPL
3. **Browser open** at `http://localhost:3000/sandbox` (or your configured port)

### Discovering the API

Use the tsain discovery functions to explore available effects:

```clojure
(require '[ascolais.tsain :as tsain])

;; List all components with schemas and docs
(tsain/describe)

;; Get details for a specific component
(tsain/describe :myapp.ui/card)

;; Search by keyword in descriptions
(tsain/grep "button")

;; Find components with specific props
(tsain/props :variant)

;; List all categories
(tsain/categories)

;; Filter by category
(tsain/by-category "cards")
```

For effect discovery (dispatch-level):

```clojure
(require '[ascolais.sandestin :as s])

;; List all effects in the dispatch
(s/describe (dispatch))

;; Inspect specific effect
(s/describe (dispatch) ::tsain/preview)

;; Generate example invocation
(s/sample (dispatch) ::tsain/preview)

;; Search effects
(s/grep (dispatch) "component")
```

### Alias-First Workflow

#### Step 0: Read Configuration

First, read `tsain.edn` to find the correct file paths:

```bash
cat tsain.edn
```

The `:ui-namespace` tells you where to add aliases. The `:stylesheet` tells you where to add CSS.

#### Step 1: Define the Chassis Alias (Required First Step)

Before iterating on visuals, define the component structure in the components namespace (from `:ui-namespace`). This is a production namespace, so aliases you define here can be used directly in your application views.

**Key conventions:**
- **Namespaced attrs** (`:card/title`) = config props (elided from HTML output)
- **Regular attrs** (`:data-on:click`, `:class`) = pass through to HTML
- **Namespace by component name** for self-documenting code

For simple components, use `defmethod`:

```clojure
;; In your ui namespace
(defmethod c/resolve-alias ::my-card
  [_ attrs _]
  (let [{:my-card/keys [title body]} attrs]
    [:div.my-card attrs  ;; namespaced keys auto-elided by chassis
     [:h2.my-card-title title]
     [:p.my-card-body body]]))
```

For components with schema validation, use `html.yeah/defelem`:

```clojure
(require '[html.yeah :as hy])

(hy/defelem my-card
  [:map {:doc "Card with title and body"
         :keys [my-card/title my-card/body]}
   [:my-card/title :string]
   [:my-card/body :string]]
  [:div.my-card attrs
   [:h2.my-card-title my-card/title]
   [:p.my-card-body my-card/body]])
```

After adding the alias, reload the namespace:

```bash
clj-nrepl-eval -p <PORT> "(reload)"
```

#### Step 2: Preview with Alias Invocation

Use the alias with config props. Replace `myapp.ui` with your `:ui-namespace`:

```bash
clj-nrepl-eval -p <PORT> "(dispatch [[::tsain/preview
  [:myapp.ui/my-card
   {:my-card/title \"Hello World\"
    :my-card/body \"A simple example\"}]]])"
```

#### Step 3: Iterate on Structure and CSS

1. **Modify the alias** in the UI namespace to adjust structure
2. **Reload**: `clj-nrepl-eval -p <PORT> "(reload)"`
3. **Re-preview** to see changes
4. **Add CSS** to the stylesheet (from `:stylesheet`) - hot-reloads automatically

#### Step 4: Check File Sizes (Before Commit)

Before committing, check if files are approaching the split threshold:

```bash
wc -l <stylesheet-path>   # e.g., dev/resources/public/styles.css
wc -l <ui-namespace-path> # e.g., dev/src/clj/sandbox/ui.clj
```

If either file exceeds or approaches `:split-threshold` (default 1500 lines), consider splitting before adding more components. See the **File Size Management** section for split procedures.

#### Step 5: Commit to Library

```bash
clj-nrepl-eval -p <PORT> "(dispatch [[::tsain/commit :my-card
  {:description \"Card with title and body\"
   :category \"cards\"
   :examples
   [{:label \"Default\"
     :hiccup [:myapp.ui/my-card
              {:my-card/title \"Hello\"
               :my-card/body \"World\"}]}
    {:label \"Light Theme\"
     :hiccup [:div.theme-light {:style \"padding: 20px; background: #f0f4f8;\"}
              [:myapp.ui/my-card
               {:my-card/title \"Hello\"
                :my-card/body \"World\"}]]}]}]])"
```

**Result:** The library stores the lean alias form. Copying from the sandbox UI gives you clean, portable hiccup.

---

## Config Props vs HTML Attrs

```clojure
[:myapp.ui/game-card
 {;; Config props (namespaced) - elided from HTML output
  :game-card/title "Neural Phantom"
  :game-card/attack "3"

  ;; HTML/Datastar attrs (not namespaced) - pass through to HTML
  :data-signals:selected "false"
  :data-on:click "$selected = !$selected"
  :class "highlighted"}]
```

The alias handler receives both, but chassis automatically elides namespaced keys from the rendered HTML.

---

## Effect Reference

All sandbox functionality is available via dispatch effects. Use `(s/describe (dispatch))` to see the full list. Common effects:

| Effect | Purpose |
|--------|---------|
| `[::tsain/preview hiccup]` | Replace preview with new content |
| `[::tsain/preview-append hiccup]` | Append to existing preview |
| `[::tsain/preview-clear]` | Clear the preview area |
| `[::tsain/commit :name opts]` | Save component to library |
| `[::tsain/uncommit :name]` | Remove from library |
| `[::tsain/show-components :name]` | View component with sidebar |
| `[::tsain/show-preview]` | Return to preview view |
| `[::tsain/patch-signals {:key val}]` | Patch Datastar signals on all clients |

---

## Discovery Functions

### Component Discovery (tsain namespace)

| Function | Purpose |
|----------|---------|
| `(tsain/describe)` | List all components with schemas |
| `(tsain/describe :myapp.ui/card)` | Get details for specific component |
| `(tsain/grep "pattern")` | Search by keyword |
| `(tsain/props :variant)` | Find components with specific prop |
| `(tsain/categories)` | List all categories |
| `(tsain/by-category "cards")` | Filter by category |

### Effect Discovery (sandestin namespace)

| Function | Purpose |
|----------|---------|
| `(s/describe (dispatch))` | List all registered effects |
| `(s/describe (dispatch) ::tsain/preview)` | Inspect specific effect |
| `(s/sample (dispatch) ::tsain/preview)` | Generate example invocation |
| `(s/grep (dispatch) "pattern")` | Search effects |
| `(reload)` | Reload changed namespaces |

---

## File Locations (from tsain.edn)

| Config Key | Purpose |
|------------|---------|
| `:ui-namespace` | Namespace for chassis aliases |
| `:stylesheet` | CSS file for component styles |
| `:database-file` | SQLite library storage (preferred) |
| `:components-file` | EDN library storage (legacy) |

---

## Best Practices

1. **Alias-first** - Always define structure in UI namespace before committing
2. **Config by component name** - Use `:component-name/prop` for config props
3. **CSS classes over inline styles** - Extract to stylesheet before committing
4. **Use CSS custom properties** - Leverage theme variables (`--accent-cyan`, `--bg-primary`)
5. **BEM-like naming** - `.component-name`, `.component-name-element`, `.component-name--modifier`
6. **Discovery-first** - Use `describe`, `grep`, `props` to explore components

---

## Dynamic Components with Datastar

For interactive components, Datastar attrs pass through to HTML:

```clojure
;; Alias handles structure
(defmethod c/resolve-alias ::accordion
  [_ attrs content]
  (let [{:accordion/keys [title]} attrs]
    [:div.accordion attrs  ;; data-signals, data-on pass through
     [:button.accordion-header {:data-on:click "$open = !$open"} title]
     [:div.accordion-content {:data-class:open "$open"} content]]))

;; Usage with Datastar attrs
[:myapp.ui/accordion
 {:accordion/title "Click to expand"
  :data-signals:open "false"}
 [:p "Hidden content"]]
```

Test interactivity from REPL:

```bash
clj-nrepl-eval -p <PORT> "(dispatch [[::tsain/patch-signals {:open true}]])"
```

---

## CSS Extraction (Required Before Commit)

All committed components must use CSS classes. The copy button returns the lean alias form from the library, so keeping examples clean ensures portable snippets.

### Extraction Process

1. **Define structure in alias** with semantic class names
2. **Add CSS** for those classes in stylesheet
3. **Use CSS custom properties** for colors that vary by theme
4. **Commit the lean alias invocation** with config props only

### Theme Variants

Use `.theme-light` wrapper class - CSS custom properties handle the rest:

```clojure
;; Dark (default)
[:myapp.ui/card {:card/title "Hello"}]

;; Light
[:div.theme-light
 [:myapp.ui/card {:card/title "Hello"}]]
```

---

## File Size Management

As component libraries grow, large files become unwieldy. Before committing components, check if files are approaching the split threshold (default: 1500 lines, configured via `:split-threshold` in `tsain.edn`).

### Conventions

These paths are conventions, not configurable:

| File Type | Split Location | Import Style |
|-----------|----------------|--------------|
| CSS | `components/` subdirectory | `@import "./components/<category>.css";` |
| Clojure | Sub-namespace from `:ui-namespace` | `(:require [<ui-ns>.<category>])` |

### When to Split

- **Stylesheet exceeds threshold** → Split into `components/<category>.css`
- **UI namespace exceeds threshold** → Split into sub-namespaces by category

### Category Taxonomy

Organize components by semantic category:

| Category | Contains |
|----------|----------|
| `cards` | Card-based layouts, tiles, panels |
| `controls` | Buttons, inputs, selects, toggles |
| `layout` | Grids, containers, spacing utilities |
| `feedback` | Toasts, alerts, loaders, progress bars |
| `navigation` | Menus, tabs, breadcrumbs, pagination |
| `display` | Text treatments, badges, avatars, icons |
| `overlays` | Modals, popovers, tooltips, drawers |

Add new categories as needed. When unsure, prefer fewer larger categories over many small ones.

### CSS Split Procedure

Given stylesheet at `dev/resources/public/styles.css`:

1. Create `components/` directory:
   ```bash
   mkdir -p dev/resources/public/components
   ```

2. Create category file (e.g., `components/cards.css`):
   ```css
   /* Card components */
   .game-card { /* ... */ }
   .game-card-header { /* ... */ }
   ```

3. Move related styles from main stylesheet to category file

4. Add import to main stylesheet (at the top, after any variables):
   ```css
   /* Component imports */
   @import "./components/cards.css";
   @import "./components/controls.css";
   ```

5. Verify hot-reload still works

### Namespace Split Procedure

Given `:ui-namespace sandbox.ui` in `tsain.edn`:

1. Create directory for sub-namespaces:
   ```bash
   mkdir -p dev/src/clj/sandbox/ui
   ```

2. Create category namespace (e.g., `sandbox/ui/cards.clj`):
   ```clojure
   (ns sandbox.ui.cards
     (:require [chassis.core :as c]
               [html.yeah :as hy]))

   (hy/defelem game-card
     [:map {:doc "Game card component"
            :keys [game-card/title]}
      [:game-card/title :string]]
     [:div.game-card attrs
      [:h2.game-card-title game-card/title]])
   ```

3. Move `defelem` definitions from main namespace to category namespace

4. Add require to main UI namespace:
   ```clojure
   (ns sandbox.ui
     (:require [chassis.core :as c]
               [sandbox.ui.cards]    ;; Just require, aliases auto-register
               [sandbox.ui.controls]))
   ```

5. Reload and verify aliases still resolve:
   ```bash
   clj-nrepl-eval -p <PORT> "(reload)"
   clj-nrepl-eval -p <PORT> "(tsain/describe :sandbox.ui/game-card)"
   ```

### When to Suggest Splits

Check file sizes before committing new components. If approaching the threshold:

1. **Identify the category** for the new component
2. **Check if category file exists** - if not, suggest creating it
3. **Recommend adding** the new component directly to the category file
4. **If category file is also large**, consider subcategories (rare)

### CSS Variables

Keep CSS custom properties in the main stylesheet (not split files):

```css
/* styles.css - keep variables here */
:root {
  --accent-cyan: #00f0ff;
  --bg-primary: #0a0a0f;
  /* ... */
}

/* Import component styles after variables */
@import "./components/cards.css";
```

This ensures variables are available to all imported stylesheets.

---

## Component Migration

Migrate legacy `defmethod c/resolve-alias` components to modern `hy/defelem` format.

### Migration Agent

The migration agent at `.claude/agents/migrate-component.md` handles individual component transformations. It:

1. Reads the legacy defmethod definition
2. Infers malli schema from prop usage patterns
3. Generates defelem with schema, doc, and proper destructuring
4. Writes to the appropriate category namespace
5. Updates barrel requires
6. Extracts CSS (if requested)
7. Verifies rendering via REPL

### Single Component Migration

To migrate one component:

1. Read the agent instructions at `.claude/agents/migrate-component.md`
2. Apply the migration steps to your target component
3. Verify via REPL: `(hy/element :namespace/component-name)`

### Orchestrating Bulk Migration

When asked to migrate multiple components from a namespace:

**Step 1: Extract component list**

```clojure
;; Find all defmethod c/resolve-alias patterns
(defmethod c/resolve-alias ::button ...)
(defmethod c/resolve-alias ::card ...)
```

**Step 2: Categorize by dependency tier**

| Tier | Components | Rationale |
|------|------------|-----------|
| 1 | icon, badge, skeleton | No component dependencies |
| 2 | button, input, toggle, avatar | May use tier 1 |
| 3 | toast, alert, card | May use tier 1-2 |
| 4 | modal, popover, table | May use anything |

**Step 3: Migrate by tier**

Run tier 1 migrations in parallel (all at once), then tier 2, etc. This respects dependencies.

**Step 4: Data migration**

After all code transformations complete, run data migration:

```clojure
(dispatch [[::tsain/migrate-from-edn "path/to/components.edn"]])
```

### Migration Flow

```
1. Setup barrel structure (once)
   └─ Create components/ CSS directory
   └─ Create category namespaces

2. For each component (agent, per-component):
   └─ Read legacy defmethod
   └─ Generate defelem with schema
   └─ Write to category namespace
   └─ Extract CSS to category file
   └─ Verify rendering
   └─ Delete old defmethod
   └─ Commit

3. Data migration (once)
   └─ (dispatch [[::tsain/migrate-from-edn "components.edn"]])

4. Cleanup
   └─ Archive/delete legacy components.edn
   └─ Remove empty source namespace
```

### Verification Checklist

After each component:

```clojure
;; 1. Schema registered
(hy/element :namespace/component)
;; → {:tag ... :doc ... :attributes ...}

;; 2. Rendering works
(dispatch [[::tsain/preview [:namespace/component {...}]]])

;; 3. Discovery API works
(tsain/describe :namespace/component)
```
