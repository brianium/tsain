# ascolais/tsain

A REPL-driven component sandbox for Clojure + Datastar applications. Design, iterate, and persist hiccup components with live browser preview and hot CSS reload.

## Getting Started

### 1. Add Dependency

```clojure
;; deps.edn
{:deps {io.github.brianium/tsain {:git/tag "v0.6.1" :git/sha "0c3da51"}}}
```

### 2. Create Configuration

Create `tsain.edn` at your project root:

```clojure
{:ui-namespace myapp.ui                        ;; Where defelem components live
 :database-file "tsain.db"                     ;; Component library (SQLite)
 :stylesheet "dev/resources/public/styles.css" ;; CSS for hot reload
 :source-paths ["src/clj" "dev/src/clj"]       ;; For finding namespace files
 :split-threshold 1500                         ;; LOC threshold for split hints
 :port 3000}
```

Legacy EDN storage is also supported:
```clojure
{:components-file "resources/components.edn"} ;; Instead of :database-file
```

### 3. Create UI Namespace

Create your UI namespace for components using html.yeah:

```clojure
(ns myapp.ui
  (:require [html.yeah :as hy]))

;; Define components with schema-driven defelem
(hy/defelem card
  [:map {:doc "A simple card component"
         :keys [card/title card/body]}
   [:card/title :string]
   [:card/body :string]]
  [:div.card
   [:h2.card-title card/title]
   [:p.card-body card/body]
   (hy/children)])
```

Components defined with `defelem` are discoverable via `tsain/describe` and `tsain/grep`.

### 4. Wire Up the System

```clojure
(ns dev
  (:require [ascolais.sandestin :as s]
            [ascolais.tsain :as tsain]
            [ascolais.tsain.routes :as tsain.routes]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [reitit.ring :as rr]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [starfederation.datastar.clojure.adapter.http-kit :as ds-hk]
            [org.httpkit.server :as hk]
            myapp.ui))  ;; Require to register aliases

(defn start []
  (let [;; Create tsain registry (reads tsain.edn)
        tsain-reg  (tsain/registry)
        state-atom (::tsain/state tsain-reg)
        config     (::tsain/config tsain-reg)

        ;; Create connection store and dispatch
        store    (sfere/store {:type :caffeine :duration-ms 30000})
        dispatch (s/create-dispatch [(twk/registry)
                                     (sfere/registry store)
                                     tsain-reg])

        ;; Create router with tsain routes
        router (rr/router
                (tsain.routes/routes dispatch state-atom config)
                {:data {:middleware [(fn [h] (fn [r] (h (assoc r :dispatch dispatch))))
                                     (twk/with-datastar ds-hk/->sse-response dispatch)]}})

        ;; Create app
        app (-> (rr/ring-handler router (rr/create-default-handler))
                wrap-params
                (wrap-resource "public"))

        ;; Start server
        server (hk/run-server app {:port (:port config)})]

    (println (str "Sandbox at http://localhost:" (:port config) "/sandbox"))
    {:dispatch dispatch :server server}))
```

### 5. Start Iterating

```clojure
(def system (start))

;; Preview a component
((:dispatch system) [[::tsain/preview
                      [:myapp.ui/card {:card/title "Hello" :card/body "World"}]]])

;; Discover available effects
(s/describe (:dispatch system))
```

Open `http://localhost:3000/sandbox` in your browser to see live updates.

## Features

### Live Preview
Push hiccup from REPL → instant browser update across all connected devices via SSE.

```clojure
(dispatch [[::tsain/preview [:div.card [:h2 "Title"] [:p "Content"]]]])
```

### Component Library
Commit, browse, and copy components with a storybook-style sidebar.

```clojure
;; Commit with dark/light variants
(dispatch [[::tsain/commit :my-card
            {:description "Card component"
             :examples [{:label "Dark" :hiccup [:myapp.ui/card {...}]}
                        {:label "Light" :hiccup [:div.theme-light [:myapp.ui/card {...}]]}]}]])

;; Browse library
(dispatch [[::tsain/show-components :my-card]])
```

### Discoverable API

Tsain provides component discovery functions:

```clojure
(tsain/describe)                         ;; List all components with schemas
(tsain/describe :myapp.ui/card)          ;; Get details for specific component
(tsain/grep "button")                    ;; Search by keyword
(tsain/props :variant)                   ;; Find components with specific prop
```

Effects are also discoverable via sandestin:

```clojure
(s/describe dispatch)                    ;; List all effects
(s/describe dispatch ::tsain/preview)    ;; Inspect specific effect
(s/sample dispatch ::tsain/preview)      ;; Generate example invocation
```

### CSS Hot Reload
Edit your stylesheet, save → browser updates automatically (requires file watcher).

### Schema-Driven Components

Use html.yeah's `defelem` for discoverable, schema-validated components:

