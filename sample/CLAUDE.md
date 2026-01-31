# My Project

## Project Overview

<!-- Fill in your project description here -->

This is a Clojure Datastar application powered by the sandestin effect dispatch ecosystem, with tsain for REPL-driven component development.

## Claude Code Skills

This project uses the tsain skill for component development workflows:

- `/tsain iterate` - Direct component iteration workflow
- `/tsain implement` - Spec-driven implementation workflow

To install the skill, copy from the tsain repository:

```bash
# From your project root
cp -r path/to/tsain/.claude/skills/tsain .claude/skills/
```

## Development Setup

### Starting the REPL

```bash
clj -M:dev
```

### Development Workflow

1. Start REPL with `clj -M:dev`
2. Load dev namespace: `(dev)`
3. Start the system: `(start)`
4. Open browser: `localhost:3000/sandbox`
5. Make changes to source files
6. Reload: `(reload)`

The `dev` namespace provides:
- `(start)` - Start the sandbox server
- `(stop)` - Stop the server
- `(reload)` - Reload changed namespaces via clj-reload
- `(restart)` - Stop, reload, and start
- `(dispatch effects)` - Dispatch sandestin effects
- `(dispatch)` - Get the raw dispatch function (for discovery)
- `(describe (dispatch))` - List all registered effects/actions
- `(sample (dispatch) key)` - Generate sample invocations
- `(grep (dispatch) pattern)` - Search registry

## Project Structure

```
tsain.edn                           # Tsain configuration
myapp/ui.clj                        # html.yeah components (defelem)
tsain.db                            # Component library (SQLite)
dev/resources/public/styles.css     # Component CSS (hot-reloadable)
```

---

# Tsain Component Workflow

Tsain provides REPL-driven component development with live preview across all connected browsers.

## Discovering the API

```clojure
(require '[ascolais.tsain :as tsain])

;; List all tsain effects
(describe (dispatch))

;; Inspect specific effect
(describe (dispatch) ::tsain/preview)

;; Generate sample invocation
(sample (dispatch) ::tsain/preview)

;; Search effects
(grep (dispatch) "component")
```

## Effect-Based Component Workflow

### Step 1: Preview with Inline Styles

Start with hiccup directly in preview for rapid iteration:

```clojure
(dispatch [[::tsain/preview
            [:div {:style {:background "#1a1a2e" :padding "1rem"}}
             [:h2 "My Card"]
             [:p "Card content"]]]])
```

### Step 2: Define Component via Effect

Write the component through the effect system (auto-formats with cljfmt):

```clojure
(dispatch [[::tsain/write-component
            "(hy/defelem my-card
               [:map {:doc \"A simple card component\"
                      :keys [my-card/title my-card/body]}
                [:my-card/title :string]
                [:my-card/body :string]]
               [:div.my-card
                [:h2.my-card-title my-card/title]
                [:p.my-card-body my-card/body]
                (hy/children)])"]])
```

Reload and preview:

```clojure
(reload)
(dispatch [[::tsain/preview
            [:myapp.ui/my-card
             {:my-card/title "Hello"
              :my-card/body "World"}]]])
```

### Step 3: Add CSS via Effect

Write CSS through the effect system (auto-formats with prettier):

```clojure
(dispatch [[::tsain/write-css
            ".my-card { background: var(--bg-secondary); padding: 1rem; }
             .my-card-title { color: var(--accent-cyan); }
             .my-card-body { color: var(--text-primary); }"
            {:category "cards"}]])
```

### Step 4: Commit to Library

```clojure
(dispatch [[::tsain/commit :my-card
            {:description "Card component"
             :examples [{:label "Default"
                         :hiccup [:myapp.ui/my-card
                                  {:my-card/title "Hello"
                                   :my-card/body "World"}]}]}]])
```

### Responding to Split Hints

When files exceed `:split-threshold`, effects return hints:

```clojure
;; Result includes hint
{:hints [{:type :split-suggested
          :category "cards"
          :action [::tsain/split-namespace "cards"]}]}

;; Act on the hint
(dispatch [[::tsain/split-namespace "cards"]])
;; Creates myapp.ui.cards, adds require to myapp.ui
```

## Tsain Effects Reference

### Preview & Library

| Effect | Purpose |
|--------|---------|
| `[::tsain/preview hiccup]` | Replace preview with content |
| `[::tsain/preview-append hiccup]` | Append to preview |
| `[::tsain/preview-clear]` | Clear preview |
| `[::tsain/commit :name opts]` | Save to library |
| `[::tsain/uncommit :name]` | Remove from library |
| `[::tsain/show-components :name]` | View component |
| `[::tsain/show-preview]` | Return to preview |
| `[::tsain/patch-signals {:key val}]` | Test Datastar signals |

### Component Authoring

| Effect | Purpose |
|--------|---------|
| `[::tsain/write-component code]` | Write defelem to UI namespace |
| `[::tsain/write-component code {:category "cards"}]` | Write with explicit category |
| `[::tsain/write-component-to ns-sym code]` | Write to specific namespace |
| `[::tsain/split-namespace "category"]` | Extract category to sub-namespace |

### CSS Authoring

| Effect | Purpose |
|--------|---------|
| `[::tsain/write-css css {:category "cards"}]` | Append CSS with category |
| `[::tsain/replace-css ".selector" new-css]` | Replace existing CSS rules |
| `[::tsain/split-css "category"]` | Extract category to sub-file |

---

# Sandestin Effect System

