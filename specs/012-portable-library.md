# Spec 012: Portable Library

## Status: Draft

## Problem

Tsain currently provides excellent component development tooling, but it's coupled to this project:

1. **Routes are hardcoded** in `sandbox/app.clj` — not reusable
2. **Views assume project structure** — file paths, namespaces baked in
3. **CLAUDE.md is monolithic** — contains project-specific + ecosystem docs together
4. **Skills live in `.claude/skills/`** — no distribution mechanism
5. **Assets served ad-hoc** — no clear resource packaging

This prevents using tsain as a companion library for other sandestin/twk projects. Each new project would need to copy and adapt significant boilerplate.

## Solution

Extract tsain as a distributable library that any sandestin/twk project can add:

1. **Library exports** — Registry factory + route factory (configured via `tsain.edn`)
2. **Sample files** — CLAUDE.md, tsain.edn, starter ui.clj for consumers to copy
3. **Skill** — `component-iterate` teaches Claude the full workflow via discovery

Consumer experience:

```clojure
;; deps.edn
{:deps {io.github.brianium/tsain {:git/tag "v1.0.0" :git/sha "..."}}}

;; dev.clj
(require '[ascolais.tsain :as tsain])

(def dispatch
  (s/create-dispatch
    [(tsain/registry)           ;; Reads tsain.edn
     (twk/registry)
     (sfere/registry store)]))

(def routes
  (into my-app-routes
        (tsain/routes dispatch state-atom)))
```

## Design Principles

1. **Configuration-driven** — `tsain.edn` controls all paths and namespaces
2. **Composition over inheritance** — Routes merge into consumer's router
3. **Discovery-first** — Claude learns via sandestin's `describe`/`sample`/`grep`
4. **Copy-friendly samples** — No magic includes, just files to copy/adapt
5. **Skill as orchestrator** — Workflow knowledge lives in the skill, not scattered docs

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Consumer Project                                   │
│                                                                              │
│  ┌─────────────┐   ┌──────────────┐   ┌─────────────────────────────────┐  │
│  │ tsain.edn   │   │ CLAUDE.md    │   │ .claude/skills/component-iterate│  │
│  │ (copy+edit) │   │ (copy+merge) │   │ (copy from sample/)             │  │
│  └──────┬──────┘   └──────────────┘   └─────────────────────────────────┘  │
│         │                                                                    │
│         │ reads config                                                       │
│         ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         dev.clj                                      │   │
│  │  (def dispatch (s/create-dispatch [(tsain/registry) ...]))          │   │
│  │  (def routes (into app-routes (tsain/routes dispatch state)))       │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│         │                                      │                            │
│         │ dispatch effects                     │ serve routes               │
│         ▼                                      ▼                            │
│  ┌──────────────────┐              ┌────────────────────────────────────┐  │
│  │ ::tsain/preview  │              │ /sandbox, /sandbox/sse, etc.       │  │
│  │ ::tsain/commit   │              │ Views rendered by tsain/views      │  │
│  │ ::tsain/show-*   │              │ Assets served from consumer paths  │  │
│  └──────────────────┘              └────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                           tsain library (git dep)                            │
│                                                                              │
│  src/clj/ascolais/                                                          │
│  ├── tsain.clj          # Registry factory (effects, state management)      │
│  └── tsain/                                                                  │
│      ├── routes.clj     # Route factory (reitit route data)                 │
│      └── views.clj      # View rendering functions                          │
│                                                                              │
│  resources/tsain/                                                            │
│  └── sandbox.css        # Sandbox chrome styles                             │
│                                                                              │
│  sample/                                                                     │
│  ├── CLAUDE.md          # Ecosystem docs + tsain workflow                   │
│  ├── tsain.edn          # Example configuration                             │
│  └── ui.clj             # Starter chassis alias file                        │
│                                                                              │
│  .claude/skills/component-iterate/                                           │
│  └── SKILL.md           # Component iteration workflow (copy to consumer)   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

