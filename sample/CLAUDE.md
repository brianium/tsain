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
myapp/ui.clj                        # Chassis aliases (component structure)
resources/components.edn            # Component library (lean alias invocations)
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

## Alias-First Workflow

### Step 1: Define Chassis Alias

Add to your UI namespace (see `:ui-namespace` in tsain.edn):

```clojure
(defmethod c/resolve-alias ::my-card [_ attrs _]
  (let [{:my-card/keys [title body]} attrs]
    [:div.my-card attrs
     [:h2.my-card-title title]
     [:p.my-card-body body]]))
```

### Step 2: Preview

```clojure
(dispatch [[::tsain/preview
            [:myapp.ui/my-card
             {:my-card/title "Hello"
              :my-card/body "World"}]]])
```

### Step 3: Add CSS

Add classes to `styles.css` (hot-reloads automatically):

```css
.my-card { ... }
.my-card-title { ... }
.my-card-body { ... }
```

### Step 4: Commit

```clojure
(dispatch [[::tsain/commit :my-card
            {:description "Card component"
             :examples [{:label "Default"
                         :hiccup [:myapp.ui/my-card
                                  {:my-card/title "Hello"
                                   :my-card/body "World"}]}]}]])
```

## Tsain Effects Reference

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

## Chassis Alias Conventions

- **Namespaced attrs** (`:card/title`) = config props, elided from HTML
- **Regular attrs** (`:class`, `:data-on:click`) = pass through

```clojure
[:myapp.ui/card
 {:card/title "Hello"      ;; Config - elided
  :class "highlighted"      ;; HTML - preserved
  :data-on:click "..."}]    ;; Datastar - preserved
```