## Registry Authoring

A registry is a map with namespaced keys under `ascolais.sandestin`:

```clojure
(require '[ascolais.sandestin :as s])

{::s/effects      {qualified-keyword -> EffectRegistration}
 ::s/actions      {qualified-keyword -> ActionRegistration}
 ::s/placeholders {qualified-keyword -> PlaceholderRegistration}
 ::s/interceptors [Interceptor ...]
 ::s/system-schema {keyword -> MalliSchema}
 ::s/system->state (fn [system] state)}
```

## Effect Structure

Effects are side-effecting operations:

```clojure
{::s/effects
 {:app/save-user
  {::s/description "Save user to database"
   ::s/schema [:tuple [:= :app/save-user] :map]
   ::s/handler (fn [{:keys [dispatch]} system user]
                 (db/save! (:db system) user))}}}
```

## Action Structure

Actions are pure functions returning effect vectors:

```clojure
{::s/actions
 {:app/update-profile
  {::s/description "Update user profile"
   ::s/schema [:tuple [:= :app/update-profile] :string :map]
   ::s/handler (fn [state user-id changes]
                 [[:app/save-user (merge (:user state) changes)]
                  [::twk/patch-elements [:div#status "Saved!"]]])}}}
```

## Creating a Dispatch

```clojure
(def dispatch
  (s/create-dispatch
    [(twk/registry)
     (sfere/registry store)
     (tsain/registry)
     app-registry]))
```

## Discovery API

```clojure
(s/describe dispatch)                    ;; List all items
(s/describe dispatch :effects)           ;; List by type
(s/describe dispatch ::tsain/preview)    ;; Inspect specific item
(s/sample dispatch ::tsain/preview)      ;; Generate sample
(s/grep dispatch "broadcast")            ;; Search
```

---

# Datastar Frontend Framework

## Core Concepts

1. **Backend drives state** - Server pushes state via SSE
2. **Signals** - Reactive variables (`$signalName`)
3. **Attributes** - `data-*` for declarative behavior
4. **Actions** - `@get()`, `@post()` for backend calls

## Signals

```html
<!-- Two-way binding -->
<input data-bind:username />

<!-- Direct initialization -->
<div data-signals:count="0"></div>

<!-- Computed -->
<div data-computed:doubled="$count * 2"></div>
```

## Attribute Plugins

| Attribute | Purpose | Example |
|-----------|---------|---------|
| `data-text` | Set text | `<span data-text="$count"></span>` |
| `data-show` | Toggle visibility | `<div data-show="$visible"></div>` |
| `data-class` | Toggle classes | `<div data-class:active="$active"></div>` |
| `data-bind` | Two-way binding | `<input data-bind:email />` |
| `data-on` | Event handlers | `<button data-on:click="$count++"></button>` |

## Event Handling

```html
<button data-on:click="@post('/api/save')">Save</button>
<input data-on:input__debounce.300ms="@get('/search')" />
```

**Event object:** Use `evt.target.value` (not `this.value`):
```html
<select data-on:change="@post('/api?v=' + evt.target.value)">
```

---

# TWK (Datastar) Patterns

## Hiccup

TWK accepts hiccup directly - no need to pre-render:

```clojure
{:body [:h1 "Hello"]}
```

## Response Shapes

```clojure
;; HTML response
{:body [:h1 "Hello"]}

;; Datastar SSE
{::twk/fx [[::twk/patch-elements [:div "content"]]]}

;; Auto-close SSE
{::twk/fx [...] ::twk/with-open-sse? true}
```

## Effects

| Effect | Purpose |
|--------|---------|
| `::twk/patch-elements` | Update DOM with hiccup |
| `::twk/patch-signals` | Update client signals |
| `::twk/execute-script` | Run JavaScript |
| `::twk/close-sse` | Close SSE connection |

---

# Sfere (Connection Management)

## Creating a Store

```clojure
(def store (sfere/store {:type :caffeine
                          :duration-ms 30000
                          :expiry-mode :sliding}))
```

## Broadcast

```clojure
[[:ascolais.sfere/broadcast {:pattern [:* [:room "lobby" :*]]}
  [:ascolais.twk/patch-elements [:div "message"]]]]
```

## Pattern Matching

`:*` is wildcard:
```clojure
[:* [:room "lobby" :*]]  ;; All users in lobby
[:* :*]                   ;; All connections
```

---

# Component Styling Conventions

## BEM-like Naming

```css
.component-name { }
.component-name-element { }
.component-name--modifier { }
```

## Theme Support

Use CSS custom properties:

```css
.my-component {
  background: var(--bg-primary);
  color: var(--accent-cyan);
}
```

Light theme: wrap with `.theme-light`:
```clojure
[:div.theme-light [:my-component]]
```

## Component Conventions

Components are defined with html.yeah `defelem` (compiles to Chassis aliases):

- **Namespaced attrs** (`:card/title`) = config props, elided from HTML
- **Regular attrs** (`:class`, `:data-on:click`) = pass through

```clojure
;; Definition
(hy/defelem card
  [:map {:doc "A card component"
         :keys [card/title]}
   [:card/title :string]]
  [:div.card card/title (hy/children)])

;; Usage
[:myapp.ui/card
 {:card/title "Hello"      ;; Config - elided
  :class "highlighted"      ;; HTML - preserved
  :data-on:click "..."}]    ;; Datastar - preserved
```

Components defined with `defelem` are discoverable via `tsain/describe` and `tsain/grep`.