## Consumer Setup Flow

### 1. Add Dependency

```clojure
;; deps.edn
{:deps {io.github.brianium/tsain {:git/tag "v1.0.0" :git/sha "..."}}}
```

### 2. Copy Sample Files

```bash
# From tsain repo:
cp sample/tsain.edn ./tsain.edn
cp sample/CLAUDE.md ./CLAUDE.md          # Or merge into existing
cp sample/ui.clj ./dev/src/clj/myapp/ui.clj
cp -r .claude/skills/component-iterate .claude/skills/
```

### 3. Configure tsain.edn

```clojure
;; tsain.edn
{:ui-namespace myapp.ui                          ;; Your alias namespace
 :components-file "resources/components.edn"     ;; Where to persist library
 :stylesheet "dev/resources/public/styles.css"   ;; CSS for hot reload
 :sandbox-css "/sandbox.css"                     ;; Route for chrome styles
 :port 3000}
```

### 4. Wire Up Routes

```clojure
(ns dev
  (:require [ascolais.tsain :as tsain]
            [ascolais.tsain.routes :as tsain.routes]
            [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [reitit.ring :as rr]))

;; Create store and dispatch
(def store (sfere/store {:type :atom}))

(def dispatch
  (s/create-dispatch
    [(tsain/registry)
     (twk/registry)
     (sfere/registry store)
     my-app-registry]))

;; Get tsain state for routes
(def tsain-state (tsain/state dispatch))

;; Compose routes
(def router
  (rr/router
    (into my-app-routes
          (tsain.routes/routes dispatch tsain-state))
    {:data {:middleware [(twk/with-datastar ds-hk/->sse-response dispatch)]}}))
```

### 5. Serve Assets

Consumer adds static file serving for:
- Their configured `:stylesheet` path
- Tsain's `sandbox.css` (from classpath or copied)

```clojure
;; Example with ring
["/sandbox.css" {:get (fn [_] (io/resource "tsain/sandbox.css"))}]
["/styles.css" {:get (fn [_] (slurp "dev/resources/public/styles.css"))}]
```

### 6. Start Developing

```clojure
(dev)
(start)

;; Discover available effects
(s/describe dispatch)
(s/sample dispatch ::tsain/preview)

;; Start iterating
(dispatch [[::tsain/preview [:div.my-component "Hello"]]])
```

## Library Exports

### tsain/registry

Factory function returning sandestin registry:

```clojure
(tsain/registry)              ;; Reads tsain.edn, uses defaults
(tsain/registry {:port 3001}) ;; Override specific keys
```

Returns registry with:
- All `::tsain/*` effects (preview, commit, show, etc.)
- Internal state atom for component library
- CSS watcher setup (if `:stylesheet` configured)

### tsain/state

Extract state atom from dispatch for route handlers:

```clojure
(tsain/state dispatch) ;; => atom with {:preview ... :library ... :view ...}
```

### tsain.routes/routes

Factory function returning reitit route data:

```clojure
(tsain.routes/routes dispatch state-atom)
;; => [["/sandbox" {...}]
;;     ["/sandbox/sse" {...}]
;;     ["/sandbox/c/:name" {...}]
;;     ...]
```

Routes handle:
- `/sandbox` — Main sandbox page
- `/sandbox/sse` — SSE connection endpoint
- `/sandbox/c/:name` — Single component view
- `/sandbox/view/*` — View switching endpoints
- `/sandbox/commit` — Component commit
- `/sandbox/copy/:name` — Get hiccup for clipboard

### tsain.views (internal)

View rendering functions used by routes. Not typically called directly by consumers.

## Sample Files

### sample/CLAUDE.md

Complete reference documentation containing:

**Section 1: Project placeholder**
```markdown
# My Project

## Project Overview
[Consumer fills this in]

## Development Setup
[Consumer fills this in]
```

