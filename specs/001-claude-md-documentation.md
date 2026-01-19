# 001: CLAUDE.md Documentation Spec

## Status: Active

## Overview

This spec defines the comprehensive CLAUDE.md documentation for the tsain project. The documentation should enable Claude to effectively work with the sandestin ecosystem: creating registries, building dispatch functions, using kaiin for declarative routing, and leveraging REPL-driven discovery.

## Goals

1. Provide clear mental model of the sandestin effect dispatch system
2. Document registry authoring patterns with concrete examples
3. Explain dispatch creation and the execution flow
4. Teach REPL-based discovery using sandestin's introspection API
5. Document kaiin's declarative routing approach
6. Show complete working patterns for twk (Datastar) and sfere (connections)

## CLAUDE.md Structure

### 1. Project Overview Section

Brief description of tsain as a sandestin-powered Datastar application.

**Key Libraries:**
- sandestin - Effect dispatch with schema-driven discoverability
- twk - Datastar SSE integration (provides `twk/registry`)
- sfere - Connection management and broadcasting (provides `sfere/registry`)
- kaiin - Declarative HTTP routing from registry metadata

### 2. Architecture Mental Model

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

### 3. Registry Authoring Section

#### Effect Structure

```clojure
{::s/effects
 {:app/my-effect
  {::s/description "What this effect does"
   ::s/schema [:tuple [:= :app/my-effect] <arg-schemas...>]
   ::s/system-keys [:optional :system :keys]
   ::s/handler (fn [{:keys [dispatch dispatch-data system]} system & args]
                 ;; Side effects here
                 result)}}}
```

**Key points:**
- Schema describes invocation shape: `[:tuple [:= :qualified/key] <args>]`
- Handler receives context map with `dispatch` for continuations
- `system-keys` documents which system keys the effect needs

#### Action Structure

```clojure
{::s/actions
 {:app/my-action
  {::s/description "What this action does"
   ::s/schema [:tuple [:= :app/my-action] <arg-schemas...>]
   ::s/handler (fn [state & args]
                 ;; Pure function returning effect vector
                 [[::twk/patch-elements [:div "result"]]
                  [::twk/patch-signals {:updated true}]])}}}
```

**Key points:**
- Actions are pure functions (no side effects)
- Return vectors of effects to dispatch
- Receive `state` from `::s/system->state` extraction

#### Placeholder Structure

```clojure
{::s/placeholders
 {:app/current-user
  {::s/description "Get current user from dispatch context"
   ::s/schema :map
   ::s/handler (fn [dispatch-data]
                 (:current-user dispatch-data))}}}
```

### 4. Dispatch Creation Section

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

### 5. REPL Discovery Section (Critical)

This section teaches Claude how to explore a dispatch interactively.

#### describe - List and inspect registered items

```clojure
;; List all items
(s/describe dispatch)

;; List by type
(s/describe dispatch :effects)
(s/describe dispatch :actions)
(s/describe dispatch :placeholders)

;; Inspect specific item
(s/describe dispatch ::twk/patch-elements)
;; => {:ascolais.sandestin/key ::twk/patch-elements
;;     :ascolais.sandestin/type :effect
;;     :ascolais.sandestin/description "Patch hiccup elements..."
;;     :ascolais.sandestin/schema [:tuple ...]}
```

#### sample - Generate example invocations

```clojure
;; Generate sample effect call
(s/sample dispatch ::twk/patch-elements)
;; => [::twk/patch-elements [:div "generated"]]

;; Generate multiple samples
(s/sample dispatch ::twk/patch-signals 3)
```

#### grep - Search by pattern

```clojure
;; String search (case-insensitive)
(s/grep dispatch "message")

;; Regex search
(s/grep dispatch #"broadcast|connection")
```

#### schemas - Get all schemas

```clojure
(s/schemas dispatch)
;; => {::twk/patch-elements [:tuple ...]
;;     ::sfere/broadcast [:tuple ...]
;;     ...}
```

#### system-schema - Get system requirements

```clojure
(s/system-schema dispatch)
;; => {:sse [:fn some?] :request [:map ...]}
```

### 6. Kaiin Declarative Routing Section

#### Registry with Kaiin Metadata

