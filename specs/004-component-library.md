# 004: Component Library

## Status: Draft

## Overview

Extend the REPL-driven workflow with persistent component storage. Components iterated on in the preview area can be committed to a browsable library that persists across restarts.

The workflow:
1. Iterate on hiccup component using `preview!`
2. Adjust CSS in `styles.css`, observe hot-reloaded styles
3. Repeat until satisfied
4. Commit the component to the library
5. Browse committed components later via gallery or direct URL

## Goals

1. Server-side state tracking for preview and library
2. Sandestin registry for all component operations
3. Persist committed components to EDN file
4. Browser UI with uncommitted indicator and commit button
5. URL-routable gallery and component views
6. Everything reloadable at the REPL

## Design Principles

- **Server is truth** - All state lives on the server. Browser reflects server state.
- **Sandestin throughout** - All operations are effects in a sandbox registry.
- **Chassis aliases** - Components should favor Chassis alias definitions with stored usage examples.
- **URL-routable** - Every view has a URL for deep linking and multi-tab support.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Sandbox Registry                         │
│  (sandestin effects for preview + library management)       │
├─────────────────────────────────────────────────────────────┤
│  State Atom                                                 │
│  {:preview    {:hiccup [...]}                               │
│   :view       {:type :preview | :component | :gallery       │
│                :name :button}                               │
│   :library    {:button {:hiccup [...] :description "..."}   │
│                :card   {:hiccup [...] :description "..."}}} │
├─────────────────────────────────────────────────────────────┤
│  Effects                                                    │
│  ::sandbox/preview         - set preview content            │
│  ::sandbox/preview-append  - append to preview              │
│  ::sandbox/preview-clear   - clear preview                  │
│  ::sandbox/commit          - save to library + EDN          │
│  ::sandbox/uncommit        - remove from library            │
│  ::sandbox/show            - view single component          │
│  ::sandbox/show-gallery    - view all components            │
│  ::sandbox/show-preview    - switch to preview view         │
├─────────────────────────────────────────────────────────────┤
│  REPL API (wrappers over dispatch)                          │
│  (preview! hiccup)     (commit! name)      (show! name)     │
│  (preview-append! h)   (uncommit! name)    (show-all!)      │
│  (preview-clear!)      (components)                         │
└─────────────────────────────────────────────────────────────┘
          │                              ▲
          │ broadcast                    │ POST /commit, etc.
          ▼                              │
