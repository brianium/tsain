# ascolais/tsain

## Project Overview

This is a Clojure Datastar application powered by the sandestin effect dispatch ecosystem.

### Key Libraries

| Library | Purpose | Provides |
|---------|---------|----------|
| **sandestin** | Effect dispatch with schema-driven discoverability | `s/create-dispatch`, discovery API |
| **twk** | Datastar SSE integration | `twk/registry`, `twk/with-datastar` middleware |
| **sfere** | Connection management and broadcasting | `sfere/registry`, `sfere/store` |
| **kaiin** | Declarative HTTP routing from registry metadata | `kaiin/routes` |

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         HTTP Request                            │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    twk/with-datastar middleware                 │
│  - Parses Datastar signals from headers                         │
│  - Dispatches ::twk/fx effects via sandestin                    │
│  - Returns SSE responses                                        │
└──────────────────────────────┬──────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      sandestin dispatch                         │
│  - Interpolates placeholders                                    │
│  - Expands actions → effect vectors                             │
│  - Executes effects with interceptors                           │
└──────────────────────────────┬──────────────────────────────────┘
                               │
            ┌──────────────────┼──────────────────┐
            ▼                  ▼                  ▼
    ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
    │  twk effects │   │ sfere effects│   │  app effects │
    │ patch-elements   │ broadcast    │   │  custom      │
    │ patch-signals    │ with-connection  │              │
    └──────────────┘   └──────────────┘   └──────────────┘
```

## Technology Stack

- **Clojure** with deps.edn
- **clj-reload** for namespace reloading during development
- **Portal** for data inspection (tap> integration)
- **Cognitect test-runner** for running tests

## Development Setup

### Starting the REPL

```bash
clj -M:dev
```

This starts a REPL with development dependencies loaded.

### Development Workflow

1. Start REPL with `clj -M:dev`
2. Load dev namespace: `(dev)`
3. Start the system: `(start)`
4. Make changes to source files
5. Reload: `(reload)`

The `dev` namespace provides:
- `(start)` - Start the sandbox server at localhost:3000
- `(stop)` - Stop the server
- `(reload)` - Reload changed namespaces via clj-reload
- `(restart)` - Stop, reload, and start
- `(preview! hiccup)` - Replace preview area with hiccup content
- `(preview-append! hiccup)` - Append hiccup to preview area
- `(preview-clear!)` - Clear the preview area

### Portal

Portal opens automatically when the dev namespace loads. Any `(tap> data)` calls will appear in the Portal UI.

### Component Preview (REPL-Driven UI Development)

The sandbox provides a browser-based preview area for rapidly iterating on hiccup components. Open `localhost:3000` in a browser, then use these functions from the REPL:

```clojure
;; Replace preview with new content
(preview! [:h1 "Hello World"])

;; Build up content by appending
(preview-clear!)
(preview-append! [:div.card [:h3 "Card 1"] [:p "First card"]])
(preview-append! [:div.card [:h3 "Card 2"] [:p "Second card"]])

;; Complex component with inline styles
(preview!
  [:div {:style "background: white; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1);"}
   [:h2 {:style "margin: 0 0 1rem 0;"} "User Profile"]
   [:p {:style "color: #666;"} "Component description here"]])
