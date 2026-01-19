# 003: File Watcher and CSS Hot-Reload

## Status: Complete

## Overview

Enable hot-reloading of CSS (and other assets) during development. The watcher monitors file changes and broadcasts effects to all sandbox connections via the existing SSE connection.

## Goals

1. Vanilla modern CSS file that works with standard tooling
2. Automatic hot-reload when watched files change
3. Extensible architecture for watching multiple file types
4. Broadcast to existing sandbox connections (no separate watch endpoint)
5. Clean integration with sandestin dispatch

## Design Principles

- **Vanilla modern CSS** - Native CSS features: grids, anchor positioning, container queries, `:has()`, nesting. No frameworks required.
- **Single connection model** - File watcher broadcasts to all sandbox connections (`[:* [:sandbox :*]]`), reusing the same SSE connection used for REPL patches.
- **Sandestin throughout** - Uses sandestin dispatch and sfere for connection management.

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│   File Change   │────▶│   Watcher        │────▶│   sfere         │
│   (beholder)    │     │   (ext-fx map)   │     │   broadcast     │
└─────────────────┘     └──────────────────┘     └─────────────────┘
                                                          │
                                                          ▼
                                                 ┌─────────────────┐
                                                 │ Sandbox clients │
                                                 │ [:* [:sandbox]] │
                                                 └─────────────────┘
```

## Dependencies

Add to deps.edn `:dev` alias:

```clojure
{:extra-deps
 {nextjournal/beholder {:mvn/version "1.0.2"}
  ...}}
```

## File Structure

```
dev/src/clj/
├── sandbox/
│   ├── watcher.clj       # Watcher core
│   └── ...
└── dev/resources/public/
    └── styles.css        # Watched CSS file
```

## Implementation

### 1. sandbox/watcher.clj - File Watcher

```clojure
(ns sandbox.watcher
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ascolais.sfere :as sfere]
            [ascolais.twk :as twk]
            [nextjournal.beholder :as beholder]
            [sandbox.system :as system]))

(defn- change-handler
  "Return a function that calls on-change with a normalized event"
  [on-change]
  (fn [event]
    (let [path      (str (:path event))
          extension (when-let [idx (str/last-index-of path ".")]
                      (subs path (inc idx)))]
      (on-change {:path path
                  :ext  extension
                  :type (:type event)}))))