**Section 2: Sandestin Ecosystem Reference**
- Registry authoring (effects, actions, placeholders)
- Discovery API (describe, sample, grep)
- Datastar frontend framework (signals, attributes, SSE)
- TWK patterns (hiccup, response shapes, patch modes)
- Sfere connection management (stores, broadcasts)
- Kaiin declarative routing (metadata, tokens)

**Section 3: Tsain Workflow**
- Component preview workflow
- Chassis alias conventions
- CSS extraction patterns
- Commit workflow

**Section 4: Configuration**
- tsain.edn reference
- File locations

### sample/tsain.edn

```clojure
{;; Namespace where chassis aliases are defined
 ;; Claude will add new aliases here during component iteration
 :ui-namespace myapp.ui

 ;; Where the component library is persisted
 :components-file "resources/components.edn"

 ;; CSS file for component styles (watched for hot reload)
 :stylesheet "dev/resources/public/styles.css"

 ;; Port for sandbox server (if using tsain's server helper)
 :port 3000}
```

### sample/ui.clj

Starter chassis alias file:

```clojure
(ns myapp.ui
  "Chassis aliases for UI components.

   Define component structure here. Use namespaced keywords for config props
   that should be elided from HTML output.

   Example:
     (defmethod c/resolve-alias ::my-card [_ attrs _]
       (let [{:my-card/keys [title body]} attrs]
         [:div.my-card attrs
          [:h2.my-card-title title]
          [:p.my-card-body body]]))"
  (:require [dev.onionpancakes.chassis.core :as c]))

;; Define your component aliases below
;; See CLAUDE.md for chassis alias conventions
```

### .claude/skills/component-iterate/SKILL.md

The component iteration skill lives in tsain's own `.claude/skills/` and is written generically so consumers can copy it directly. Key sections:

**Discovery-first approach:**
```markdown
## Discovering the API

Before starting, explore available effects:

\`\`\`clojure
(require '[ascolais.tsain :as tsain]
         '[ascolais.sandestin :as s])

;; List all tsain effects
(s/describe dispatch)

;; Inspect specific effect
(s/describe dispatch ::tsain/preview)

;; Generate sample invocation
(s/sample dispatch ::tsain/preview)
\`\`\`
```

**Configuration awareness:**
```markdown
## Configuration

Read `tsain.edn` at project root for file locations:

| Key | Purpose | Used for |
|-----|---------|----------|
| `:ui-namespace` | Alias namespace | Where to add `defmethod c/resolve-alias` |
| `:stylesheet` | CSS file | Where to add component classes |
| `:components-file` | Library persistence | Where examples are stored |
```

**Workflow steps:**
```markdown
## Component Iteration Workflow

### Phase 1: Explore with Plain Hiccup

1. Preview raw hiccup to iterate on structure:
   \`\`\`clojure
   (dispatch [[::tsain/preview
               [:div {:style "..."}
                [:h2 "Title"]]]])
   \`\`\`

2. Use inline styles for rapid exploration

3. Iterate until the visual design is solid

### Phase 2: Extract to Chassis Alias

1. Read `:ui-namespace` from `tsain.edn`
2. Create `defmethod c/resolve-alias` in that namespace
3. Use namespaced keywords for config props (`:component-name/prop`)
4. Structure passes through to root element for Datastar attrs

### Phase 3: Extract CSS

1. Read `:stylesheet` from `tsain.edn`
2. Move inline styles to CSS classes
3. Use BEM-like naming: `.component-name`, `.component-name-element`
4. Use CSS custom properties for theming

### Phase 4: Commit to Library

1. Update preview to use the alias:
   \`\`\`clojure
   (dispatch [[::tsain/preview
               [:myapp.ui/my-card
                {:my-card/title "Example"
                 :my-card/body "Content"}]]])
   \`\`\`

2. Commit with description:
   \`\`\`clojure
   (dispatch [[::tsain/commit :my-card "Brief description"]])
   \`\`\`
```