```clojure
(def room-registry
  {::s/actions
   {:room/send-message
    {::s/description "Send a message to a room"
     ::s/schema [:tuple [:= :room/send-message] :string :string :string]
     ::s/handler (fn [_state room-id username message]
                   [[::twk/patch-elements [:div.message username ": " message]
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

#### Kaiin Metadata Fields

| Field | Purpose |
|-------|---------|
| `::kaiin/path` | HTTP path with reitit params |
| `::kaiin/method` | HTTP method (default `:post`) |
| `::kaiin/signals` | Malli schema for expected Datastar signals |
| `::kaiin/dispatch` | Effect vector with token placeholders |
| `::kaiin/target` | Sfere pattern for broadcast (optional) |

#### Token Types

- `[::kaiin/signal :key]` - Extract from Datastar signals
- `[::kaiin/path-param :key]` - Extract from URL path params

#### Generating Routes

```clojure
(require '[ascolais.kaiin :as kaiin])

;; Generate routes from dispatch
(kaiin/routes dispatch)

;; Combine with custom routes
(def router
  (rr/router
    (into custom-routes (kaiin/routes dispatch))
    {:data {:middleware [(twk/with-datastar ds-hk/->sse-response dispatch)]}}))
```

### 7. TWK (Datastar) Patterns Section

#### Handler Response Patterns

```clojure
;; HTML response (non-Datastar request)
{:body [:h1 "Hello"]}

;; Datastar SSE response
{::twk/fx [[::twk/patch-elements [:div "content"]]
           [::twk/patch-signals {:count 1}]]}

;; Auto-close SSE after sending
{::twk/fx [[::twk/patch-elements [:div "done"]]]
 ::twk/with-open-sse? true}
```

#### Available Effects

- `::twk/patch-elements` - Update DOM elements
- `::twk/patch-signals` - Update client signals
- `::twk/execute-script` - Run JavaScript
- `::twk/close-sse` - Close SSE connection

#### Patch Modes

```clojure
twk/pm-outer twk/pm-inner twk/pm-append twk/pm-prepend
twk/pm-before twk/pm-after twk/pm-remove twk/pm-replace
```

### 8. Sfere (Connections) Section

#### Creating a Store

```clojure
;; Development
(def store (sfere/store {:type :atom}))

;; Production with TTL
(def store
  (sfere/store
    {:type :caffeine
     :duration-ms 30000
     :expiry-mode :sliding}))
```

#### Broadcast Effect

```clojure
;; Broadcast to pattern
[[::sfere/broadcast {:pattern [:* [:room "lobby" :*]]}
  [::twk/patch-elements [:div "announcement"]]]]

;; With exclusions
[[::sfere/broadcast {:pattern [:* [:room :*]]
                     :exclude #{[:user-123 [:room "lobby"]]}}
  [::twk/patch-elements [:div "to everyone except user-123"]]]]
```

#### With-Connection Effect

```clojure
;; Send to specific connection
[[::sfere/with-connection [:user-123 [:room "lobby"]]
  [::twk/patch-signals {:typing false}]]]
```

#### Connection Keys

```clojure
[scope-id [:category id]]
;; Examples:
[::sfere/default-scope [:room "lobby"]]
[:user-123 [:game 42]]
```

### 9. Complete Working Example Section

Show a minimal but complete example combining all concepts:

```clojure
(ns myapp.core
  (:require [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [ascolais.kaiin :as kaiin]
            [reitit.ring :as rr]
            [dev.data-star.http-kit :as ds-hk]))

;; Application registry with kaiin metadata
(def app-registry
  {::s/actions
   {:app/greet
    {::s/description "Greet a user"
     ::s/schema [:tuple [:= :app/greet] :string]
     ::s/handler (fn [_state name]
                   [[::twk/patch-elements [:div#greeting "Hello, " name "!"]]])
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

  ;; Search for message-related items
  (s/grep dispatch "message"))
```

## Documentation Principles

1. **Code examples are primary** - Every concept should have a concrete example
2. **REPL discovery is essential** - Teach Claude to explore, not memorize
3. **Composition over configuration** - Show how registries compose
4. **Schema-driven** - Emphasize schemas for discoverability
5. **Working examples** - All examples should be runnable

## Success Criteria

- Claude can create new effects and actions with proper schemas
- Claude can compose registries into a dispatch
- Claude can use REPL discovery to explore available effects
- Claude can add kaiin metadata for declarative routing
- Claude can use twk and sfere effects appropriately
- Examples are copy-pasteable and work
