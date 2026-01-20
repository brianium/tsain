# Spec 011: Discoverable Dev Registry

## Status: Complete

## Problem

The current dev namespace has two parallel APIs:
1. **Ad-hoc helper functions** in `dev.clj`: `preview!`, `commit!`, `show!`, `patch-signals!`, etc.
2. **Sandestin effects** in `sandbox.registry`: `::preview`, `::commit`, `::show`, etc.

This creates friction:
- Claude must learn both the REPL functions AND the effect system
- Documentation lives in docstrings (functions) and descriptions (effects) separately
- Schemas are minimal (`:any` for hiccup) — not useful for discovery
- The long-term goal of tsain as a reusable library is hindered by the bespoke function API

## Solution

Make the sandestin registry **the only API**. Claude (and users) discover the sandbox API via sandestin's REPL discovery functions:

```clojure
(require '[ascolais.tsain :as tsain])

;; Discover what's available
(s/describe dispatch)

;; Inspect specific effect
(s/describe dispatch ::tsain/preview)

;; Generate example invocation
(s/sample dispatch ::tsain/preview)

;; Search by keyword
(s/grep dispatch "component")
```

## Design Principles

1. **Registry-first** — All functionality is effects/actions with rich schemas and descriptions
2. **Self-documenting** — Descriptions explain what, when, and how to use each effect
3. **Sample-driven** — `(s/sample dispatch ::tsain/effect)` generates usable invocations
4. **Single access point** — `dispatch` function in dev namespace, everything else via registry
5. **Shared config** — `tsain.edn` serves both library runtime and Claude/skills

## Configuration: tsain.edn

A single EDN file at project root configures both the library and Claude:

```clojure
;; tsain.edn
{:ui-namespace sandbox.ui                        ;; Where chassis aliases live
 :components-file "resources/components.edn"     ;; Component library persistence
 :stylesheet "dev/resources/public/styles.css"   ;; CSS for hot reload
 :port 3000}                                     ;; Sandbox server port
```

**Library reads for:**
- `components-file` → load/save component library
- `stylesheet` → CSS hot reload watcher
- `port` → server startup

**Claude/skill reads for:**
- `ui-namespace` → where to add chassis aliases
- `stylesheet` → where to add CSS classes
- `components-file` → where examples are stored

**Defaults (if no tsain.edn):**
```clojure
{:ui-namespace sandbox.ui
 :components-file "resources/components.edn"
 :stylesheet "dev/resources/public/styles.css"
 :port 3000}
```

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        tsain.edn                                 │
│  Shared config for library + Claude                              │
│  {:ui-namespace sandbox.ui :components-file "..." :port 3000}    │
└──────────────────────────────┬──────────────────────────────────┘
                               │
            ┌──────────────────┴──────────────────┐
            ▼                                     ▼
┌───────────────────────────┐       ┌─────────────────────────────┐
│   ascolais.tsain          │       │   Claude / Skills           │
│   (tsain/registry)        │       │   Reads config for paths    │
│   - Reads tsain.edn       │       │   - ui-namespace            │
│   - Creates state atom    │       │   - stylesheet              │
│   - Returns registry map  │       │   - components-file         │
└─────────────┬─────────────┘       └─────────────────────────────┘
              │
              │ composed into dispatch
              ▼
┌─────────────────────────────────────────────────────────────────┐
│                          dev.clj                                 │
│  Minimal: dispatch accessor + system lifecycle                   │
│                                                                  │
│  (def dispatch (s/create-dispatch [(tsain/registry) ...]))      │
│  (defn start [] ...)                                             │
│  (defn stop [] ...)                                              │
└─────────────────────────────┬───────────────────────────────────┘
                              │
                              │ (dispatch [[::tsain/preview ...]])
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   ascolais.tsain effects                         │
│  Rich schemas + descriptions                                     │
│                                                                  │
│  ::tsain/preview     "Replace preview with hiccup content..."    │
│  ::tsain/commit      "Commit component to library..."            │
│  ::tsain/show        "Navigate to single component view..."      │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ s/describe, s/sample, s/grep
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Claude / REPL User                          │
│  Discovers API via sandestin                                     │
│                                                                  │
│  (s/describe dispatch)                                           │
│  (s/sample dispatch ::tsain/preview)                             │
│  (s/grep dispatch "component")                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Registry Design

