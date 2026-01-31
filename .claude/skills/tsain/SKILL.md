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
  [:map {:doc "Content card with title and body text.
               Use for displaying discrete content blocks in grids or lists.
               Supports optional variant for visual emphasis."
         :my-card/keys [title body variant]}
   [:my-card/title [:string {:min 1}]]
   [:my-card/body :string]
   [:my-card/variant {:optional true} [:enum :default :elevated :outlined]]]
  (let [variant-class (when my-card/variant
                        (str "my-card--" (name my-card/variant)))]
    [:div.my-card {:class variant-class}
     [:h2.my-card-title my-card/title]
     [:p.my-card-body my-card/body]]))
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

#### Step 4: Use Effect-Based Writes

Instead of manually editing files, use the phandaal-based write effects. They automatically track line counts and return hints when thresholds are exceeded:

```clojure
;; CSS - use ::tsain/write-css instead of editing files directly
(dispatch [[::tsain/write-css ".my-card { ... }" {:category "cards"}]])

;; Components - use ::tsain/write-component instead of editing files directly
(dispatch [[::tsain/write-component "(hy/defelem my-card ...)"]])
```

If the result contains `:hints` with `:type :split-suggested`, respond by dispatching the suggested split effect. See the **Effect Reference** section for details.

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

### CSS Write Effects (Phandaal-Based)

These effects provide tracked CSS writes with threshold detection and actionable hints:

| Effect | Purpose |
|--------|---------|
| `[::tsain/write-css content opts]` | Append CSS to main stylesheet with LOC tracking |
| `[::tsain/write-css-to path content]` | Write CSS to specific file (for barrel imports) |
| `[::tsain/replace-css pattern new-css opts]` | Replace rules matching selector pattern |
| `[::tsain/split-css category opts]` | Extract category styles to barrel file |

**Example: Write CSS with hints**
```clojure
;; Write with category tracking - hints appear when threshold exceeded
(dispatch [[::tsain/write-css ".my-card { background: red; }"
            {:category "cards" :comment "My card styles"}]])

;; Result when threshold exceeded:
;; {:results [{:res {:hints [{:type :split-suggested
;;                            :category "cards"
;;                            :target "components/cards.css"
;;                            :action {:effect ::tsain/split-css
;;                                     :args ["cards"]}}]}}]}
```

**Example: Split by category**
```clojure
;; Extract all card-related styles to components/cards.css
(dispatch [[::tsain/split-css "cards"]])

;; Result:
;; {:category "cards"
;;  :extracted 15
;;  :selectors [".card" ".card-header" ".card-body" ...]
;;  :target-path "/project/dev/resources/public/components/cards.css"
;;  :import-added? true}
```

### Component Write Effects (Phandaal-Based)

These effects provide tracked component code writes with threshold detection:

| Effect | Purpose |
|--------|---------|
| `[::tsain/write-component code opts]` | Append defelem to UI namespace with LOC tracking |
| `[::tsain/write-component-to ns code]` | Write to specific namespace (for barrel imports) |
| `[::tsain/split-namespace category]` | Extract category components to sub-namespace |

**Example: Write component with hints**
```clojure
;; Write component - category inferred from name (game-card -> "cards")
(dispatch [[::tsain/write-component "(hy/defelem game-card ...)"]])

;; Or with explicit category
(dispatch [[::tsain/write-component "(hy/defelem my-widget ...)"
            {:category "controls"}]])

;; Result when threshold exceeded:
;; {:results [{:res {:hints [{:type :split-suggested
;;                            :category "cards"
;;                            :target "sandbox/ui/cards.clj"
;;                            :action {:effect ::tsain/split-namespace
;;                                     :args ["cards"]}}]}}]}
```

**Example: Split by category**
```clojure
;; Extract all card-related components to sub-namespace
(dispatch [[::tsain/split-namespace "cards"]])

;; Result:
;; {:category "cards"
;;  :extracted 3
;;  :components ["game-card" "profile-card" "stats-card"]
;;  :target-namespace "sandbox.ui.cards"
;;  :target-path "/project/src/clj/sandbox/ui/cards.clj"}
```

**Category inference from component names:**

| Component Name | Inferred Category |
|----------------|-------------------|
| `*-card`, `*-tile`, `*-panel` | cards |
| `*-btn`, `*-button`, `*-input` | controls |
| `*-toast`, `*-alert`, `*-loader` | feedback |
| `*-nav`, `*-menu`, `*-tab` | navigation |
| `*-badge`, `*-avatar`, `*-indicator` | display |
| `*-modal`, `*-popover`, `*-tooltip` | overlays |

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

## Component Schema Quality

Write robust schemas and documentation for every component. These power the discovery API and enable runtime validation.

### Documentation Standards

Write `:doc` strings that explain:

- **What** the component renders (structure, visual appearance)
- **When** to use it vs alternatives
- **Behavior** notes (interactions, edge cases, responsive behavior)

```clojure
;; GOOD: Comprehensive documentation
[:map {:doc "Dismissible alert banner for user feedback.
             Renders a colored banner with icon, title, and optional message.
             Use for transient feedback after user actions (form submit, delete, etc.).
             For persistent system status, use StatusBanner instead.
             Supports Datastar signals for dismiss animation."
       :alert/keys [variant title message dismissible]}
 ...]

;; BAD: Minimal documentation
[:map {:doc "An alert"
       :alert/keys [variant title]}
 ...]
```

### Prop Schema Standards

