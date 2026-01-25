# Sandbox UI Enhancements - Research

## Problem Statement

Components defined with `html.yeah/defelem` expose rich metadata: descriptions, malli schemas, categories, and examples. The current sandbox UI doesn't surface this metadata effectively. Users can't easily:
- Find components by category
- See what props a component accepts
- Copy the hiccup source easily
- Understand what a component does at a glance

## Requirements

### Functional Requirements

1. Sidebar must group components by category with collapsible sections
2. Component view must have tabs: Preview (default), Code, Props
3. Code tab must show hiccup source with copy button
4. Props tab must show prop name, type, and required/optional
5. Component description must be visible in the header area
6. Commit form must allow selecting existing category or creating new one
7. Sidebar collapse state must persist across page loads

### Non-Functional Requirements

- Performance: No noticeable lag when switching tabs or categories
- Simplicity: All state management via Datastar signals
- Consistency: Match existing cyberpunk aesthetic

## Component View Layout

```
┌─────────────────────────────────────────────────────────────┐
│  < prev-component   component-name  [Example ▼]   next >    │
│                   Component description here...             │
├─────────────────────────────────────────────────────────────┤
│  [Preview]  [Code]  [Props]                                 │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    Tab content area                         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

## Tab Content

### Preview Tab
- The rendered component (current behavior)
- Clean, focused view

### Code Tab
- Hiccup source formatted for readability
- Copy button (moved from current location)
- Delete button (moved from current location)

### Props Tab
- Table with columns: Name, Type, Required
- Derived from malli schema via `m/children`, `m/properties`, `m/type`
- Enums show possible values: `:primary | :secondary`
- Complex types show simplified: "map", "vector", "complex"

## Sidebar Category Grouping

### Data Source

```clojure
;; Existing discovery API
(tsain/categories)        ;; => ("cards" "controls" "display")
(tsain/by-category "cards") ;; => [{:tag :sandbox.ui/game-card ...}]
```

### Structure

```
┌──────────────────┐
│ ▼ Cards          │  ← collapsible header
│   game-card      │
│   info-card      │
│ ▶ Controls       │  ← collapsed
│ ▼ Display        │
│   badge          │
│   avatar         │
│ ▼ Other          │  ← uncategorized components
│   misc-thing     │
└──────────────────┘
```

### Collapse State Persistence

Without `data-persist` (Datastar Pro), use expressions:

```html
<!-- Initialize from localStorage on load -->
<div data-on-load="$sidebarState = JSON.parse(localStorage.getItem('tsain-sidebar') || '{}')">

<!-- Persist on any change -->
<div data-effect="localStorage.setItem('tsain-sidebar', JSON.stringify($sidebarState))">

<!-- Toggle category collapse -->
<button data-on:click="$sidebarState.cards = !$sidebarState.cards">
  Cards
</button>
<div data-show="$sidebarState.cards">
  <!-- category items -->
</div>
```

## Commit Form Category Selection

### UI

```
┌─────────────────────────────────────────┐
│ Component Name: [my-component        ]  │
│ Description:    [A cool component    ]  │
│ Category:       [Cards           ▼]     │  ← select with existing categories
│                 ├─ Cards               │
│                 ├─ Controls            │
│                 ├─ Display             │
│                 └─ + New category...   │  ← reveals text input
└─────────────────────────────────────────┘
```

### Interaction

1. Select from existing categories (common case)
2. Choose "+ New category..." to reveal text input
3. New category created on commit

## Props Extraction from Malli

```clojure
(require '[malli.core :as m])

(defn extract-props [schema]
  (when (= :map (m/type schema))
    (for [[key props child] (m/children schema)]
      {:name key
       :type (format-type child)
       :required (not (:optional props))})))

(defn format-type [schema]
  (case (m/type schema)
    :string "string"
    :int "int"
    :keyword "keyword"
    :boolean "boolean"
    :enum (str/join " | " (map pr-str (m/children schema)))
    :map "map"
    :vector "vector"
    ;; default
    (pr-str (m/form schema))))
```

## Open Questions

- [x] Eager vs lazy tab rendering? → Eager (negligible cost)
- [x] Props detail level? → Name, type, required; enums show values
- [x] Sidebar persistence? → localStorage via Datastar expressions
- [x] Category editing? → Separate spec

## References

- Current sandbox views: `dev/src/clj/sandbox/views.clj`
- Discovery API: `src/clj/ascolais/tsain.clj`
- html.yeah metadata: `hy/element`, `hy/search-elements`
- Malli introspection: `m/children`, `m/properties`, `m/type`