### Effect Namespace

Change from `::registry/` (auto-resolved in sandbox.registry) to `::tsain/` (auto-resolved in ascolais.tsain). This makes effects:
- Consistent with other ascolais libraries (twk, sfere, kaiin)
- Ready for extraction as standalone library
- Discoverable via `(require '[ascolais.tsain :as tsain])`

Usage:
```clojure
(require '[ascolais.tsain :as tsain])

(dispatch [[::tsain/preview [:div "Hello"]]])
;; Expands to [:ascolais.tsain/preview ...]
```

### Registry Factory

The library exports a `registry` function that reads `tsain.edn` and returns a sandestin registry:

```clojure
;; Zero-config (reads tsain.edn, uses defaults for missing keys)
(tsain/registry)

;; Programmatic override (merges with tsain.edn)
(tsain/registry {:port 3001})
```

Internally, the registry:
- Creates and manages its own state atom
- Loads component library from `components-file`
- Sets up CSS hot reload for `stylesheet`

### Schema Improvements

Replace `:any` with expressive schemas that generate useful samples:

```clojure
;; Before
::s/schema [:tuple [:= ::preview] :any]

;; After
::s/schema [:tuple [:= ::tsain/preview]
            [:vector {:description "Hiccup element"
                      :gen/elements [[:div [:h1 "Hello"]]]}
             :any]]
```

### Description Guidelines

Descriptions should answer:
- **What** does this effect do?
- **When** should you use it vs. alternatives?
- **How** are arguments used?

```clojure
;; Before
::s/description "Set preview content and broadcast to clients"

;; After
::s/description "Replace the preview area with hiccup content.
Broadcasts to all connected browsers. Use for iterating on new components.
For appending to existing content, use ::tsain/preview-append instead."
```

## Proposed Effects

### Preview Effects

| Effect | Description |
|--------|-------------|
| `::tsain/preview` | Replace preview area with hiccup (broadcasts) |
| `::tsain/preview-append` | Append hiccup to preview (broadcasts) |
| `::tsain/preview-clear` | Clear preview area (broadcasts) |

### Component Library Effects

| Effect | Description |
|--------|-------------|
| `::tsain/commit` | Save component to library with examples |
| `::tsain/uncommit` | Remove component from library |
| `::tsain/show` | View single component at index |
| `::tsain/show-components` | View components sidebar |
| `::tsain/show-preview` | Switch to preview view |

### Query Actions

| Action | Description |
|--------|-------------|
| `::tsain/components` | Return list of committed component names |

### Signal Effects

| Effect | Description |
|--------|-------------|
| `::tsain/patch-signals` | Broadcast Datastar signal patch |

## Sample Schemas

### ::tsain/preview

```clojure
{::tsain/preview
 {::s/description "Replace the sandbox preview area with hiccup content.

  Broadcasts to all connected browsers/devices simultaneously.
  Use this when iterating on new components before committing.
  For adding to existing content, use ::tsain/preview-append.

  Example:
    [::tsain/preview [:div.my-card [:h2 \"Title\"] [:p \"Body\"]]]"

  ::s/schema
  [:tuple [:= ::tsain/preview]
   [:vector {:description "Hiccup element to render"
             :gen/elements [[:div [:h1 "Hello World"]]
                            [:div.card [:h2 "Title"] [:p "Content"]]]}
    :any]]

  ::s/handler (fn [{:keys [dispatch]} _system hiccup] ...)}}
```

### ::tsain/commit