┌─────────────────────────────────────────────────────────────┐
│                       Browser                               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Nav: [Preview] [Gallery]              [Commit] [Clear]│  │
│  ├───────────────────────────────────────────────────────┤  │
│  │                                                       │  │
│  │              Rendered Content                         │  │
│  │     (preview / single component / gallery grid)       │  │
│  │                                                       │  │
│  │              [uncommitted badge if preview]           │  │
│  └───────────────────────────────────────────────────────┘  │
│                                                             │
│  Routes:                                                    │
│    /sandbox              - preview view (default)           │
│    /sandbox/components   - gallery view                     │
│    /sandbox/c/:name      - single component view            │
└─────────────────────────────────────────────────────────────┘
```

## State Model

### Server State Atom

```clojure
{:preview {:hiccup [:div "Current preview content"]}

 :view {:type :preview}  ;; or {:type :component :name :button}
                         ;; or {:type :gallery}

 :library {:primary-button
           {:hiccup [::ui/button {:variant :primary} "Submit"]
            :description "Primary action button"
            :created-at #inst "2024-..."}

           :user-card
           {:hiccup [::ui/card {:title "Name"} [:p "Bio"]]
            :description "User profile card"
            :created-at #inst "2024-..."}}}
```

### Persistence

- **File**: `resources/components.edn`
- **Load**: On system start, load existing library
- **Save**: On every `commit!`, write entire library to EDN
- **Format**:

```clojure
;; resources/components.edn
{:primary-button
 {:hiccup [::ui/button {:variant :primary} "Submit"]
  :description "Primary action button"}

 :user-card
 {:hiccup [::ui/card {:title "Name"} [:p "Bio"]]
  :description "User profile card"}}
```

## File Structure

```
dev/src/clj/
├── sandbox/
│   ├── registry.clj      # Sandestin registry for sandbox effects
│   ├── state.clj         # State atom, load/save EDN
│   ├── views.clj         # Hiccup for all views
│   └── app.clj           # Routes, middleware (updated)

resources/
├── components.edn        # Persisted component library
└── public/
    └── styles.css        # Global CSS (hot-reloaded)
```

## Implementation

### 1. sandbox/state.clj - State Management

```clojure
(ns sandbox.state
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def components-path "resources/components.edn")

(defn load-library
  "Load component library from EDN file."
  []
  (let [f (io/file components-path)]
    (if (.exists f)
      (edn/read-string (slurp f))
      {})))

(defn save-library!
  "Save component library to EDN file."
  [library]
  (spit components-path (pr-str library)))

(defn initial-state
  "Create initial state, loading persisted library."
  []
  (atom {:preview {:hiccup nil}
         :view    {:type :preview}
         :library (load-library)}))
```

### 2. sandbox/registry.clj - Sandestin Registry

```clojure
(ns sandbox.registry
  (:require [ascolais.sandestin :as s]
            [ascolais.sfere :as sfere]
            [ascolais.twk :as twk]
            [sandbox.state :as state]
            [sandbox.views :as views]))

(defn registry
  "Create sandbox registry with state atom."
  [state-atom]
  {::s/effects
   {::preview
    {::s/description "Set preview content and broadcast to clients"
     ::s/schema [:tuple [:= ::preview] :any]
     ::s/handler
     (fn [{:keys [dispatch]} _system hiccup]
       (swap! state-atom assoc
              :preview {:hiccup hiccup}
              :view {:type :preview})
       (dispatch {} {}
         [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
           [::twk/patch-elements (views/render-view @state-atom)]]]))}

    ::preview-append
    {::s/description "Append content to preview"
     ::s/schema [:tuple [:= ::preview-append] :any]
     ::s/handler
     (fn [{:keys [dispatch]} _system hiccup]
       (swap! state-atom update-in [:preview :hiccup]
              (fn [existing]
                (if existing
                  [:div existing hiccup]
                  hiccup)))
       (swap! state-atom assoc :view {:type :preview})
       (dispatch {} {}
         [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
           [::twk/patch-elements (views/render-view @state-atom)]]]))}

    ::preview-clear
    {::s/description "Clear preview content"
     ::s/schema [:tuple [:= ::preview-clear]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom assoc
              :preview {:hiccup nil}
              :view {:type :preview})
       (dispatch {} {}
         [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
           [::twk/patch-elements (views/render-view @state-atom)]]]))}

    ::commit
    {::s/description "Commit current preview to library"
     ::s/schema [:tuple [:= ::commit] :keyword [:maybe :string]]
     ::s/handler
     (fn [{:keys [dispatch]} _system name description]
       (let [{:keys [preview]} @state-atom
             hiccup (:hiccup preview)]
         (when hiccup
           (swap! state-atom assoc-in [:library name]
                  {:hiccup hiccup
                   :description (or description "")
                   :created-at (java.util.Date.)})
           (state/save-library! (:library @state-atom))
           (dispatch {} {}
             [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
               [::twk/patch-elements (views/render-view @state-atom)]]]))))}

    ::uncommit
    {::s/description "Remove component from library"
     ::s/schema [:tuple [:= ::uncommit] :keyword]
     ::s/handler
     (fn [{:keys [dispatch]} _system name]
       (swap! state-atom update :library dissoc name)
       (state/save-library! (:library @state-atom))
       (dispatch {} {}
         [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
           [::twk/patch-elements (views/render-view @state-atom)]]]))}

    ::show
    {::s/description "Show a single component"
     ::s/schema [:tuple [:= ::show] :keyword]
     ::s/handler
     (fn [{:keys [dispatch]} _system name]
       (swap! state-atom assoc :view {:type :component :name name})
       (dispatch {} {}
         [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
           [::twk/patch-elements (views/render-view @state-atom)]]]))}

    ::show-gallery
    {::s/description "Show component gallery"
     ::s/schema [:tuple [:= ::show-gallery]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom assoc :view {:type :gallery})
       (dispatch {} {}
         [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
           [::twk/patch-elements (views/render-view @state-atom)]]]))}

    ::show-preview
    {::s/description "Switch to preview view"
     ::s/schema [:tuple [:= ::show-preview]]
     ::s/handler
     (fn [{:keys [dispatch]} _system]
       (swap! state-atom assoc :view {:type :preview})
       (dispatch {} {}
         [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
           [::twk/patch-elements (views/render-view @state-atom)]]]))}

    ::sync-view
    {::s/description "Sync a client to current view state"
     ::s/schema [:tuple [:= ::sync-view]]
     ::s/handler
     (fn [_ctx _system]
       ;; Returns effects to be sent to the connecting client
       [[::twk/patch-elements (views/render-view @state-atom)]])}}})