Use specific malli types - never use `:string` when a more precise type applies:

| Use Case | Schema | Not |
|----------|--------|-----|
| Variants/modes | `[:enum :small :medium :large]` | `:string` |
| Counts/indices | `[:int {:min 0}]` | `:int` or `:string` |
| Boolean flags | `:boolean` | `:string` |
| Optional props | `{:optional true}` | Omitting from schema |
| Constrained strings | `[:string {:min 1 :max 100}]` | `:string` |
| URLs | `[:string {:re #"^https?://.*"}]` | `:string` |

### Complete Example

```clojure
(hy/defelem alert
  [:map {:doc "Dismissible alert banner for user feedback.
               Renders a colored banner with optional icon, title, and message body.
               Use for transient feedback after user actions.
               Pass :dismissible true to show close button with Datastar dismiss behavior."
         :alert/keys [variant title message icon dismissible]}
   [:alert/variant [:enum :info :success :warning :error]]
   [:alert/title :string]
   [:alert/message {:optional true} :string]
   [:alert/icon {:optional true} :string]
   [:alert/dismissible {:optional true} :boolean]]
  (let [variant-class (str "alert--" (name alert/variant))]
    [:div.alert {:class variant-class
                 :role "alert"}
     (when alert/icon
       [:span.alert-icon alert/icon])
     [:div.alert-content
      [:strong.alert-title alert/title]
      (when alert/message
        [:p.alert-message alert/message])]
     (when alert/dismissible
       [:button.alert-dismiss {:data-on:click "$dismissed = true"
                               :aria-label "Dismiss"}
        "×"])]))
```

### Schema Checklist

Before committing a component, verify:

- [ ] `:doc` explains what, when, and behavior
- [ ] All props use appropriate malli types (not just `:string`)
- [ ] Optional props marked with `{:optional true}`
- [ ] Enum variants cover all valid values
- [ ] Numeric props have sensible constraints (`:min`, `:max`)

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

As component libraries grow, large files become unwieldy. Tsain provides effect-based CSS writes that automatically track line counts and suggest splits when the threshold is exceeded.

### Automatic Threshold Detection

When using `::tsain/write-css`, phandaal tracks line counts and returns hints when the configured `:split-threshold` (default 1500 lines) is exceeded:

```clojure
;; Write with category tracking
(dispatch [[::tsain/write-css ".btn { padding: 1rem; }"
            {:category "controls"}]])

;; If threshold exceeded, result includes actionable hint:
;; {:hints [{:type :split-suggested
;;           :category "controls"
;;           :target "components/controls.css"
;;           :message "Stylesheet exceeds 1500 lines. Extract controls styles to components/controls.css"
;;           :action {:effect ::tsain/split-css :args ["controls"]}}]}
```

### Responding to Split Hints

When you see a `:split-suggested` hint, dispatch the suggested action:

```clojure
(dispatch [[::tsain/split-css "controls"]])
```

This will:
1. Parse the stylesheet with jStyleParser
2. Find all rules matching `.btn*`, `.button*`, `.input*`, etc.
3. Extract them to `components/controls.css`
4. Add `@import "./components/controls.css";` to the main stylesheet
5. Return a summary of extracted selectors

### Conventions

These paths are conventions, not configurable:

| File Type | Split Location | Import Style |
|-----------|----------------|--------------|
| CSS | `components/` subdirectory | `@import "./components/<category>.css";` |
| Clojure | Sub-namespace from `:ui-namespace` | `(:require [<ui-ns>.<category>])` |

### Category Taxonomy

Categories map to selector patterns automatically:

| Category | Matches Patterns |
|----------|-----------------|
| `cards` | `.card*`, `.cards*` |
| `controls` | `.btn*`, `.button*`, `.input*`, `.select*`, `.toggle*` |
| `layout` | `.container*`, `.grid*`, `.flex*`, `.row*`, `.col*` |
| `feedback` | `.toast*`, `.alert*`, `.loader*`, `.progress*` |
| `navigation` | `.nav*`, `.menu*`, `.tab*`, `.breadcrumb*` |
| `display` | `.badge*`, `.avatar*`, `.text*`, `.heading*` |
| `overlays` | `.modal*`, `.popover*`, `.tooltip*`, `.dropdown*` |

For custom patterns, pass them explicitly:

```clojure
(dispatch [[::tsain/split-css "game"
            {:patterns [".game-card" ".player-hud" ".game-board"]}]])
```

### Manual CSS Split (If Needed)

For cases where the automatic split doesn't work, you can still split manually:

1. Create `components/` directory:
   ```bash
   mkdir -p dev/resources/public/components
   ```

2. Create category file with extracted styles

3. Add import to main stylesheet:
   ```css
   @import "./components/cards.css";
   ```

4. Verify hot-reload still works

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
     [:map {:doc "Card displaying game information with title, stats, and optional image.
                  Use in game library grids or search results.
                  Supports selection state via Datastar signals."
            :game-card/keys [title subtitle image-url stats]}
      [:game-card/title [:string {:min 1}]]
      [:game-card/subtitle {:optional true} :string]
      [:game-card/image-url {:optional true} :string]
      [:game-card/stats {:optional true} [:map-of :keyword [:or :string :int]]]]
     [:div.game-card attrs
      (when game-card/image-url
        [:img.game-card-image {:src game-card/image-url :alt game-card/title}])
      [:h2.game-card-title game-card/title]
      (when game-card/subtitle
        [:p.game-card-subtitle game-card/subtitle])])
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
