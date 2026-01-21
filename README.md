# ascolais/tsain

A REPL-driven component sandbox for Clojure + Datastar applications. Design, iterate, and persist hiccup components with live browser preview and hot CSS reload.

## Getting Started

### 1. Add Dependency

```clojure
;; deps.edn
{:deps {io.github.brianium/tsain {:git/tag "v0.1.0" :git/sha "25f129f"}}}
```

### 2. Create Configuration

Create `tsain.edn` at your project root:

```clojure
{:ui-namespace myapp.ui                      ;; Where chassis aliases live
 :components-file "resources/components.edn" ;; Component library persistence
 :stylesheet "dev/resources/public/styles.css" ;; CSS for hot reload
 :port 3000}
```

### 3. Create UI Namespace

Create your UI namespace for chassis aliases:

```clojure
(ns myapp.ui
  (:require [dev.onionpancakes.chassis.core :as c]))

;; Define component structure with aliases
(defmethod c/resolve-alias ::card [_ attrs _]
  (let [{:card/keys [title body]} attrs]
    [:div.card attrs
     [:h2.card-title title]
     [:p.card-body body]]))
```

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
All effects are self-documenting via sandestin:

```clojure
(s/describe dispatch)                    ;; List all effects
(s/describe dispatch ::tsain/preview)    ;; Inspect specific effect
(s/sample dispatch ::tsain/preview)      ;; Generate example invocation
(s/grep dispatch "component")            ;; Search by keyword
```

### CSS Hot Reload
Edit your stylesheet, save → browser updates automatically (requires file watcher).

### Chassis Aliases
Component structure lives in code, configuration in data:

```clojure
;; Structure in myapp/ui.clj
(defmethod c/resolve-alias ::game-card [_ attrs _]
  (let [{:game-card/keys [title cost attack]} attrs]
    [:div.game-card
     [:div.game-card-cost cost]
     [:div.game-card-title title]]))

;; Usage - lean config props
[:myapp.ui/game-card
 {:game-card/title "Neural Phantom"
  :game-card/cost "4"
  :game-card/attack "3"}]
```

Namespaced attributes (`:game-card/title`) are elided from HTML output - they're config for the alias, not HTML attributes.

## Effect Reference

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

## Sample Files

The `sample/` directory contains starter templates:

- `sample/CLAUDE.md` - Ecosystem documentation for Claude Code
- `sample/tsain.edn` - Documented configuration
- `sample/ui.clj` - Starter chassis alias namespace

Copy and adapt these for your project.

## Tech Stack

| Library | Purpose |
|---------|---------|
| [sandestin](https://github.com/brianium/sandestin) | Effect dispatch with schema-driven discoverability |
| [twk](https://github.com/brianium/twk) | Datastar SSE integration |
| [sfere](https://github.com/brianium/sfere) | Connection management and broadcasting |
| [chassis](https://github.com/onionpancakes/chassis) | Hiccup aliases for component structure |
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
src/clj/ascolais/tsain.clj          # Registry factory
src/clj/ascolais/tsain/routes.clj   # Route factory
src/clj/ascolais/tsain/views.clj    # View rendering
resources/tsain/sandbox.css         # Sandbox chrome (classpath)
sample/                             # Starter templates for consumers
dev/src/clj/sandbox/                # Development sandbox (dogfooding)
specs/                              # Living specifications
```

## Specifications

See [specs/README.md](specs/README.md) for living documentation.

## License

Copyright © 2026

Distributed under the Eclipse Public License version 1.0.
