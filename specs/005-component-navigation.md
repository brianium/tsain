# 005: Component Navigation

## Status: Complete

## Overview

Add "previous" and "next" navigation buttons to the single component view (`/sandbox/c/:name`). When viewing a component in isolation, users should be able to quickly navigate through the library without returning to the gallery.

## Goals

1. Previous/Next buttons on single component view
2. Wrap-around navigation (last → first, first → last)
3. Keyboard shortcuts for navigation
4. Consistent ordering (alphabetical by name)

## Design

### Navigation Flow

```
┌─────────────────────────────────────────────────────────────┐
│  Component: button                                          │
│  ← Previous (alert)          Next (card) →                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│              [Rendered Component]                           │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  ← Back to Gallery                                          │
└─────────────────────────────────────────────────────────────┘
```

When viewing the only component, both buttons are disabled or hidden.

### Ordering

Components are ordered alphabetically by keyword name:
- `:alert` → `:button` → `:card` → `:dialog`

This matches the gallery grid order for consistency.

## Implementation

### 1. Update sandbox/views.clj

Add navigation helper and update `component-view`:

```clojure
(defn component-neighbors
  "Get previous and next component names for navigation."
  [library current-name]
  (let [sorted-names (sort (keys library))
        idx (.indexOf (vec sorted-names) current-name)
        total (count sorted-names)]
    (when (pos? total)
      {:prev (when (> total 1)
               (nth sorted-names (mod (dec idx) total)))
       :next (when (> total 1)
               (nth sorted-names (mod (inc idx) total)))})))

(defn component-view
  "Render single component view with navigation."
  [{:keys [library view]}]
  (let [name (:name view)
        {:keys [hiccup description]} (get library name)
        {:keys [prev next]} (component-neighbors library name)]
    [:div.component-view
     [:div.component-header
      [:div.component-nav
       (when prev
         [:button.nav-prev
          {:data-on-click (str "@post('/sandbox/view/component/" (clojure.core/name prev) "')")}
          "← " (clojure.core/name prev)])
       [:h2 (clojure.core/name name)]
       (when next
         [:button.nav-next
          {:data-on-click (str "@post('/sandbox/view/component/" (clojure.core/name next) "')")}
          (clojure.core/name next) " →"])]
      (when (seq description)
        [:p description])]
     [:div.component-render
      (if hiccup
        hiccup
        [:p.empty-state "Component not found"])]
     [:button {:data-on-click "@post('/sandbox/view/gallery')"}
      "← Back to Gallery"]]))
```

### 2. Add CSS for Navigation

Add to `dev-only-styles`:

```css
.component-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1rem;
}
.component-nav h2 {
  margin: 0;
  flex: 1;
  text-align: center;
}
.nav-prev, .nav-next {
  padding: 0.5rem 1rem;
  cursor: pointer;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  min-width: 120px;
}
.nav-prev:hover, .nav-next:hover {
  background: #e5e5e5;
}
.nav-prev {
  text-align: left;
}
.nav-next {
  text-align: right;
}
```

### 3. Keyboard Navigation (Optional Enhancement)

Add to the sandbox page shell or component view:

```html
<div data-on-keydown__window="
  if (event.key === 'ArrowLeft') @post('/sandbox/nav/prev');
  if (event.key === 'ArrowRight') @post('/sandbox/nav/next');
">
```

With corresponding routes/effects for `/sandbox/nav/prev` and `/sandbox/nav/next` that navigate relative to current component.

## Routes

No new routes required for basic implementation. The existing `/sandbox/view/component/:name` route handles navigation.

For keyboard shortcuts, add:
- `POST /sandbox/nav/prev` - Navigate to previous component
- `POST /sandbox/nav/next` - Navigate to next component

## REPL API

Optional additions to dev namespace:

```clojure
(defn show-next!
  "Navigate to next component in library."
  []
  (let [state @(:state system/*system*)
        current (get-in state [:view :name])
        {:keys [next]} (views/component-neighbors (:library state) current)]
    (when next
      (dispatch [[::sandbox/show next]]))))

(defn show-prev!
  "Navigate to previous component in library."
  []
  (let [state @(:state system/*system*)
        current (get-in state [:view :name])
        {:keys [prev]} (views/component-neighbors (:library state) current)]
    (when prev
      (dispatch [[::sandbox/show prev]]))))
```

## Success Criteria

- [x] Previous/Next buttons appear on single component view
- [x] Clicking Previous navigates to alphabetically earlier component
- [x] Clicking Next navigates to alphabetically later component
- [x] Navigation wraps around (last → first, first → last)
- [x] Buttons show destination component name
- [x] Single-component library shows no navigation buttons
- [x] Navigation broadcasts to all connected clients
- [ ] (Optional) Arrow keys navigate when viewing component

## Relationship to Other Specs

- **004-component-library**: Extends single component view with navigation
- Uses same sfere broadcast pattern for multi-client sync
