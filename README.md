# ascolais/tsain

A REPL-driven component sandbox for Clojure + Datastar applications. Design, iterate, and persist hiccup components with live browser preview and hot CSS reload.

## Features

### Component Sandbox
- **Live Preview**: Push hiccup from REPL → instant browser update
- **Multi-device Sync**: All connected browsers/devices update simultaneously via SSE
- **CSS Hot Reload**: Edit `styles.css`, save → browser updates automatically
- **Component Library**: Commit, browse, and copy components with storybook-style sidebar

### REPL-Driven Workflow
```clojure
(dev)
(start)  ;; Sandbox at localhost:3000

;; Iterate on components
(dispatch [[::tsain/preview [:div.card [:h2 "Title"] [:p "Content"]]]])

;; Test Datastar signals
(dispatch [[::tsain/patch-signals {:count 42}]])

;; Commit to library
(dispatch [[::tsain/commit :my-card {:description "Card component"}]])

;; Browse library
(dispatch [[::tsain/show-components :my-card]])
```

### Discoverable API
All sandbox effects are discoverable via sandestin:
```clojure
(describe dispatch)                    ;; List all effects
(describe dispatch ::tsain/preview)    ;; Inspect specific effect
(sample dispatch ::tsain/preview)      ;; Generate example invocation
(grep dispatch "component")            ;; Search by keyword
```

### Chassis Aliases
Component structure lives in code, configuration in data:
```clojure
;; Structure in sandbox/ui.clj
(defmethod c/resolve-alias ::game-card [_ attrs _]
  (let [{:game-card/keys [title cost attack]} attrs]
    [:div.game-card
     [:div.game-card-cost cost]
     [:div.game-card-title title]
     ...]))

;; Config in components.edn
[:sandbox.ui/game-card
 {:game-card/title "Neural Phantom"
  :game-card/cost "4"
  :game-card/attack "3"}]
```

## Component Library

| Component | Description |
|-----------|-------------|
| `game-card` | Cyberpunk game card with attack/defense stats |
| `combat-log` | Battle log with timestamped events |
| `player-hud` | Player HUD with health/energy bars |
| `player-portrait` | 16-bit pixel art character portrait |
| `action-buttons` | Primary/secondary/small button variants |
| `resource-display` | Energy orbs and credit chips |
| `card-type-badges` | Badge and rarity indicators |
| `toast` | System alert notifications |

## Prerequisites

- [Clojure CLI](https://clojure.org/guides/install_clojure) 1.11+
- [Babashka](https://github.com/babashka/babashka) v1.12.212+
- [bbin](https://github.com/babashka/bbin)

## Setup

### 1. Install clojure-mcp-light tools (for Claude Code)

```bash
# Paren repair hook
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.1

# nREPL evaluation CLI
bbin install https://github.com/bhauman/clojure-mcp-light.git --tag v0.2.1 \
  --as clj-nrepl-eval --main-opts '["-m" "clojure-mcp-light.nrepl-eval"]'
```

### 2. Start Development

```bash
clj -M:dev
```

```clojure
(dev)       ;; Switch to dev namespace
(start)     ;; Start sandbox at localhost:3000
(reload)    ;; Reload after code changes
(restart)   ;; Full restart
```

### 3. Open Sandbox

Navigate to http://localhost:3000/sandbox

## Project Structure

```
tsain.edn                           # Configuration
src/clj/ascolais/tsain.clj          # Registry factory with effects
dev/src/clj/
├── dev.clj                         # REPL entry point
├── sandbox/
│   ├── app.clj                     # HTTP routes and handlers
│   ├── views.clj                   # Hiccup view templates
│   └── ui.clj                      # Chassis aliases
dev/resources/public/
├── sandbox.css                     # Sandbox chrome styles
└── styles.css                      # Component styles
resources/components.edn            # Persisted component library
specs/                              # Living specification documents
```

## Tech Stack

| Library | Purpose |
|---------|---------|
| [sandestin](https://github.com/brianium/sandestin) | Effect dispatch with schema-driven discoverability |
| [twk](https://github.com/brianium/twk) | Datastar SSE integration |
| [sfere](https://github.com/brianium/sfere) | Connection management and broadcasting |
| [chassis](https://github.com/onionpancakes/chassis) | Hiccup aliases for component structure |
| [Datastar](https://data-star.dev/) | Frontend reactivity via HTML attributes |

## Testing

```bash
clj -X:test
```

## Specifications

See [specs/README.md](specs/README.md) for living documentation of all implemented features.

## License

Copyright © 2026

Distributed under the Eclipse Public License version 1.0.
