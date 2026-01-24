---
name: hiccup-component
description: Develop hiccup components with live preview, CSS styling, and library commits. Supports spec-driven implementation workflow. Keywords: component, preview, iterate, css, hiccup, design, ui, commit, datastar, signals, interactive, alias, implement, spec.
---

# Hiccup Component Development

Drive component development through an **alias-first** REPL-powered iteration loop. Component structure lives in a UI namespace as chassis aliases, while `components.edn` stores lean alias invocations with config props.

## Commands

This skill supports commands via arguments:

- `/hiccup-component` - Show available commands
- `/hiccup-component auto` - Autonomous component development loop
- `/hiccup-component implement` - Spec-driven implementation workflow
- `/hiccup-component iterate` - Direct component iteration

If no argument is provided, show available commands.

---

## `/hiccup-component auto`

Autonomous component development loop. Claude drives the entire workflow - deciding priorities, validating work visually, and continuing until satisfied.

### Required Tools

- **Chrome extension** - For visual validation of rendered components
- **frontend-design MCP** - For design decisions and validation

### Workflow Loop

1. **Determine next task** - Claude reads specs and decides what to work on (not user-directed)
2. **Implement component** using `/hiccup-component iterate` workflow
3. **Visual validation** - Use chrome extension to screenshot and validate the rendered component
4. **Test interactions** - For interactive components, test Datastar behaviors via `::tsain/patch-signals`
5. **Iterate until satisfied** - Claude decides when quality is acceptable
6. **Checkpoint** - Commit to tsain library, update specs, git commit
7. **Continue** - Return to step 1 for next component

### Autonomous Principles

- **Claude decides priority** - Read specs, assess dependencies, choose what to build next
- **Claude decides completion** - Keep iterating until visually and functionally satisfied
- **Self-validation** - Use chrome screenshots to verify rendering matches intent
- **Checkpoint frequently** - Update specs after each component so progress survives compaction
- **No user prompting** - Only stop for genuine blockers (missing requirements, ambiguous specs)

### Compaction Survival

Progress persists through context compaction via specs:
- Each completed component is marked in `specs/<feature>/implementation-plan.md`
- Running `/specs implement` after compaction resumes from last checkpoint
- Tsain library (`components.edn`) preserves committed components

### Loop Pseudocode

```
while specs have incomplete components:
  1. /specs implement → identify next component
  2. Read spec requirements for that component
  3. /hiccup-component iterate → build it
  4. Screenshot via chrome extension → validate visually
  5. If interactive: test with patch-signals
  6. If not satisfied: iterate (go to 3)
  7. Commit to tsain library
  8. Update CLAUDE.md with component reference
  9. Update spec: mark task complete with commit sha
  10. git commit
  11. Regenerate spec index
```

### Starting the Loop

```bash
# Ensure prerequisites
clj-nrepl-eval --discover-ports  # Find REPL
open http://localhost:3000/sandbox  # Browser ready

# Then run
/hiccup-component auto
```

Claude will take over from there, working through specs autonomously.

---

## `/hiccup-component implement`

End-to-end workflow for implementing components from specs to production.

### Steps

1. **Run `/specs implement`** to identify the next spec to work on
2. **Use `/hiccup-component iterate`** to develop the component
3. **Use `/clojure-eval`** for REPL interaction
4. **Update CLAUDE.md** with component reference when done
5. **Commit** means: commit to component library with tsain AND git

Do not assume a REPL connection needs to be restarted. Always run `clj-nrepl-eval --discover-ports` before your first REPL expression. The REPL is likely still running. If it is not, stop and ask the user what they want you to do.

---

## `/hiccup-component iterate`

Direct component iteration workflow for developing a single component.

### Configuration

Read `tsain.edn` at project root for file locations:

```clojure
;; tsain.edn
{:ui-namespace brianium.views.components  ;; Where chassis aliases live
 :components-file "dev/resources/components.edn"  ;; Component library persistence
 :stylesheet "resources/public/styles.css"  ;; CSS for hot reload
 :port 3000}
```

### Prerequisites

1. **Discover nREPL port first** - run `clj-nrepl-eval --discover-ports` before your first REPL expression
2. **Sandbox started** - if not running, evaluate `(dev)` then `(start)` in the REPL
3. **Browser open** at `http://localhost:3000/sandbox`

### Discovering the API

Use sandestin discovery to explore available effects:

```clojure
(require '[ascolais.tsain :as tsain])

;; List all tsain effects
(describe (dispatch))

;; See schema and docs for an effect
(describe (dispatch) ::tsain/preview)

;; Generate example invocation
(sample (dispatch) ::tsain/preview)

;; Search by keyword
(grep (dispatch) "component")
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
- **Namespaced attrs** (`:game-card/title`) = config props (elided from HTML output)
- **Regular attrs** (`:data-on:click`, `:class`) = pass through to HTML
- **Namespace by component name** for self-documenting code

```clojure
;; In views/components.clj
(defmethod c/resolve-alias ::my-component
  [_ attrs _]
  (let [{:my-component/keys [title subtitle icon]} attrs]
    [:div.my-component attrs  ;; namespaced keys auto-elided by chassis
     [:div.my-component-header
      [:span.my-component-icon icon]
      [:h3.my-component-title title]]
     [:p.my-component-subtitle subtitle]]))