```clojure
(hy/defelem game-card
  [:map {:doc "Cyberpunk-styled game card"
         :keys [game-card/title game-card/cost game-card/attack]}
   [:game-card/title :string]
   [:game-card/cost :string]
   [:game-card/attack {:optional true} :string]]
  [:div.game-card
   [:div.game-card-cost game-card/cost]
   [:div.game-card-title game-card/title]
   (when game-card/attack
     [:div.game-card-attack game-card/attack])])

;; Usage - lean config props
[:myapp.ui/game-card
 {:game-card/title "Neural Phantom"
  :game-card/cost "4"
  :game-card/attack "3"}]
```

Namespaced attributes (`:game-card/title`) are elided from HTML output - they're config for the component, not HTML attributes.

### Effect-Based Authoring

Write components and CSS through effects that track line counts and return split hints:

```clojure
;; Write a component - auto-formats with cljfmt if available
(dispatch [[::tsain/write-component
            "(hy/defelem my-button
               [:map {:doc \"Action button\"}
                [:my-button/label :string]]
               [:button.btn my-button/label])"]])
;; => {:hints [...] :loc 45}

;; Write CSS - auto-formats with prettier
(dispatch [[::tsain/write-css
            ".btn { background: var(--accent-cyan); }"
            {:category "controls"}]])
```

When files exceed `:split-threshold`, results include actionable hints:

```clojure
;; Result includes hint when threshold exceeded
{:hints [{:type :split-suggested
          :category "controls"
          :action [::tsain/split-namespace "controls"]}]}

;; Act on hint to extract category to sub-namespace
(dispatch [[::tsain/split-namespace "controls"]])
;; Creates myapp.ui.controls, adds require to myapp.ui
```

## Effect Reference

### Preview & Library

| Effect | Purpose |
|--------|---------|
| `[::tsain/preview hiccup]` | Replace preview with content |
| `[::tsain/preview-append hiccup]` | Append to preview |
| `[::tsain/preview-clear]` | Clear preview |
| `[::tsain/commit :name opts]` | Save to library |
| `[::tsain/uncommit :name]` | Remove from library |
| `[::tsain/show-components :name]` | View component with sidebar |
| `[::tsain/show-preview]` | Return to preview view |
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

## Sample Files

The `sample/` directory contains starter templates:

- `sample/CLAUDE.md` - Ecosystem documentation for Claude Code
- `sample/tsain.edn` - Documented configuration
- `sample/ui.clj` - Starter chassis alias namespace

Copy and adapt these for your project.

## Claude Code Skill

Tsain includes a skill for REPL-driven component development workflows:

- `/tsain iterate` - Direct component iteration workflow
- `/tsain implement` - Spec-driven implementation workflow

Install by copying from this repo:

```bash
cp -r path/to/tsain/.claude/skills/tsain .claude/skills/
```

## Tech Stack

| Library | Purpose |
|---------|---------|
| [sandestin](https://github.com/brianium/sandestin) | Effect dispatch with schema-driven discoverability |
| [twk](https://github.com/brianium/twk) | Datastar SSE integration |
| [sfere](https://github.com/brianium/sfere) | Connection management and broadcasting |
| [html.yeah](https://github.com/brianium/html.yeah) | Schema-driven components with malli |
| [chassis](https://github.com/onionpancakes/chassis) | Hiccup aliases (html.yeah compiles to these) |
| [phandaal](https://github.com/brianium/phandaal) | File I/O with LOC tracking and formatters |
| [Datastar](https://data-star.dev/) | Frontend reactivity via HTML attributes |

## Development (Contributing to tsain)

### Prerequisites

- [Clojure CLI](https://clojure.org/guides/install_clojure) 1.11+
- [Babashka](https://github.com/babashka/babashka) v1.12.212+
- [bbin](https://github.com/babashka/bbin)

### Setup

```bash
# Install clojure-mcp-light tools (for Claude Code)
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.1
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.1 \
  --as clj-nrepl-eval --main-opts '["-m" "clojure-mcp-light.nrepl-eval"]'
```

### Running

```bash
clj -M:dev
```

```clojure
(dev)       ;; Switch to dev namespace
(start)     ;; Start sandbox at localhost:3000
(reload)    ;; Reload after code changes
```

### Testing

```bash
clj -X:test
```

### Project Structure

```
tsain.edn                           # Configuration
src/clj/ascolais/tsain.clj          # Registry factory with effects
src/clj/ascolais/tsain/clj.clj      # Clojure utilities (cljfmt, namespace ops)
src/clj/ascolais/tsain/css.clj      # CSS utilities (prettier, split ops)
src/clj/ascolais/tsain/routes.clj   # Route factory
src/clj/ascolais/tsain/views.clj    # View rendering
resources/tsain/sandbox.css         # Sandbox chrome (classpath)
sample/                             # Starter templates for consumers
.claude/skills/tsain/               # Claude Code skill for component workflow
dev/src/clj/sandbox/                # Development sandbox (dogfooding)
specs/                              # Living specifications
```

## Specifications

See [specs/README.md](specs/README.md) for living documentation.

## License

Copyright © 2026

Distributed under the Eclipse Public License version 1.0.