```

### 3. sandbox/views.clj - View Rendering

```clojure
(ns sandbox.views
  (:require [ascolais.twk :as twk]
            [dev.onionpancakes.chassis.core :as c]))

(def dev-only-styles
  "body { font-family: system-ui, sans-serif; margin: 0; }
   .sandbox-nav {
     display: flex; gap: 1rem; padding: 1rem;
     background: #f5f5f5; border-bottom: 1px solid #ddd;
     align-items: center;
   }
   .sandbox-nav a { color: #0066cc; text-decoration: none; }
   .sandbox-nav a:hover { text-decoration: underline; }
   .sandbox-nav .spacer { flex: 1; }
   .sandbox-nav button { padding: 0.5rem 1rem; cursor: pointer; }
   .uncommitted-badge {
     background: #fff3cd; color: #856404;
     padding: 0.25rem 0.5rem; border-radius: 4px;
     font-size: 0.85rem;
   }
   #content { padding: 2rem; }
   .gallery-grid {
     display: grid;
     grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
     gap: 1.5rem;
   }
   .gallery-item {
     border: 1px solid #ddd; border-radius: 8px;
     overflow: hidden; cursor: pointer;
   }
   .gallery-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
   .gallery-item-preview { padding: 1rem; min-height: 100px; }
   .gallery-item-footer {
     background: #f9f9f9; padding: 0.75rem;
     border-top: 1px solid #eee;
     font-weight: 500;
   }
   .component-view { max-width: 800px; }
   .component-header { margin-bottom: 1.5rem; }
   .component-header h2 { margin: 0 0 0.5rem 0; }
   .component-header p { color: #666; margin: 0; }
   .component-render {
     border: 2px dashed #ddd; padding: 2rem;
     border-radius: 8px; margin-bottom: 1rem;
   }
   .empty-state { color: #999; font-style: italic; }")

(defn nav-bar
  "Navigation bar with view controls."
  [{:keys [view preview]}]
  (let [view-type (:type view)
        has-preview? (some? (:hiccup preview))]
    [:nav.sandbox-nav
     [:a {:href "/sandbox"
          :data-on-click "@post('/sandbox/view/preview')"
          :style (when (= view-type :preview) "font-weight: bold")}
      "Preview"]
     [:a {:href "/sandbox/components"
          :data-on-click "@post('/sandbox/view/gallery')"
          :style (when (= view-type :gallery) "font-weight: bold")}
      "Gallery"]
     [:div.spacer]
     (when (and (= view-type :preview) has-preview?)
       [:span.uncommitted-badge "uncommitted"])
     (when (and (= view-type :preview) has-preview?)
       [:button {:data-on-click "@post('/sandbox/commit')"}
        "Commit"])
     (when (= view-type :preview)
       [:button {:data-on-click "@post('/sandbox/clear')"}
        "Clear"])]))

(defn preview-view
  "Render preview area."
  [{:keys [preview]}]
  (let [hiccup (:hiccup preview)]
    [:div#preview
     (if hiccup
       hiccup
       [:p.empty-state "Preview area - use (preview! hiccup) from REPL"])]))

(defn gallery-view
  "Render component gallery."
  [{:keys [library]}]
  [:div#gallery
   (if (seq library)
     [:div.gallery-grid
      (for [[name {:keys [hiccup description]}] (sort-by key library)]
        [:div.gallery-item
         {:key (str name)
          :data-on-click (str "@post('/sandbox/view/component/" (clojure.core/name name) "')")}
         [:div.gallery-item-preview hiccup]
         [:div.gallery-item-footer (clojure.core/name name)]])]
     [:p.empty-state "No components yet - commit some from the preview!"])])

(defn component-view
  "Render single component view."
  [{:keys [library view]}]
  (let [name (:name view)
        {:keys [hiccup description]} (get library name)]
    [:div.component-view
     [:div.component-header
      [:h2 (clojure.core/name name)]
      (when (seq description)
        [:p description])]
     [:div.component-render
      (if hiccup
        hiccup
        [:p.empty-state "Component not found"])]
     [:button {:data-on-click "@post('/sandbox/view/gallery')"}
      "← Back to Gallery"]]))

(defn render-view
  "Render the appropriate view based on state."
  [state]
  (let [view-type (get-in state [:view :type])]
    [:div#app
     (nav-bar state)
     [:div#content
      (case view-type
        :preview   (preview-view state)
        :gallery   (gallery-view state)
        :component (component-view state)
        (preview-view state))]]))

(defn sandbox-page
  "Full page shell - content populated via SSE."
  []
  [c/doctype-html5
   [:html {:lang "en"}
    [:head
     [:meta {:charset "UTF-8"}]
     [:title "Component Sandbox"]
     [:script {:src twk/CDN-url :type "module"}]
     [:link {:rel "stylesheet" :href "/styles.css"}]
     [:style dev-only-styles]]
    [:body {:data-on-load "@post('/sandbox/sse')"}
     [:div#app
      [:p {:style "padding: 2rem; color: #999"} "Connecting..."]]]]])
```

### 4. Updated Routes

```clojure
;; In sandbox/app.clj
(def routes
  [;; Page shell
   ["/sandbox" {:get (fn [_] {:body (views/sandbox-page)})}]
   ["/sandbox/components" {:get (fn [_] {:body (views/sandbox-page)})}]
   ["/sandbox/c/:name" {:get (fn [_] {:body (views/sandbox-page)})}]

   ;; SSE connection - syncs client to current state
   ["/sandbox/sse" {:post sse-connect}]

   ;; View switching (from browser nav)
   ["/sandbox/view/preview" {:post view-preview}]
   ["/sandbox/view/gallery" {:post view-gallery}]
   ["/sandbox/view/component/:name" {:post view-component}]

   ;; Actions
   ["/sandbox/commit" {:post commit-handler}]
   ["/sandbox/clear" {:post clear-handler}]])
```

### 5. Updated REPL API

```clojure
;; In dev.clj

(defn preview!
  "Replace preview with hiccup content."
  [hiccup]
  (dispatch [[::sandbox/preview hiccup]]))

(defn preview-append!
  "Append hiccup to preview."
  [hiccup]
  (dispatch [[::sandbox/preview-append hiccup]]))

(defn preview-clear!
  "Clear preview area."
  []
  (dispatch [[::sandbox/preview-clear]]))

(defn commit!
  "Commit current preview to library."
  ([name] (commit! name nil))
  ([name description]
   (dispatch [[::sandbox/commit name description]])))

(defn uncommit!
  "Remove component from library."
  [name]
  (dispatch [[::sandbox/uncommit name]]))

(defn show!
  "Show a single component."
  [name]
  (dispatch [[::sandbox/show name]]))

(defn show-all!
  "Show component gallery."
  []
  (dispatch [[::sandbox/show-gallery]]))

(defn components
  "List all committed components."
  []
  (keys (:library @(:state system/*system*))))
```

## Usage

### REPL Workflow

```clojure
(dev)
(start)

;; Iterate on a component
(preview! [:button.btn-primary "Submit"])

;; Edit styles.css, see hot-reload

;; Try variations
(preview! [:button.btn-primary {:disabled true} "Submit"])

;; When satisfied, commit
(commit! :primary-button "Primary action button")

;; View in gallery
(show-all!)

;; View single component
(show! :primary-button)

;; List components
(components)
;; => (:primary-button)

;; Remove if needed
(uncommit! :primary-button)
```

### Browser Workflow

1. Open `localhost:3000/sandbox`
2. See preview area with any REPL-pushed content
3. Click "Gallery" to see committed components
4. Click a component to view it in isolation
5. Click "Commit" button to save current preview (prompts for name)
6. New tabs opening to `/sandbox/c/:name` go directly to that component

## URL Behavior

| URL | Initial View |
|-----|--------------|
| `/sandbox` | Preview |
| `/sandbox/components` | Gallery |
| `/sandbox/c/:name` | Single component |

When navigating via SSE actions, the URL doesn't change (SPA-style). Direct URL access renders the shell which then syncs to server state.

## Success Criteria

- [ ] `(preview!)` sets content and broadcasts to all clients
- [ ] `(commit! :name)` saves to library atom and `components.edn`
- [ ] `(show!)` and `(show-all!)` switch views across all clients
- [ ] Components persist across server restarts
- [ ] Browser shows "uncommitted" badge when previewing
- [ ] Browser "Commit" button prompts for name and commits
- [ ] Gallery renders grid of all committed components
- [ ] Single component view shows component in isolation
- [ ] `/sandbox/c/:name` deep links work for new tabs
- [ ] Everything reloads cleanly via `(reload)`

## Future Enhancements

1. **Commit modal** - Browser-side modal for entering name/description
2. **Chassis alias integration** - Define aliases from REPL, store in library
3. **Component variants** - Support multiple states per component
4. **Export/publish** - Generate static component documentation
5. **Datastar integration** - Interactive component states

## Relationship to Other Specs

- **002-dev-sandbox**: Extends sandbox with state management and persistence
- **003-css-hot-reload**: Works alongside - CSS changes still hot-reload to all views
- Reuses same SSE connection pattern and sfere broadcast
