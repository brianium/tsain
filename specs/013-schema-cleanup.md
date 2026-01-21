# Spec 013: Schema Cleanup

## Status: Draft

## Problem

Tsain has two issues with its current schema and namespace design:

1. **State key namespace pollution** - The registry uses `::s/state` (`:ascolais.sandestin/state`) but this is tsain-specific state, not sandestin framework state. It should be `::state` (`:ascolais.tsain/state`) to match `::config`.

2. **Duplicate schema definitions** - Tsain defines its own `HiccupSchema` and `SignalMapSchema` inline, but `ascolais.twk.schema` already provides canonical, well-structured schemas:
   - `ascolais.twk.schema/Hiccup` - Recursive schema with proper registry-based definition
   - `ascolais.twk.schema/Signals` - Clean `[:map-of keyword? any?]`

The current inline schemas are ad-hoc workarounds that don't leverage the ecosystem's existing type definitions.

## Solution

### 1. Use Tsain Namespace for State

Change the registry to use `::state` instead of `::s/state`:

```clojure
;; Before
{::s/state state-atom
 ::config config
 ::s/effects {...}}

;; After
{::state state-atom
 ::config config
 ::s/effects {...}}
```

This keeps tsain's state in tsain's namespace while still being compatible with sandestin's registry composition.

### 2. Use TWK Schemas

Import and use the canonical schemas from twk:

```clojure
(require '[ascolais.twk.schema :as twk.schema])

;; Use twk's Hiccup with generation hints
(def HiccupSchema
  (mu/update-properties
   twk.schema/Hiccup
   assoc :gen/elements [[:div [:h1 "Hello World"]]
                        [:div.card [:h2 "Title"] [:p "Content"]]
                        [:button {:data-on:click "@post('/action')"} "Click me"]]))

;; Use twk's Signals with generation hints
(def SignalMapSchema
  (mu/update-properties
   twk.schema/Signals
   assoc :gen/elements [{:count 0}
                        {:selected true}
                        {:form {:email "test@example.com"}}]))
```

This:
- Reuses the canonical type definitions from twk
- Preserves the `:gen/elements` for `s/sample` functionality
- Ensures schema consistency across the ecosystem

## Implementation

### Phase 1: Update tsain.clj

1. Add require for `ascolais.twk.schema`
2. Add require for `malli.util` (for `mu/update-properties`)
3. Replace inline `HiccupSchema` with twk schema + gen hints
4. Replace inline `SignalMapSchema` with twk schema + gen hints
5. Change `::s/state` to `::state`

### Phase 2: Update Consumers

1. Update `sandbox/app.clj` to use `::tsain/state` instead of `::s/state`
2. Update `tsain.routes` to use `::tsain/state`
3. Update README examples

### Phase 3: Update Tests

1. Update tests to use `::tsain/state`
2. Verify schema validation still works
3. Verify `s/sample` still generates useful examples

## Done When

- [ ] `::state` used instead of `::s/state` in tsain registry
- [ ] `HiccupSchema` uses `ascolais.twk.schema/Hiccup` as base
- [ ] `SignalMapSchema` uses `ascolais.twk.schema/Signals` as base
- [ ] Generation hints preserved for `s/sample` functionality
- [ ] `sandbox/app.clj` updated to use `::tsain/state`
- [ ] `tsain.routes` updated to use `::tsain/state`
- [ ] README updated with correct namespace
- [ ] All tests pass

## Relationship to Other Specs

- **012-portable-library**: Consumers need correct namespace for state access
- **011-discoverable-dev-registry**: Schema quality affects discovery experience
