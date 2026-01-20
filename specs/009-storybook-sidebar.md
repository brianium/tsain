# 009: Storybook Sidebar

## Status: Complete

## Overview

Replace the gallery grid view with a storybook-style sidebar navigation. The sidebar lists all components and clicking an entry loads the component view in the main content area. The sidebar is collapsible to maximize component viewing space.

## Goals

1. Collapsible sidebar with component list
2. Click sidebar entry → load single component view
3. Preserve preview area for REPL-driven development
4. Clean, stylish design consistent with existing sandbox chrome
5. Sidebar state persists across view changes

## Design Principles

- **Sidebar + content layout** - Two-column layout with resizable/collapsible sidebar
- **Single source of truth** - Sidebar reflects `library` state, updates via broadcast
- **Deep-linkable** - `/sandbox/c/:name` still works, sidebar highlights current
- **Preview remains primary** - Preview tab is default, sidebar only shows in component browsing mode

## Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│  Nav: [Preview] [Components]                    [Commit] [Clear]         │
├───────────────────────┬──────────────────────────────────────────────────┤
│                       │                                                  │
│  ┌─────────────────┐  │                                                  │
│  │ ≡ Components    │  │         Component View                           │
│  ├─────────────────┤  │                                                  │
│  │ > game-card     │  │   ┌────────────────────────────────────────┐    │
│  │   hero-badge    │  │   │           [component render]           │    │
│  │   stat-block    │  │   │                                        │    │
│  │   user-profile  │  │   └────────────────────────────────────────┘    │
│  │                 │  │                                                  │
│  │                 │  │   [← Prev]  [variant selector]  [Next →]        │
│  │                 │  │                                                  │
│  │                 │  │   [Copy] [Delete]                                │
│  └─────────────────┘  │                                                  │
│        [«]            │                                                  │
└───────────────────────┴──────────────────────────────────────────────────┘
```

When sidebar collapsed:
```
┌──────────────────────────────────────────────────────────────────────────┐
│  Nav: [Preview] [Components]                    [Commit] [Clear]         │
├────┬─────────────────────────────────────────────────────────────────────┤
│    │                                                                     │
│ »  │                    Full-width Component View                        │
│    │                                                                     │
└────┴─────────────────────────────────────────────────────────────────────┘
```

## State Changes

### Server State

Add `sidebar-collapsed?` to state:

```clojure
{:preview {:hiccup [:div "..."]}
 :view {:type :component :name :game-card :example-idx 0}
 :sidebar-collapsed? false
 :library {:game-card {...}
           :hero-badge {...}}}
```

### New Effects

```clojure
::sidebar/toggle
{::s/description "Toggle sidebar collapsed state"
 ::s/schema [:tuple [:= ::sidebar/toggle]]
 ::s/handler (fn [{:keys [dispatch]} _system]
               (swap! state-atom update :sidebar-collapsed? not)
               (dispatch {} {}
                 [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
                   [::twk/patch-signals {:sidebarCollapsed (:sidebar-collapsed? @state-atom)}]]]))}
```

## View Changes

### Remove Gallery View

The `:gallery` view type is replaced by `:components` which shows the sidebar + component layout.

### New View: Components (with sidebar)

```clojure
(defn components-view
  "Render sidebar + component view layout."
  [{:keys [library view sidebar-collapsed?]}]
  (let [current-name (:name view)
        sorted-components (sort-by key library)]
    [:div.components-layout
     {:class (when sidebar-collapsed? "sidebar-collapsed")}

     ;; Sidebar
     [:aside.sidebar
      [:div.sidebar-header
       [:span.sidebar-title "Components"]
       [:button.sidebar-toggle
        {:data-on:click "@post('/sandbox/sidebar/toggle')"}
        (if sidebar-collapsed? "»" "«")]]

      [:nav.sidebar-list
       (for [[component-name _] sorted-components]
         [:a.sidebar-item
          {:key (str component-name)
           :class (when (= component-name current-name) "active")
           :data-on:click (str "@post('/sandbox/view/component/" (name component-name) "')")}
          (name component-name)])]]

     ;; Main content
     [:main.component-main
      (if current-name
        (component-detail {:library library :view view})
        [:div.empty-state "Select a component from the sidebar"])]]))