**Optional enhancements:**
```markdown
## Optional: Enhanced Iteration

For accelerated design iteration, consider:

- **Chrome extension** — Screenshot-driven feedback loop
- **frontend-design skill** — AI-generated component designs

These are optional power-ups. The core workflow works with just:
- A REPL
- A browser at localhost:3000
- The discovery API
```

## File Structure Changes

### Current Structure (Project-Coupled)

```
tsain/
├── src/clj/ascolais/tsain.clj       # Registry only
├── dev/src/clj/
│   ├── dev.clj                       # Lifecycle + helpers
│   └── sandbox/
│       ├── app.clj                   # Routes (hardcoded)
│       ├── views.clj                 # Views (hardcoded paths)
│       ├── ui.clj                    # Project aliases
│       └── watcher.clj               # CSS watcher
├── dev/resources/public/
│   ├── sandbox.css                   # Chrome styles
│   └── styles.css                    # Component styles
├── resources/components.edn          # Component library
├── CLAUDE.md                         # Monolithic docs
└── .claude/skills/component-iterate/ # Project skill
```

### Target Structure (Portable Library)

```
tsain/
├── src/clj/ascolais/
│   ├── tsain.clj                     # Registry factory + state
│   └── tsain/
│       ├── routes.clj                # Route factory
│       ├── views.clj                 # View rendering (config-driven)
│       └── watcher.clj               # CSS watcher utility
│
├── resources/tsain/
│   └── sandbox.css                   # Chrome styles (classpath resource)
│
├── sample/
│   ├── CLAUDE.md                     # Copy to project root
│   ├── tsain.edn                     # Copy to project root
│   └── ui.clj                        # Copy to project's ui namespace
│
├── .claude/skills/component-iterate/ # Copy to consumer's .claude/skills/
│   └── SKILL.md                      # Generic, works with any tsain project
│
├── dev/                              # Tsain's own dev setup (dogfooding)
│   ├── src/clj/
│   │   ├── dev.clj
│   │   └── sandbox/ui.clj            # Tsain's own aliases
│   └── resources/public/
│       └── styles.css                # Tsain's own component styles
│
├── resources/components.edn          # Tsain's own component library
├── tsain.edn                         # Tsain's own config
├── CLAUDE.md                         # Tsain dev docs (not the sample)
└── .claude/skills/component-iterate/ # Generic skill (consumers copy this)
```

## Route Factory Design

### Input

```clojure
(tsain.routes/routes dispatch state-atom)
```

- `dispatch` — Sandestin dispatch function (for handlers)
- `state-atom` — Tsain state atom (for current view, library access)

### Output

Reitit route data vector:

```clojure
[["/sandbox" {:get sandbox-handler}]
 ["/sandbox/sse" {:post sse-handler}]
 ["/sandbox/c/:name" {:get component-handler}]
 ["/sandbox/view/preview" {:post view-preview-handler}]
 ["/sandbox/view/components" {:post view-components-handler}]
 ["/sandbox/view/component/:name" {:post view-component-handler}]
 ["/sandbox/commit" {:post commit-handler}]
 ["/sandbox/clear" {:post clear-handler}]
 ["/sandbox/uncommit/:name" {:post uncommit-handler}]
 ["/sandbox/copy/:name" {:get copy-handler}]
 ["/sandbox/sidebar/toggle" {:post sidebar-toggle-handler}]]
```

### Handler Pattern

Handlers use dispatch for effects, return twk response maps:

```clojure
(defn commit-handler [{:keys [signals] :as request}]
  (let [{:keys [commitName commitDescription]} signals]
    (dispatch [[::tsain/commit (keyword commitName) commitDescription]])
    {::twk/fx []
     ::twk/with-open-sse? true}))
```

## View Rendering

Views read configuration to resolve paths and namespaces:

