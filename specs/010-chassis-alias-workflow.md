# Spec 010: Chassis Alias Workflow

## Status: Complete

## Problem

When Claude iterates on components, the full verbose hiccup gets committed to `components.edn`. This results in:
- Bloated EDN files with repeated structure (40+ line entries)
- Dark/Light variants duplicating 95% of the same markup
- Less portable assets (copying requires understanding the full structure)
- No reusable component definitions

## Solution

Establish a workflow where:
1. **Component structure** lives in `sandbox/ui.clj` as chassis aliases
2. **Component examples** in `components.edn` use aliases with config props
3. **Copied hiccup** is the lean alias invocation, not expanded markup

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                    sandbox/ui.clj                               │
│  Chassis aliases define STRUCTURE                               │
│  - Namespaced attrs (:game-card/title) = config (elided)        │
│  - Regular attrs (:data-on:click) = pass-through to HTML        │
└─────────────────────────────────────────────────────────────────┘
                               │
                               │ require + use alias
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    components.edn                               │
│  Examples use aliases with variant-specific config              │
│  - {:label "Dark" :hiccup [:sandbox.ui/game-card {:game-card/...│
│  - {:label "Light" :hiccup [:div.theme-light [:sandbox.ui/...]]}│
└─────────────────────────────────────────────────────────────────┘
                               │
                               │ render via chassis
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Browser                                      │
│  Sees fully expanded HTML                                       │
└─────────────────────────────────────────────────────────────────┘
```

## Chassis Alias Conventions

### Namespaced Attributes for Config

Chassis automatically elides namespaced attributes from HTML output. Use them for component configuration. **Convention:** namespace config props by component name (e.g., `:game-card/title`) — the namespace doesn't need to actually exist.

```clojure
;; In sandbox/ui.clj
(defmethod c/resolve-alias :sandbox.ui/game-card
  [_ attrs content]
  (let [{:game-card/keys [title type cost attack defense]} attrs]
    [:div.game-card attrs  ;; namespaced keys auto-elided by chassis
     [:div.game-card-cost cost]
     [:div.game-card-header
      [:div.game-card-title title]
      [:div.game-card-type type]]
     ;; ... rest of structure
     ]))
```

```clojure
;; Usage - component-namespaced keys for config, regular keys pass through
[:sandbox.ui/game-card
 {:game-card/title "Neural Phantom"
  :game-card/type "// Creature — AI Specter"
  :game-card/cost "4"
  :data-signals:selected "false"
  :data-on:click "$selected = !$selected"}]
```

This convention is self-documenting — you can tell at a glance which props belong to which component.

### Structure vs Content

- **Fixed structure** goes in the alias (divs, classes, decorative elements)
- **Variable content** comes from config props or children
- **Datastar attributes** pass through via the attrs map

### Sub-components

Sub-components (like `::game-card-stat`) can be defined for internal use but don't need `components.edn` entries. Only top-level "storybook-worthy" components get library entries.

## Example: Before and After

### Before (Current - Verbose)

```clojure
;; components.edn - 80 lines for two variants
{:game-card
 {:examples
  [{:label "Dark"
    :hiccup [:div {:style "padding: 40px;"}
             [:div.game-card.clip-corners-lg
              [:div.game-card-corner.game-card-corner--top-h]
              [:div.game-card-corner.game-card-corner--top-v]
              ;; ... 35 more lines of structure
              ]]}
   {:label "Light"
    :hiccup [:div.theme-light {:style "padding: 40px;"}
             [:div.game-card.clip-corners-lg
              ;; ... same 35 lines repeated
              ]]}]}}
```

### After (Alias-Based - Lean)

```clojure
;; sandbox/ui.clj - structure defined once
(defmethod c/resolve-alias :sandbox.ui/game-card
  [_ attrs _]
  (let [{:game-card/keys [title type cost attack defense icon flavor ability]} attrs]
    [:div.game-card.clip-corners-lg attrs  ;; namespaced keys auto-elided
     [:div.game-card-corner.game-card-corner--top-h]
     ;; ... full structure with config interpolated
     ]))

;; components.edn - lean examples
{:game-card
 {:description "Cyberpunk game card with attack/defense stats"
  :examples
  [{:label "Dark"
    :hiccup [:div {:style "padding: 40px; display: flex; justify-content: center;"}
             [:sandbox.ui/game-card
              {:game-card/title "Neural Phantom"
               :game-card/type "// Creature — AI Specter"
               :game-card/cost "4"
               :game-card/attack "3"
               :game-card/defense "2"
               :game-card/icon "👻"
               :game-card/ability "Cannot be targeted by ICE programs on the turn it enters."
               :game-card/flavor "\"They call it a ghost in the machine. I call it evolution.\""}]]}
   {:label "Light"
    :hiccup [:div.theme-light {:style "padding: 40px; background: #f0f4f8; display: flex; justify-content: center;"}
             [:sandbox.ui/game-card
              {:game-card/title "Neural Phantom"
               ;; ... same config, different wrapper
               }]]}]}}
```

## Implementation

### 1. Update component-iterate Skill ✓

Added a **required first step**: define the chassis alias before iterating.

### 2. Copy Button Already Copies Alias Form ✓

The copy functionality reads from `components.edn` (the stored alias form) directly via `get-example-hiccup` in `app.clj`.

### 3. Add Namespace Require Pattern ✓

The `sandbox.views` namespace requires `sandbox.ui` to ensure aliases are registered before rendering:

```clojure
(ns sandbox.views
  (:require [ascolais.twk :as twk]
            [dev.onionpancakes.chassis.core :as c]
            [sandbox.ui]))  ;; Ensures aliases are loaded
```

### 4. Update CLAUDE.md ✓

Added chassis alias conventions section documenting:
- Namespaced attrs for config
- When to create sub-aliases vs inline structure
- File locations

## Files Changed

| File | Change |
|------|--------|
| `dev/src/clj/sandbox/ui.clj` | Added aliases for all 6 components |
| `resources/components.edn` | Migrated to alias-based examples |
| `.claude/skills/component-iterate/SKILL.md` | Updated workflow to require alias-first |
| `CLAUDE.md` | Added chassis alias conventions |

## Migration

All existing components in `components.edn` migrated:

| Component | Status |
|-----------|--------|
| `:game-card` | ✓ Complete |
| `:combat-log` | ✓ Complete |
| `:card-type-badges` | ✓ Complete |
| `:player-hud` | ✓ Complete |
| `:action-buttons` | ✓ Complete |
| `:resource-display` | ✓ Complete |

## Done When

- [x] All 6 existing components migrated to alias-based format
- [x] `sandbox/ui.clj` has aliases with `:component-name/prop` conventions
- [x] `components.edn` uses alias invocations (no raw verbose hiccup)
- [x] Copy button copies the alias form (lean snippet)
- [x] component-iterate skill documents alias-first workflow
- [x] CLAUDE.md has chassis alias conventions
- [x] Sandbox renders all components correctly after migration

## Future: sandbox.edn Configuration

Note for later: Add `sandbox.edn` for project-level configuration:

```clojure
{:ui-namespace sandbox.ui          ;; Where aliases live
 :components-file "resources/components.edn"
 :stylesheet "dev/resources/public/styles.css"}
```

This allows different projects to customize the sandbox setup.
