# 006: Dynamic Component Iteration

## Status: Complete

## Overview

Extend the component iteration workflow to support **stateful Datastar components**. Keep it simple: Chassis aliases define structure, Datastar attributes are just HTML attributes passed explicitly, and the sandbox UI provides controls to toggle between different attribute configurations.

**Primary goal**: Enable Claude to autonomously create, iterate on, and test dynamic Datastar-ready components with minimal friction.

## Design Principles

1. **Aliases = structure + styling** - Chassis aliases define what a component looks like, not how it behaves
2. **Datastar attributes = just HTML attributes** - No magic, no special handling, explicit at the usage site
3. **Multiple configs per component** - Store several example configurations showing static vs. interactive variants
4. **Sandbox UI controls** - Toggle between configs in the browser to see different behaviors
5. **REPL-driven testing** - `patch-signals!` lets Claude test interactivity without clicking

## Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                     sandbox/ui.clj                                   │
│  Chassis aliases = pure structure + styling                          │
│                                                                      │
│  (defmethod c/resolve-alias ::counter                                │
│    [_ attrs _]                                                       │
│    [:div.counter attrs                                               │
│     [:button.counter-btn "−"]                                        │
│     [:span.counter-value "0"]                                        │
│     [:button.counter-btn "+"]])                                      │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                   resources/components.edn                           │
│  Multiple example configs per component                              │
│                                                                      │
│  {:counter                                                           │
│   {:alias :sandbox.ui/counter                                        │
│    :description "Counter with +/- buttons"                           │
│    :examples                                                         │
│    [{:label "Static"                                                 │
│      :hiccup [:sandbox.ui/counter]}                                  │
│     {:label "Interactive"                                            │
│      :hiccup [:sandbox.ui/counter                                    │
│               {:data-signals:count "0"}                              │
│               [:button {:data-on:click "$count--"} "−"]              │
│               [:span {:data-text "$count"}]                          │
│               [:button {:data-on:click "$count++"} "+"]]}]}}         │
└───────────────────────────────┬──────────────────────────────────────┘
                                │
                                ▼
┌──────────────────────────────────────────────────────────────────────┐
│                      Sandbox Browser UI                              │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  [Static ▼]  ← Dropdown to switch between example configs      │  │
│  ├────────────────────────────────────────────────────────────────┤  │
│  │                                                                │  │
│  │              Rendered Component                                │  │
│  │                                                                │  │
│  └────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
```

## Chassis Alias Pattern

Aliases accept `attrs` and pass them through to the root element. They define structure and CSS classes only:

```clojure
(ns sandbox.ui
  (:require [dev.onionpancakes.chassis.core :as c]))

(defmethod c/resolve-alias ::counter
  [_ attrs _content]
  [:div.counter attrs
   [:button.counter-btn "−"]
   [:span.counter-value "0"]
   [:button.counter-btn "+"]])

(defmethod c/resolve-alias ::card
  [_ attrs content]
  [:div.card attrs
   content])

(defmethod c/resolve-alias ::toggle
  [_ attrs content]
  [:button.toggle attrs
   content])
```

## Example Configurations

Each component in `components.edn` has multiple `:examples` showing different attribute configs:

```clojure
{:counter
 {:alias :sandbox.ui/counter
  :description "Simple counter with increment/decrement"
  :examples
  [{:label "Static"
    :hiccup [:sandbox.ui/counter]}

   {:label "Interactive"
    :hiccup [:div.counter {:data-signals:count "0"}
             [:button.counter-btn {:data-on:click "$count--"} "−"]
             [:span.counter-value {:data-text "$count"}]
             [:button.counter-btn {:data-on:click "$count++"} "+"]]}

   {:label "Starting at 100"
    :hiccup [:div.counter {:data-signals:count "100"}
             [:button.counter-btn {:data-on:click "$count--"} "−"]
             [:span.counter-value {:data-text "$count"}]
             [:button.counter-btn {:data-on:click "$count++"} "+"]]}]}

 :toggle
 {:alias :sandbox.ui/toggle
  :description "Boolean toggle button"
  :examples
  [{:label "Static"
    :hiccup [:sandbox.ui/toggle "Dark Mode"]}

   {:label "Interactive"
    :hiccup [:button.toggle
             {:data-signals:on "false"
              :data-class:active "$on"
              :data-on:click "$on = !$on"}
             "Dark Mode"]}]}}