```

After adding the alias, reload the namespace:

```bash
clj-nrepl-eval -p <PORT> "(reload)"
```

#### Step 2: Preview with Alias Invocation

Use the alias with config props. The namespace is from `:ui-namespace`:

```bash
clj-nrepl-eval -p <PORT> "(dispatch [[::tsain/preview
  [:brianium.views.components/my-component
   {:my-component/title \"Hello World\"
    :my-component/subtitle \"A simple example\"
    :my-component/icon \"🎉\"}]]])"
```

#### Step 3: Iterate on Structure and CSS

1. **Modify the alias** in the UI namespace to adjust structure
2. **Reload**: `clj-nrepl-eval -p <PORT> "(reload)"`
3. **Re-preview** to see changes
4. **Add CSS** to the stylesheet (from `:stylesheet`) - hot-reloads automatically

#### Step 4: Commit the Lean Example

```bash
clj-nrepl-eval -p <PORT> "(dispatch [[::tsain/commit :my-component
  {:description \"Card with icon and title\"
   :examples
   [{:label \"Dark\"
     :hiccup [:div {:style \"padding: 40px;\"}
              [:brianium.views.components/my-component
               {:my-component/title \"Hello World\"
                :my-component/subtitle \"A simple example\"
                :my-component/icon \"🎉\"}]]}
    {:label \"Light\"
     :hiccup [:div.theme-light {:style \"padding: 40px; background: #f0f4f8;\"}
              [:brianium.views.components/my-component
               {:my-component/title \"Hello World\"
                :my-component/subtitle \"A simple example\"
                :my-component/icon \"🎉\"}]]}]}]])"
```

**Result:** `components.edn` stores the lean alias form. Copying from the sandbox UI gives you clean, portable hiccup.

---

## Config Props vs HTML Attrs

```clojure
[:brianium.views.components/game-card
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

All sandbox functionality is available via dispatch effects. Use `(describe (dispatch))` to see the full list. Common effects:

| Effect | Purpose |
|--------|---------|
| `[::tsain/preview hiccup]` | Replace preview with new content |
| `[::tsain/preview-append hiccup]` | Append to existing preview |
| `[::tsain/preview-clear]` | Clear the preview area |
| `[::tsain/commit :name]` | Save preview to library |
| `[::tsain/commit :name "desc"]` | Save with description |
| `[::tsain/commit :name {:description "..." :examples [...]}]` | Save with multiple examples |
| `[::tsain/uncommit :name]` | Remove from library |
| `[::tsain/show-components :name]` | View component with sidebar |
| `[::tsain/show-preview]` | Return to preview view |
| `[::tsain/patch-signals {:key val}]` | Patch Datastar signals on all clients |

---

## Discovery Functions

Available in the dev namespace:

| Function | Purpose |
|----------|---------|
| `(describe (dispatch))` | List all registered effects |
| `(describe (dispatch) ::tsain/preview)` | Inspect specific effect |
| `(sample (dispatch) ::tsain/preview)` | Generate example invocation |
| `(grep (dispatch) "pattern")` | Search by pattern |
| `(reload)` | Reload changed namespaces (includes alias changes) |

---

## File Locations (from tsain.edn)

| Config Key | Purpose |
|------------|---------|
| `:ui-namespace` | Namespace for chassis aliases |
| `:stylesheet` | CSS file for component styles |
| `:components-file` | Library persistence file |

---

## File Organization

### Recognizing When to Split

Before adding a new component, check if splitting would help:

```bash
wc -l src/clj/*/views/components.clj
```

If over ~1500 lines, consider splitting first.

### Splitting Workflow

1. **Create components directory:**
   ```bash
   mkdir -p src/clj/brianium/views/components
   ```

2. **Move related aliases to domain file:**
   - Cut alias definitions from `components.clj`
   - Create new file (e.g., `components/overlays.clj`)
   - Add namespace declaration requiring chassis
   - Paste aliases (keep `:brianium.views.components/` prefix on keywords)

3. **Update barrel file:**
   ```clojure
   (ns brianium.views.components
     (:require [brianium.views.components.overlays]
               ...))
   ```

4. **Reload and verify:**
   ```bash
   clj-nrepl-eval -p <PORT> "(reload)"
   ```

5. **Test in sandbox** - existing component library entries should still work

### Domain Groupings (Suggested)

| File | Components |
|------|------------|
| `buttons.clj` | button, icon-button, button-group |
| `forms.clj` | input, textarea, select, checkbox, radio, toggle, slider, tag-input |
| `feedback.clj` | toast, alert, progress, skeleton, empty-state |
| `overlays.clj` | modal, drawer, popover, tooltip, dropdown-menu, command-palette |
| `navigation.clj` | tabs, breadcrumb, pagination, stepper |
| `data_display.clj` | table, card, badge, avatar, timeline, stat-card |

---

## Best Practices

1. **Alias-first** - Always define structure in UI namespace before committing
2. **Config by component name** - Use `:component-name/prop` for config props
3. **CSS classes over inline styles** - Extract to stylesheet before committing
4. **Use CSS custom properties** - Leverage theme variables (`--accent-cyan`, `--bg-primary`)
5. **BEM-like naming** - `.component-name`, `.component-name-element`, `.component-name--modifier`
6. **Discovery-first** - Use `describe`, `sample`, `grep` to explore the API

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
[:brianium.views.components/accordion
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

All committed components must use CSS classes. The copy button returns the lean alias form from `components.edn`, so keeping examples clean ensures portable snippets.

### Extraction Process

1. **Define structure in alias** with semantic class names
2. **Add CSS** for those classes in stylesheet
3. **Use CSS custom properties** for colors that vary by theme
4. **Commit the lean alias invocation** with config props only

### Theme Variants

Use `.theme-light` wrapper class - CSS custom properties handle the rest:

```clojure
;; Dark (default)
[:brianium.views.components/game-card {:game-card/title "Card" ...}]

;; Light
[:div.theme-light
 [:brianium.views.components/game-card {:game-card/title "Card" ...}]]
```