```clojure
{::tsain/commit
 {::s/description "Commit a component to the library.

  Saves to both in-memory library and components.edn for persistence.
  Components should use chassis aliases (see :ui-namespace in tsain.edn).

  Accepts three forms:
  1. Name only: uses current preview as single 'Default' example
  2. Name + description string: adds description to preview-based example
  3. Name + options map: full control with :description and :examples

  Examples:
    [::tsain/commit :my-card]
    [::tsain/commit :my-card \"Card component\"]
    [::tsain/commit :my-card {:description \"...\" :examples [...]}]"

  ::s/schema
  [:tuple [:= ::tsain/commit]
   [:keyword {:description "Component name (e.g., :game-card)"}]
   [:maybe [:or
            [:string {:description "Simple description"}]
            [:map {:description "Full options"}
             [:description {:optional true} :string]
             [:examples {:optional true}
              [:vector [:map
                        [:label :string]
                        [:hiccup :any]]]]]]]]

  ::s/handler (fn [{:keys [dispatch]} _system name opts] ...)}}
```

### ::tsain/components (Action)

```clojure
{::s/actions
 {::tsain/components
  {::s/description "List all committed component names.

   Returns a sequence of keywords representing library entries.
   Use to see what's available before calling ::tsain/show.

   Example:
     [::tsain/components]
   Returns:
     (:game-card :player-hud :action-buttons)"

   ::s/schema [:tuple [:= ::tsain/components]]

   ::s/handler (fn [state]
                 (keys (:library state)))}}}
```

## Dev Namespace Changes

### Remove

- `preview!` function
- `preview-append!` function
- `preview-clear!` function
- `commit!` function
- `uncommit!` function
- `show!` function
- `show-all!` function
- `components` function
- `patch-signals!` function

### Keep

- `dispatch` — Access to system dispatch (required for invoking effects)
- `start` / `stop` / `reload` / `restart` — System lifecycle
- Portal setup — Development tooling

### Add

- Require tsain with alias:

```clojure
(ns dev
  (:require [ascolais.tsain :as tsain]
            [ascolais.sandestin :as s]
            ...))
```

- Convenience aliases for sandestin discovery:

```clojure
(def describe s/describe)
(def sample s/sample)
(def grep s/grep)

;; Usage from REPL:
(describe dispatch)
(sample dispatch ::tsain/preview)
(grep dispatch "commit")
```

## REPL Workflow

### Before (Ad-hoc Functions)

```clojure
(dev)
(start)
(preview! [:div.card "Hello"])
(commit! :my-card "A card")
(show! :my-card)
(components)
```

### After (Registry Discovery)

```clojure
(dev)
(start)

;; Discover the API
(describe dispatch)
;; => Lists all ::tsain/* effects

(sample dispatch ::tsain/preview)
;; => [:ascolais.tsain/preview [:div [:h1 "Hello World"]]]

;; Use the API
(dispatch [[::tsain/preview [:div.card "Hello"]]])
(dispatch [[::tsain/commit :my-card "A card"]])
(dispatch [[::tsain/show :my-card]])

;; Query actions return values
(dispatch [[::tsain/components]])
;; => (:my-card)
```

## Skill Updates

### component-iterate Skill

Update to read `tsain.edn` for file paths:

```markdown
## Configuration

Read `tsain.edn` at project root for file locations:
- `:ui-namespace` → where to add chassis aliases
- `:stylesheet` → where to add CSS classes
- `:components-file` → where examples are stored
```

Update REPL API Reference table:

| Before | After |
|--------|-------|
| `(dev/preview! hiccup)` | `(dispatch [[::tsain/preview hiccup]])` |
| `(dev/commit! :name)` | `(dispatch [[::tsain/commit :name]])` |
| `(dev/show! :name)` | `(dispatch [[::tsain/show :name]])` |

Add discovery section:

```markdown
## Discovering the API

Use sandestin discovery to explore available effects:

\`\`\`clojure
(require '[ascolais.tsain :as tsain])

;; List all tsain effects
(describe dispatch)

;; See schema and docs for an effect
(describe dispatch ::tsain/preview)

;; Generate example invocation
(sample dispatch ::tsain/preview)

;; Search by keyword
(grep dispatch "component")
\`\`\`
```

### fx-explore Skill (if exists)

Ensure it documents using `dispatch` from dev namespace.

## Migration Path