```clojure
(defn render-view [state config]
  (let [{:keys [ui-namespace]} config
        {:keys [view library preview]} @state]
    (case (:type view)
      :preview (preview-view preview)
      :components (components-view library ui-namespace)
      :component (component-view library (:name view) ui-namespace)
      (preview-view preview))))
```

The `ui-namespace` is used when rendering the copy button — it shows the full alias keyword.

## CSS Hot Reload

The watcher utility is extracted and configured via `tsain.edn`:

```clojure
(defn start-watcher [dispatch stylesheet-path]
  (when stylesheet-path
    (let [dir (-> stylesheet-path io/file .getParent)]
      (beholder/watch
        (fn [{:keys [path]}]
          (when (str/ends-with? (str path) ".css")
            (dispatch [[::twk/execute-script
                        "document.querySelectorAll('link[rel=stylesheet]').forEach(...)"]
                       [:ascolais.sfere/broadcast {:pattern [:* :*]}]])))
        dir))))
```

Consumer calls this during system start if they want hot reload.

## Testing Strategy

### Library Tests (`test/src/clj/ascolais/tsain_test.clj`)

Existing tests plus:
- Route factory returns valid reitit route data
- Routes have correct paths and methods
- View rendering handles all view types

### Integration Tests

- Consumer setup flow works end-to-end
- Routes serve correct responses
- Discovery API shows all effects

## Migration Path

### Phase 1: Extract Routes

1. Create `src/clj/ascolais/tsain/routes.clj`
2. Move route definitions from `sandbox/app.clj`
3. Parameterize with dispatch and state-atom
4. Update `sandbox/app.clj` to use route factory

### Phase 2: Extract Views

1. Create `src/clj/ascolais/tsain/views.clj`
2. Move view functions from `sandbox/views.clj`
3. Parameterize with config for namespace resolution
4. Update routes to use extracted views

### Phase 3: Package Assets

1. Move `sandbox.css` to `resources/tsain/sandbox.css`
2. Update routes to serve from classpath
3. Document consumer asset serving

### Phase 4: Create Samples

1. Extract ecosystem docs from CLAUDE.md to `sample/CLAUDE.md`
2. Create `sample/tsain.edn` with documented defaults
3. Create `sample/ui.clj` starter template
4. Ensure skill in `.claude/skills/component-iterate/` is generic

### Phase 5: Update Skill

1. Rewrite skill for discovery-first approach
2. Add configuration reading section
3. Document optional enhancements (chrome, frontend-design)
4. Test skill works with fresh project setup

### Phase 6: Documentation

1. Create `docs/setup.md` with integration guide
2. Update README with library overview
3. Update tsain's own CLAUDE.md to reference samples

## Done When

- [ ] `src/clj/ascolais/tsain/routes.clj` exports `routes` function
- [ ] `src/clj/ascolais/tsain/views.clj` exports view rendering functions
- [ ] Routes are parameterized (dispatch + state-atom)
- [ ] Views are parameterized (config for ui-namespace)
- [ ] `resources/tsain/sandbox.css` exists (classpath resource)
- [ ] `sample/CLAUDE.md` contains ecosystem docs + workflow
- [ ] `sample/tsain.edn` documents all configuration keys
- [ ] `sample/ui.clj` provides starter alias template
- [ ] `.claude/skills/component-iterate/SKILL.md` uses discovery-first approach
- [ ] Skill reads `tsain.edn` for file paths
- [ ] Skill documents optional enhancements (chrome, frontend-design)
- [ ] Fresh project can integrate tsain following setup guide
- [ ] Tsain dogfoods its own library exports
- [ ] Tests pass for route factory and view rendering

## Relationship to Other Specs

- **011-discoverable-dev-registry**: Foundation — registry factory and `::tsain/*` effects
- **010-chassis-alias-workflow**: Pattern documented in skill and CLAUDE.md
- **008-css-extraction**: Workflow documented in skill
- **006-dynamic-components**: Examples format used by commit/show