```

**Key points:**
- All preview functions broadcast to ALL connected browsers/devices simultaneously
- Use `preview!` to reset and show a single component
- Use `preview-append!` to build up multiple components for comparison
- Use `preview-clear!` to reset to empty state
- Inline styles work well for prototyping; extract to CSS classes later

## Project Structure

```
src/clj/          # Clojure source files
dev/src/clj/      # Development-only source (user.clj, dev.clj)
test/src/clj/     # Test files
resources/        # Resource files
specs/            # Living specification documents
```

## REPL Evaluation

Use the clojure-eval skill to evaluate code via nREPL.

### Starting an nREPL Server

To start a REPL with nREPL support (required for clojure-eval):

```bash
clj -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.0"}}}' -M:dev -m nrepl.cmdline --port 7888
```

This starts an nREPL server on port 7888 with all dev dependencies loaded.

### Connecting and Evaluating

```bash
clj-nrepl-eval --discover-ports          # Find running REPLs
clj-nrepl-eval -p 7888 "(+ 1 2 3)"       # Evaluate expression
```

**Important:** All REPL evaluation should take place in the `dev` namespace. After connecting, switch to the dev namespace:

```bash
clj-nrepl-eval -p 7888 "(dev)"
```

To reload code after making changes, use clj-reload:

```bash
clj-nrepl-eval -p 7888 "(reload)"
```

## Running Tests

```bash
clj -X:test
```

Or from the REPL (in the dev namespace):

```clojure
(reload)  ; Reload changed namespaces first
(require '[clojure.test :refer [run-tests]])
(run-tests 'ascolais.tsain-test)
```

---

## Sandestin Effect System

### Registry Authoring

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

### Effect Structure

Effects are side-effecting operations:

```clojure
{::s/effects
 {:app/save-user
  {::s/description "Save user to database"
   ::s/schema [:tuple [:= :app/save-user] :map]
   ::s/system-keys [:db]
   ::s/handler (fn [{:keys [dispatch dispatch-data]} system user]
                 (db/save! (:db system) user)
                 {:saved true})}}}
```

**Key points:**
- `::s/schema` describes invocation shape: `[:tuple [:= :qualified/key] <arg-schemas...>]`
- Handler receives context map with `dispatch` for continuations
- `::s/system-keys` documents which system keys the effect needs

### Action Structure

Actions are pure functions that return effect vectors:

```clojure
{::s/actions
 {:app/update-profile
  {::s/description "Update user profile and notify"
   ::s/schema [:tuple [:= :app/update-profile] :string :map]
   ::s/handler (fn [state user-id changes]
                 ;; Pure function - returns effects to dispatch
                 [[:app/save-user (merge (:user state) changes)]
                  [::twk/patch-elements [:div#status "Saved!"]]])}}}
```

**Key points:**
- Actions are pure functions (no side effects in the handler)
- Return vectors of effects to dispatch
- Receive `state` from `::s/system->state` extraction

### Placeholder Structure

Placeholders resolve values from dispatch-data at dispatch time:

```clojure
{::s/placeholders
 {:app/current-user
  {::s/description "Get current user from dispatch context"
   ::s/schema :map
   ::s/handler (fn [dispatch-data]
                 (:current-user dispatch-data))}}}

;; Usage in effects:
[[:app/greet [:app/current-user]]]
;; Placeholder is replaced with actual user before dispatch
```

### Creating a Dispatch

Compose registries into a dispatch function:

```clojure
(require '[ascolais.sandestin :as s]
         '[ascolais.twk :as twk]
         '[ascolais.sfere :as sfere])

;; Create connection store
(def store (sfere/store {:type :caffeine :duration-ms 30000}))

;; Create dispatch from composed registries
(def dispatch
  (s/create-dispatch
    [(twk/registry)                    ;; Datastar effects
     (sfere/registry store)            ;; Connection effects
     app-registry]))                   ;; Application effects/actions
```

**Registry composition:**
- Registries merge left-to-right
- Later registries override earlier ones (with warning via tap>)
- Interceptors concatenate in order

### Invoking a Dispatch

```clojure
;; 1-arity: effects only (empty system and dispatch-data)
(dispatch [[:app/log "hello"]])

;; 2-arity: system + effects
(dispatch {:db connection}
          [[:app/save-user {:name "Alice"}]])

;; 3-arity: system + dispatch-data + effects
(dispatch {:db connection}
          {:current-user {:id 1}}
          [[:app/greet [:app/current-user]]])
```

---

## REPL Discovery API

**Critical:** Use these functions to explore available effects, actions, and their schemas.

### describe - List and inspect registered items

```clojure
;; List all items
(s/describe dispatch)

;; List by type
(s/describe dispatch :effects)
(s/describe dispatch :actions)
(s/describe dispatch :placeholders)

;; Inspect specific item
(s/describe dispatch :ascolais.twk/patch-elements)
;; => {:ascolais.sandestin/key :ascolais.twk/patch-elements
;;     :ascolais.sandestin/type :effect
;;     :ascolais.sandestin/description "Patch hiccup elements..."
;;     :ascolais.sandestin/schema [:tuple ...]}
```

### sample - Generate example invocations

```clojure
;; Generate sample effect call
(s/sample dispatch :ascolais.twk/patch-elements)
;; => [:ascolais.twk/patch-elements [:div "generated"]]

;; Generate multiple samples
(s/sample dispatch :ascolais.twk/patch-signals 3)
```

### grep - Search by pattern

```clojure
;; String search (case-insensitive)
(s/grep dispatch "message")

;; Regex search
(s/grep dispatch #"broadcast|connection")
```

### schemas - Get all schemas

```clojure
(s/schemas dispatch)
;; => {:ascolais.twk/patch-elements [:tuple ...]
;;     :ascolais.sfere/broadcast [:tuple ...]
;;     ...}
```

### system-schema - Get system requirements

```clojure
(s/system-schema dispatch)
;; => {:sse [:fn some?] :request [:map ...]}
```

---

## Datastar Frontend Framework

Datastar is a lightweight frontend framework combining backend-driven reactivity with frontend interactivity. It uses HTML `data-*` attributes for declarative behavior and Server-Sent Events (SSE) for backend communication.

### Core Concepts

1. **Backend drives state** - The server is the source of truth; it pushes state to the frontend via SSE
2. **Signals** - Reactive variables prefixed with `$` that auto-propagate changes
3. **Attributes** - `data-*` attributes declare reactive behavior in HTML
4. **Actions** - `@get()`, `@post()`, etc. send requests that return SSE streams

### Signals

Signals are reactive variables accessible throughout the DOM using `$signalName` syntax.

**Creating signals:**

```html
<!-- Via data-bind (two-way binding on form elements) -->
<input data-bind:username />
<!-- Creates signal $username -->

<!-- Via data-signals (direct initialization) -->
<div data-signals:count="0"></div>
<div data-signals="{count: 0, user: {name: 'Alice'}}"></div>

<!-- Via data-computed (derived read-only signals) -->
<div data-computed:doubled="$count * 2"></div>
```

**Naming convention:** Hyphenated attributes become camelCase signals:
- `data-bind:foo-bar` → `$fooBar`
- `data-signals:user-name` → `$userName`

**Nested signals:** Use dot notation for object properties:
```html
<div data-signals:form.email="''"></div>
<span data-text="$form.email"></span>
```

### Attribute Plugins

| Attribute | Purpose | Example |
|-----------|---------|---------|
| `data-text` | Set element text content | `<span data-text="$count"></span>` |
| `data-show` | Toggle visibility | `<div data-show="$isVisible"></div>` |
| `data-class` | Toggle CSS classes | `<div data-class:active="$isActive"></div>` |
| `data-attr` | Set HTML attributes | `<button data-attr:disabled="$loading"></button>` |
| `data-style` | Set inline styles | `<div data-style:color="$textColor"></div>` |
| `data-bind` | Two-way form binding | `<input data-bind:email />` |
| `data-signals` | Initialize signals | `<div data-signals:count="0"></div>` |
| `data-computed` | Derived signals | `<div data-computed:total="$a + $b"></div>` |
| `data-on` | Event handlers | `<button data-on:click="$count++"></button>` |
| `data-ref` | Element references | `<div data-ref:myDiv></div>` |
| `data-indicator` | Loading state tracking | `<button data-indicator:loading></button>` |

**Multiple values syntax:**
```html
<div data-class="{active: $isActive, hidden: $isHidden}"></div>
<div data-attr="{disabled: $loading, 'aria-busy': $loading}"></div>
```

### Event Handling with data-on

```html
<!-- Basic click handler -->
<button data-on:click="$count++">Increment</button>

<!-- Call backend action -->
<button data-on:click="@post('/api/save')">Save</button>

<!-- Multiple expressions -->
<button data-on:click="$loading = true; @post('/api/save')">Save</button>

<!-- With modifiers -->
<input data-on:input__debounce.300ms="@get('/search')" />
<button data-on:click__once="@post('/init')">Initialize</button>
```

**Common modifiers:**
- `__debounce.Nms` - Debounce by N milliseconds
- `__throttle.Nms` - Throttle by N milliseconds
- `__once` - Fire only once
- `__prevent` - preventDefault()
- `__stop` - stopPropagation()

### Lifecycle Attributes

```html
<!-- Run on element load -->
<div data-on-load="@post('/sse')"></div>

<!-- Alternative: data-init -->
<div data-init="$count = 0"></div>

<!-- Run when signals change -->
<div data-effect="console.log('count is', $count)"></div>

<!-- Run on interval -->
<div data-on-interval__duration.1000ms="@get('/poll')"></div>
```

### Backend Actions

Actions send HTTP requests and process SSE responses:

```html
<!-- GET with signals as query params -->
<button data-on:click="@get('/api/data')">Load</button>

<!-- POST with signals as JSON body -->
<button data-on:click="@post('/api/save')">Save</button>

<!-- Other methods -->
<button data-on:click="@put('/api/update')">Update</button>
<button data-on:click="@patch('/api/partial')">Patch</button>
<button data-on:click="@delete('/api/remove')">Delete</button>
```

**Action options:**
```html
<button data-on:click="@post('/api', {
  headers: {'X-Custom': 'value'},
  contentType: 'form'
})">Submit</button>
```

### SSE Events (Backend → Frontend)

The backend sends SSE events that Datastar processes:

**`datastar-patch-elements`** - Update DOM:
```
event: datastar-patch-elements
data: elements <div id="content">New content</div>
```

With options:
```
event: datastar-patch-elements
data: selector #target
data: mode append
data: elements <div>Appended content</div>
```

Patch modes: `morph` (default), `inner`, `outer`, `prepend`, `append`, `before`, `after`, `remove`

**`datastar-patch-signals`** - Update signals:
```
event: datastar-patch-signals
data: signals {count: 42, user: {name: "Bob"}}
```

Delete signals by setting to null:
```
event: datastar-patch-signals
data: signals {oldSignal: null}
```

**`datastar-execute-script`** - Run JavaScript:
```
event: datastar-execute-script
data: script console.log('Hello from server')
```

### Common Patterns

**Form with validation:**
```html
<form data-signals="{email: '', valid: false}">
  <input data-bind:email
         data-on:input="$valid = $email.includes('@')" />
  <button data-attr:disabled="!$valid"
          data-on:click__prevent="@post('/submit')">
    Submit
  </button>
</form>
```

**Loading states:**
```html
<button data-on:click="@post('/api/save')"
        data-indicator:saving
        data-attr:disabled="$saving">
  <span data-show="!$saving">Save</span>
  <span data-show="$saving">Saving...</span>
</button>
```

**Conditional rendering:**
```html
<div data-show="$items.length > 0">
  <!-- content -->
</div>
<div data-show="$items.length === 0">
  No items found
</div>
```

**Real-time updates via SSE:**
```html
<body data-on-load="@get('/sse/connect')">
  <div id="messages">
    <!-- Server pushes updates here -->
  </div>
</body>
```

---

## TWK (Datastar) Patterns

### Hiccup in TWK

TWK understands hiccup directly - you do not need to pre-render it to HTML strings. Return hiccup data structures in `:body` and TWK will render them automatically.

```clojure
;; CORRECT - return hiccup directly
(defn my-page []
  [c/doctype-html5
   [:html [:body [:h1 "Hello"]]]])

{:body (my-page)}

;; WRONG - don't wrap with c/html
;; This returns an encoded HTML string which then requires c/raw
(defn my-page-wrong []
  (c/html  ;; <-- Don't do this
   [c/doctype-html5
    [:html [:body [:h1 "Hello"]]]]))
```

### Handler Response Patterns

```clojure
;; HTML response (non-Datastar request)
{:body [:h1 "Hello"]}
{:body [:h1 "Not Found"] :status 404}

;; Datastar SSE response
{:ascolais.twk/fx
 [[:ascolais.twk/patch-elements [:div "content"]]
  [:ascolais.twk/patch-signals {:count 1}]]}

;; Auto-close SSE after sending
{:ascolais.twk/fx [[:ascolais.twk/patch-elements [:div "done"]]]
 :ascolais.twk/with-open-sse? true}

;; Shorthand alias
{:🚀 [[:ascolais.twk/patch-elements [:div "Rocket!"]]]}
```

### Available TWK Effects

| Effect | SSE Event | Purpose |
|--------|-----------|---------|
| `::twk/patch-elements` | `datastar-patch-elements` | Update DOM elements with hiccup |
| `::twk/patch-signals` | `datastar-patch-signals` | Update Datastar client signals |
| `::twk/execute-script` | `datastar-execute-script` | Run JavaScript in browser |
| `::twk/close-sse` | - | Close SSE connection |

**patch-elements** sends hiccup that gets rendered to HTML and morphed into the DOM.
**patch-signals** sends a Clojure map that becomes the JSON signal patch.

### Patch Modes

```clojure
(require '[ascolais.twk :as twk])

twk/pm-outer    twk/pm-inner    twk/pm-append   twk/pm-prepend
twk/pm-before   twk/pm-after    twk/pm-remove   twk/pm-replace

;; Usage:
[:ascolais.twk/patch-elements [:div "content"]
 {twk/selector "#target" twk/patch-mode twk/pm-append}]
```

### Accessing Signals

Client-side Datastar signals are available on requests:

```clojure
(defn my-handler [{:keys [signals]}]
  (let [{:keys [username message]} signals]
    {:ascolais.twk/fx
     [[:ascolais.twk/patch-elements
       [:div.message [:strong username] ": " message]]]}))
```

---

## Sfere (Connection Management)

### Creating a Store

```clojure
(require '[ascolais.sfere :as sfere])

;; Development (in-memory)
(def store (sfere/store {:type :atom}))

;; Production with TTL
(def store
  (sfere/store
    {:type :caffeine
     :duration-ms 30000
     :expiry-mode :sliding}))  ;; or :fixed
```

### Connection Keys

```clojure
[scope-id [:category id]]

;; Examples:
[:ascolais.sfere/default-scope [:room "lobby"]]
[:user-123 [:game 42]]
[:session-abc [:chat "general" "alice"]]
```

### Storing Connections (Automatic)

In handlers, include `::sfere/key` to auto-store the connection:

```clojure
{::sfere/key [:room "lobby" username]  ;; Inner key (scope added automatically)
 ::twk/fx [[:ascolais.twk/patch-elements [:div "Joined"]]]}
```

### Broadcast Effect

Send effects to all connections matching a pattern:

```clojure
;; Broadcast to pattern (uses :* wildcards)
[[:ascolais.sfere/broadcast {:pattern [:* [:room "lobby" :*]]}
  [:ascolais.twk/patch-elements [:div "announcement"]]]]

;; With exclusions
[[:ascolais.sfere/broadcast
  {:pattern [:* [:room :*]]
   :exclude #{[:user-123 [:room "lobby"]]}}
  [:ascolais.twk/patch-elements [:div "to everyone except user-123"]]]]

;; Multiple effects (variadic)
[[:ascolais.sfere/broadcast {:pattern [:* [:room "lobby" :*]]}
  [:ascolais.twk/patch-signals {:typing false}]
  [:ascolais.twk/patch-elements [:div "User stopped typing"]]]]
```

### With-Connection Effect

Send effects to a specific stored connection:

```clojure
[[:ascolais.sfere/with-connection [:user-123 [:room "lobby"]]
  [:ascolais.twk/patch-signals {:message ""}]]]
```

### Pattern Matching

Use `:*` as wildcard at any position:

```clojure
[:* [:room "lobby" :*]]    ;; All users in "lobby" room
[:* [:room :* :*]]         ;; All users in any room
[:user-123 :*]             ;; All connections for user-123
[:* :*]                    ;; All connections (true broadcast)
```

---

## Kaiin (Declarative Routing)

### Registry with Kaiin Metadata

Add kaiin metadata to actions/effects to auto-generate HTTP routes:

```clojure
(require '[ascolais.kaiin :as kaiin])

(def room-registry
  {::s/actions
   {:room/send-message
    {::s/description "Send a message to a room"
     ::s/schema [:tuple [:= :room/send-message] :string :string :string]
     ::s/handler (fn [_state room-id username message]
                   [[:ascolais.twk/patch-elements
                     [:div.message [:strong username] ": " message]
                     {twk/selector "#messages" twk/patch-mode twk/pm-append}]])

     ;; Kaiin metadata - generates POST /room/:room-id/message
     ::kaiin/path "/room/:room-id/message"
     ::kaiin/method :post
     ::kaiin/signals [:map [:username :string] [:message :string]]
     ::kaiin/dispatch [:room/send-message
                       [::kaiin/path-param :room-id]
                       [::kaiin/signal :username]
                       [::kaiin/signal :message]]
     ::kaiin/target [:* [:room [::kaiin/path-param :room-id] :*]]}}})
```

### Kaiin Metadata Fields

| Field | Purpose |
|-------|---------|
| `::kaiin/path` | HTTP path with reitit params (e.g., `/room/:room-id/message`) |
| `::kaiin/method` | HTTP method (default `:post`) |
| `::kaiin/signals` | Malli schema for expected Datastar signals |
| `::kaiin/dispatch` | Effect vector with token placeholders |
| `::kaiin/target` | Sfere pattern for broadcast (optional - omit for direct response) |

### Token Types

- `[::kaiin/signal :key]` - Extract from Datastar signals
- `[::kaiin/path-param :key]` - Extract from URL path params

### Generating Routes

```clojure
(require '[ascolais.kaiin :as kaiin]
         '[reitit.ring :as rr])

;; Generate routes from dispatch
(kaiin/routes dispatch)

;; Combine with custom routes
(def router
  (rr/router
    (into custom-routes (kaiin/routes dispatch))
    {:data {:middleware [(twk/with-datastar ds-hk/->sse-response dispatch)]}}))
```

### Target Patterns

- **With wildcard** (`::kaiin/target` contains `:*`) - Broadcasts via `::sfere/broadcast`
- **Without wildcard** - Sends to specific connection via `::sfere/with-connection`
- **No target** - Effects dispatched directly to caller's SSE

---

## Complete Example

```clojure
(ns myapp.core
  (:require [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [ascolais.kaiin :as kaiin]
            [reitit.ring :as rr]
            [starfederation.datastar.clojure.adapter.http-kit :as ds-hk]))

;; Application registry with kaiin metadata
(def app-registry
  {::s/actions
   {:app/greet
    {::s/description "Greet a user"
     ::s/schema [:tuple [:= :app/greet] :string]
     ::s/handler (fn [_state name]
                   [[:ascolais.twk/patch-elements
                     [:div#greeting "Hello, " name "!"]]])
     ::kaiin/path "/greet"
     ::kaiin/method :post
     ::kaiin/signals [:map [:name :string]]
     ::kaiin/dispatch [:app/greet [::kaiin/signal :name]]}}})

;; System setup
(def store (sfere/store {:type :caffeine :duration-ms 30000}))

(def dispatch
  (s/create-dispatch
    [(twk/registry)
     (sfere/registry store)
     app-registry]))

;; Router with generated routes
(def router
  (rr/router
    (kaiin/routes dispatch)
    {:data {:middleware [(twk/with-datastar ds-hk/->sse-response dispatch)]}}))

;; REPL exploration
(comment
  ;; Discover available effects/actions
  (s/describe dispatch)

  ;; Inspect specific action
  (s/describe dispatch :app/greet)

  ;; Generate sample invocation
  (s/sample dispatch :app/greet)

  ;; Search for broadcast-related items
  (s/grep dispatch "broadcast"))
```

---

## Adding Dependencies

When adding new dependencies in a REPL-connected environment:

1. **Add to the running REPL first** using `clojure.repl.deps/add-lib`:
   ```clojure
   (clojure.repl.deps/add-lib 'metosin/malli {:mvn/version "0.16.4"})
   ```
   Note: The library name must be quoted.

2. **Confirm the dependency works** by requiring and testing it in the REPL.

3. **Only then add to deps.edn** once confirmed working.

This ensures dependencies are immediately available without restarting the REPL.

## Code Style

- Follow standard Clojure conventions
- Use `cljfmt` formatting (applied automatically via hooks)
- Prefer pure functions where possible
- Use `tap>` for debugging output (appears in Portal)

### Namespaced Keywords

Clojure has two syntaxes for namespaced keywords:

**Single colon (`:`)** - Explicit namespace, works anywhere:
```clojure
:my.app.config/timeout    ; Fully qualified namespace
:ui/visible               ; Arbitrary namespace (doesn't need to exist)
:db/id                    ; Common convention for domain markers
```

**Double colon (`::`)** - Auto-resolved namespace:
```clojure
;; In namespace my.app.core:
::key                     ; Expands to :my.app.core/key

;; With required aliases:
(require '[my.app.db :as db])
::db/query                ; Expands to :my.app.db/query
```

**When to use which:**
- Use `:` with explicit namespace when the keyword meaning is independent of the current file
- Use `::` when the keyword is specific to the current namespace
- Use `::alias/key` to reference keywords from required namespaces without typing the full name
- Prefer `:` for spec keys, component IDs, and data that crosses namespace boundaries

## Git Commits

Use conventional commits format:

```
<type>: <description>

[optional body]
```

Types: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`

Examples:
- `feat: add user authentication`
- `fix: resolve nil pointer in data parser`
- `refactor: simplify database connection logic`