1. **Create `tsain.edn`** with current configuration
2. **Create `src/clj/ascolais/tsain.clj`** with registry factory
3. **Move effects** from `sandbox/registry.clj` to `ascolais/tsain.clj`
4. **Add rich schemas and descriptions** to all effects
5. **Change effect namespace** from `::registry/` to `::tsain/`
6. **Update dev.clj** to use `(tsain/registry)` and add discovery aliases
7. **Remove wrapper functions** from dev.clj
8. **Update component-iterate skill** with discovery workflow and tsain.edn
9. **Update CLAUDE.md** with registry-first workflow

## Future: Companion Library

This spec prepares tsain to become a reusable companion library:

```clojure
;; In any project's deps.edn
{:deps {io.github.brianium/tsain {:git/tag "v0.1.0" :git/sha "..."}}}

;; In any project's dev.clj
(require '[ascolais.tsain :as tsain])

(def dispatch
  (s/create-dispatch
    [(tsain/registry)        ;; Reads tsain.edn automatically
     (twk/registry)
     (sfere/registry store)
     (my-app/registry)]))

;; Same discovery workflow works
(s/describe dispatch)
(s/sample dispatch ::tsain/preview)
```

The registry-first approach means:
- No coupling to specific function names
- Configuration via `tsain.edn` (shared with Claude/skills)
- Standard sandestin discovery
- Drop-in addition to any sandestin project

## Testing

Unit tests in `test/src/clj/ascolais/tsain_test.clj` covering:

### Registry Factory Tests
- `(tsain/registry)` returns valid sandestin registry map
- Config loading from `tsain.edn` with defaults for missing keys
- Programmatic override merges with file config
- Missing `tsain.edn` uses all defaults

### Schema Validation Tests
- All effects have valid Malli schemas (`:ascolais.sandestin/schema`)
- Schemas reject invalid inputs
- `:gen/elements` produce valid samples

### Effect Registration Tests
- All expected effects registered under `::tsain/` namespace
- Effects have non-empty descriptions
- Effect handlers are functions

Run tests with: `clj -X:test`

## Done When

- [x] `tsain.edn` exists at project root with configuration
- [x] `src/clj/ascolais/tsain.clj` exists with registry factory
- [x] Registry factory reads `tsain.edn` and returns registry map
- [x] All effects use `::tsain/` namespace (`:ascolais.tsain/*`)
- [x] All effects have rich schemas with `:description` and `:gen/elements`
- [x] All effects have multi-line descriptions explaining what/when/how
- [x] dev.clj uses `(tsain/registry)` in dispatch composition
- [x] dev.clj exports `describe`, `sample`, `grep` aliases
- [x] dev.clj removes ad-hoc helper functions
- [x] component-iterate skill reads `tsain.edn` for file paths
- [x] component-iterate skill documents discovery workflow
- [x] `(s/describe dispatch)` shows useful API overview
- [x] `(s/sample dispatch ::tsain/preview)` generates usable hiccup
- [x] CLAUDE.md updated with registry-first dev workflow
- [x] Unit tests pass for registry factory, config loading, and schema validation

## File Changes

```
tsain.edn                      # NEW: Shared config for library + Claude

src/clj/ascolais/
└── tsain.clj                  # UPDATED: Registry factory + effects (was stub)

test/src/clj/ascolais/
└── tsain_test.clj             # UPDATED: Unit tests for registry factory

dev/src/clj/
├── dev.clj                    # Simplified: dispatch + lifecycle + discovery aliases
└── sandbox/
    ├── app.clj                # UPDATED: Uses tsain/registry
    ├── views.clj              # Keep: view rendering
    ├── state.clj              # REMOVED: persistence moved to tsain.clj
    └── registry.clj           # REMOVED: effects moved to ascolais/tsain.clj

.claude/skills/
└── component-iterate/
    └── SKILL.md               # UPDATED: tsain.edn + discovery workflow

CLAUDE.md                      # UPDATED: Registry-first dev workflow section
```

## Relationship to Other Specs

- **004-component-library**: Foundation for commit/show effects
- **006-dynamic-components**: patch-signals effect
- **010-chassis-alias-workflow**: Component structure in aliases