```

Note: Interactive examples may use raw hiccup instead of aliases when Datastar attributes need to be on nested elements.

## Sandbox UI Changes

### Config Selector

Add a dropdown to the component view that lets you switch between examples:

```clojure
(defn component-view
  [{:keys [library view]}]
  (let [name (:name view)
        {:keys [examples description]} (get library name)
        selected-idx (or (:example-idx view) 0)
        selected (nth examples selected-idx nil)]
    [:div.component-view
     [:div.component-header
      [:h2 (clojure.core/name name)]
      (when (> (count examples) 1)
        [:select.config-selector
         {:data-on:change "@post('/sandbox/view/component/" (name name) "?idx=' + this.value)"}
         (for [[idx {:keys [label]}] (map-indexed vector examples)]
           [:option {:value idx :selected (= idx selected-idx)} label])])]
     [:div.component-render
      (:hiccup selected)]]))
```

### Gallery View

Gallery shows the first example of each component as the thumbnail.

## REPL Workflow

### Iterating on a New Component

```bash
# 1. Start with structure
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div.accordion
   [:button.accordion-header \"Section Title\"]
   [:div.accordion-content \"Hidden content\"]])"

# 2. Add CSS to styles.css (hot-reloads)

# 3. Add Datastar attributes for interactivity
clj-nrepl-eval -p 7888 "(dev/preview!
  [:div.accordion {:data-signals:open \"false\"}
   [:button.accordion-header {:data-on:click \"\\$open = !\\$open\"}
    \"Section Title\"]
   [:div.accordion-content {:data-show \"\\$open\"}
    \"Hidden content\"]])"

# 4. Test by patching signals from REPL
clj-nrepl-eval -p 7888 "(dev/patch-signals! {:open true})"
```

### Committing with Multiple Examples

```bash
clj-nrepl-eval -p 7888 "(dev/commit! :accordion
  {:description \"Expandable content section\"
   :examples
   [{:label \"Static\"
     :hiccup [:div.accordion
              [:button.accordion-header \"Title\"]
              [:div.accordion-content \"Content\"]]}
    {:label \"Interactive\"
     :hiccup [:div.accordion {:data-signals:open \"false\"}
              [:button.accordion-header {:data-on:click \"$open = !$open\"} \"Title\"]
              [:div.accordion-content {:data-show \"$open\"} \"Content\"]]}]})"
```

## New REPL Helper: patch-signals!

```clojure
;; In sandbox/registry.clj
::patch-signals
{::s/description "Patch Datastar signals and broadcast to all clients"
 ::s/schema [:tuple [:= ::patch-signals] :map]
 ::s/handler
 (fn [{:keys [dispatch]} _system signals]
   (dispatch {} {}
     [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
       [::twk/patch-signals signals]]]))}

;; In dev.clj
(defn patch-signals!
  "Patch Datastar signals on all connected browsers.

   Usage:
     (patch-signals! {:count 42})
     (patch-signals! {:open true})
     (patch-signals! {:form {:email \"test@example.com\"}})"
  [signals]
  (dispatch [[::registry/patch-signals signals]]))
```

## Updated commit! Signature

```clojure
(defn commit!
  "Commit component to the library.

   Simple (current preview as single example):
     (commit! :my-card \"Card component\")

   With multiple examples:
     (commit! :my-card
       {:description \"Card component\"
        :examples [{:label \"Basic\" :hiccup [...]}
                   {:label \"With image\" :hiccup [...]}]})"
  ([component-name]
   (commit! component-name nil))
  ([component-name opts]
   (dispatch [[::registry/commit component-name opts]])))
```

## Success Criteria

- [x] Chassis aliases in `sandbox/ui.clj` accept and pass through `attrs`
- [x] `components.edn` supports `:examples` vector with `:label` and `:hiccup`
- [x] Sandbox UI shows config dropdown when component has multiple examples
- [x] `patch-signals!` broadcasts signal patches to all clients
- [x] `commit!` accepts `:examples` in opts
- [x] Component-iterate skill updated with this workflow

## File Changes

```
dev/src/clj/sandbox/
├── ui.clj              # NEW: Chassis alias definitions (structure only)
├── registry.clj        # Add ::patch-signals effect
├── views.clj           # Add config selector to component view
└── state.clj           # Track selected example index in view state

resources/
├── components.edn      # Updated schema with :examples

.claude/skills/
├── component-iterate/
│   └── SKILL.md        # Updated workflow documentation
```

## Relationship to Other Specs

- **004-component-library**: Extends with `:examples` support
- **003-css-hot-reload**: Unchanged
- **002-dev-sandbox**: Foundation for preview/broadcast