(defn broadcast!
  "Broadcast effects to all sandbox connections."
  [fx]
  (when-let [{:keys [dispatch]} system/*system*]
    (dispatch {} {}
              [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
                fx]])))

(defn reload-css!
  "Broadcast CSS reload to all sandbox connections."
  []
  (broadcast!
   [::twk/execute-script
    "document.querySelectorAll('link[rel=stylesheet]').forEach(l => l.href = l.href.split('?')[0] + '?v=' + Date.now())"]))

(defn reload-page!
  "Broadcast page reload to all sandbox connections."
  []
  (broadcast!
   [::twk/execute-script "window.location.reload()"]))

(def default-ext-fx
  "Default extension to effect mapping"
  {"css" reload-css!})

(defn watcher
  "Create a file watcher that triggers effects based on file extension.

   Config keys:
   | key     | description                                    |
   |---------|------------------------------------------------|
   | :paths  | vector of paths to watch                       |
   | :ext-fx | map of extension -> effect fn (default: css)   |"
  [{:keys [paths ext-fx]
    :or   {ext-fx default-ext-fx}}]
  (let [valid-paths (filterv #(.exists (io/file %)) paths)]
    (when (seq valid-paths)
      (apply beholder/watch
             (change-handler
              (fn [{:keys [ext path]}]
                (when-some [effect-fn (get ext-fx ext)]
                  (println (str "File changed: " path))
                  (effect-fn))))
             valid-paths))))

(defn stop!
  "Stop a watcher"
  [w]
  (when w
    (beholder/stop w)))
```

### 2. Integration with sandbox/app.clj

```clojure
(ns sandbox.app
  (:require [ring.middleware.resource :refer [wrap-resource]]
            [sandbox.watcher :as watcher]
            ...))

;; Add static file serving
(defn- create-app [dispatch]
  (-> (rr/ring-handler
       (create-router dispatch)
       (rr/create-default-handler))
      (wrap-resource "public")  ;; Serves from resources/public
      wrap-request-logging))

(defn start-system
  ([] (start-system 3000))
  ([port]
   (let [store    (create-store)
         dispatch (create-dispatch store)
         handler  (create-app dispatch)
         server   (hk/run-server handler {:port port})
         watch    (watcher/watcher
                   {:paths ["dev/resources/public/styles.css"]})]
     (alter-var-root #'system/*system*
                     (constantly {:store    store
                                  :dispatch dispatch
                                  :server   server
                                  :watcher  watch}))
     (println (str "Sandbox running at http://localhost:" port))
     system/*system*)))

(defn stop-system []
  (when-let [{:keys [server watcher]} system/*system*]
    (watcher/stop! watcher)
    (server)
    (alter-var-root #'system/*system* (constantly nil))
    (println "Sandbox stopped")))
```

### 3. Sandbox Page with Stylesheet

```clojure
(defn sandbox-page []
  (c/html
   [c/doctype-html5
    [:html {:lang "en"}
     [:head
      [:meta {:charset "UTF-8"}]
      [:title "Component Sandbox"]
      [:script {:src twk/CDN-url :type "module"}]
      [:link {:rel "stylesheet" :href "/styles.css"}]
      [:style dev-only-styles]]
     [:body {:data-init "@post('/sse')"}
      [:h1 "Component Sandbox"]
      [:div#status "Connecting..."]
      [:div#preview
       [:p {:style "color: #999"} "Preview area"]]]]]))
```

### 4. Example CSS File

`dev/resources/public/styles.css`:

```css
/* Modern vanilla CSS */

/* CSS nesting */
.card {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 1rem;

  & h2 {
    margin-top: 0;
  }

  &:hover {
    box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  }
}

/* Grid layout */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

/* Container queries */
.responsive-card {
  container-type: inline-size;

  @container (min-width: 300px) {
    display: flex;
    gap: 1rem;
  }
}

/* :has() selector */
.form-group:has(:invalid) {
  border-color: red;
}

/* Anchor positioning (when supported) */
.tooltip {
  position: absolute;
  position-anchor: --trigger;
  top: anchor(bottom);
  left: anchor(center);
}
```

## Data Flow

```
1. Edit styles.css, save
       ↓
2. beholder detects change
       ↓
3. change-handler extracts extension "css"
       ↓
4. ext-fx lookup → reload-css!
       ↓
5. broadcast! via sfere
   Pattern: [:* [:sandbox :*]]
       ↓
6. twk/execute-script sent to all sandbox tabs
       ↓
7. Browser cache-busts stylesheet links
```

## REPL Helpers

Available in dev namespace:

```clojure
;; Manual CSS reload
(watcher/reload-css!)

;; Manual page reload
(watcher/reload-page!)

;; Custom broadcast
(watcher/broadcast! [::twk/execute-script "console.log('hello')"])
```

## Configuration

The watcher uses an extension-to-function map:

```clojure
(watcher/watcher
 {:paths  ["dev/resources/public/styles.css"
           "dev/resources/public/app.js"]
  :ext-fx {"css" watcher/reload-css!
           "js"  watcher/reload-page!}})
```

## Usage

```bash
clj -M:dev
```

```clojure
(dev)
(start)
;; Sandbox at http://localhost:3000
;; Watching dev/resources/public/styles.css
```

Edit CSS → Save → Browser updates automatically.

## Success Criteria

- [x] CSS file served at `/styles.css`
- [x] Watcher starts with system, stops cleanly
- [x] CSS changes trigger automatic browser reload
- [x] Multiple browser tabs all receive updates
- [x] `(watcher/reload-css!)` works as manual fallback
- [x] ext-fx map allows adding new file types

## Relationship to Other Specs

- **002-dev-sandbox**: Watcher integrates into the sandbox system, sharing store/dispatch
- Uses the same `/sse` connection - no separate watch endpoint needed
- Both REPL patches and file changes broadcast to `[:* [:sandbox :*]]`