```

### Component Detail (extracted from component-view)

```clojure
(defn component-detail
  "Render component details (reused from component-view, minus back button)."
  [{:keys [library view]}]
  (let [component-name (:name view)
        component-data (get library component-name)
        {:keys [description examples]} component-data
        example-idx (or (:example-idx view) 0)
        hiccup (get-example-hiccup component-data example-idx)
        {:keys [prev next]} (component-neighbors library component-name)]
    [:div.component-detail
     [:div.component-nav
      (when prev
        [:button.nav-prev {:data-on:click (str "@post('/sandbox/view/component/" (name prev) "')")}
         "← " (name prev)])
      [:div.component-title
       [:h2 (name component-name)]
       (when (and examples (> (count examples) 1))
         [:select.config-selector
          {:id (str "variant-" (name component-name))
           :data-on:change (str "@post('/sandbox/view/component/" (name component-name) "?idx=' + evt.target.value)")}
          (for [[idx {:keys [label]}] (map-indexed vector examples)]
            [:option {:value idx :selected (= idx example-idx)} (or label (str "Example " (inc idx)))])])]
      (when next
        [:button.nav-next {:data-on:click (str "@post('/sandbox/view/component/" (name next) "')")}
         (name next) " →"])]
     (when (seq description)
       [:p.component-desc description])
     [:div.component-render hiccup]
     [:div.component-actions
      [:button.copy-btn
       {:data-on:click (copy-script component-name example-idx)}
       "Copy"]
      [:button {:data-on:click (str "@post('/sandbox/uncommit/" (name component-name) "')")}
       "Delete"]]]))
```

## CSS Changes

### Layout

```css
.components-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  min-height: calc(100vh - 52px); /* nav height */
  transition: grid-template-columns 0.2s ease;
}

.components-layout.sidebar-collapsed {
  grid-template-columns: 48px 1fr;
}
```

### Sidebar

```css
.sidebar {
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-subtle);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem;
  border-bottom: 1px solid var(--border-subtle);
}

.sidebar-title {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.sidebar-collapsed .sidebar-title {
  display: none;
}

.sidebar-toggle {
  background: transparent;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  padding: 0.25rem 0.5rem;
  font-size: 1rem;
}

.sidebar-toggle:hover {
  color: var(--text-primary);
}

.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem;
}

.sidebar-item {
  display: block;
  padding: 0.6rem 0.75rem;
  color: var(--text-secondary);
  text-decoration: none;
  border-radius: var(--radius-sm);
  font-size: 0.9rem;
  cursor: pointer;
  transition: all var(--transition);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-item:hover {
  background: var(--bg-elevated);
  color: var(--text-primary);
}

.sidebar-item.active {
  background: var(--accent-subtle);
  color: var(--accent);
  font-weight: 500;
}

.sidebar-collapsed .sidebar-list {
  display: none;
}
```

### Main content

```css
.component-main {
  padding: 2rem 2.5rem;
  overflow-y: auto;
}

.component-detail {
  max-width: 900px;
}
```

## Routes

### Updated routes

| Route | Handler | View |
|-------|---------|------|
| `/sandbox` | preview-page | Preview (no sidebar) |
| `/sandbox/components` | components-page | Components (sidebar + first component) |
| `/sandbox/c/:name` | component-page | Components (sidebar + named component) |

### New endpoint

| Route | Handler | Purpose |
|-------|---------|---------|
| `/sandbox/sidebar/toggle` | toggle-sidebar | Toggle collapsed state |

## Navigation Behavior

1. **Nav bar**: "Preview" and "Components" tabs
2. **Components tab**: Shows sidebar + component view
3. **First load of `/sandbox/components`**: Selects first component alphabetically
4. **Clicking sidebar item**: Updates view to that component, keeps sidebar visible
5. **Delete component**: If deleted component was selected, select next (or first)

## Implementation Steps

1. Add `sidebar-collapsed?` to state atom and initial-state
2. Add `::sidebar/toggle` effect to registry
3. Create `components-view` and `component-detail` in views.clj
4. Update `render-view` to use new view types
5. Add sidebar toggle endpoint to routes
6. Update nav bar to show "Components" instead of "Gallery"
7. Add CSS for sidebar layout
8. Handle first-component selection on `/sandbox/components`
9. Handle component deletion when current is deleted

## Success Criteria

- [x] Sidebar shows all components alphabetically
- [x] Clicking sidebar item loads component in main area
- [x] Sidebar collapse/expand works with smooth animation
- [x] Collapsed state persists across component switches
- [x] `/sandbox/c/:name` deep links work, highlight correct sidebar item
- [x] Deleting current component selects another
- [x] Preview tab works as before (no sidebar)
- [x] Mobile/narrow viewport: sidebar auto-collapses

## Future Enhancements

1. **Search/filter** - Filter sidebar list by component name
2. **Groups** - Organize components by category
3. **Keyboard navigation** - Arrow keys to navigate sidebar
4. **Drag to resize** - Resize sidebar width
5. **Favorites** - Pin frequently used components to top
