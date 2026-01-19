# 002: Development Sandbox

## Status: Complete

## Overview

Create a reloadable development sandbox for rapidly iterating on hiccup components. The sandbox provides a browser-based UI that can be updated in real-time from the REPL by dispatching twk effects via sandestin.

This enables a tight feedback loop for component development:
1. Write hiccup in the REPL
2. Dispatch a twk effect
3. See the result instantly in the browser
4. Iterate without page refreshes

## Goals

1. Set up a reloadable dev environment following the kaiin demo pattern
2. Establish a persistent SSE connection for REPL-driven UI updates
3. Create a minimal component sandbox page
4. Use standard sandestin dispatch for all REPL interactions
5. Lay groundwork for a hiccup component library

## Design Principles

- **Vanilla modern CSS** - Use native CSS features: grids, anchor positioning, container queries, `:has()`, nesting. No frameworks, no preprocessors required.
- **Sandestin ecosystem** - Built on sandestin for dispatch, twk for Datastar effects, sfere for connection management.
- **Single SSE connection** - One persistent connection per browser tab handles both REPL patches and file watcher broadcasts.
- **Multi-device support** - Each connection gets a unique key, but all match the broadcast pattern. Open the sandbox on laptop, phone, tablet - all update simultaneously (like browser-sync).
- **No custom helpers** - Use sandestin dispatch directly. No wrapper functions that hide what's happening.

## Dependencies

### deps.edn Additions

```clojure
{:paths ["src/clj" "resources"]
 :deps {org.clojure/clojure {:mvn/version "1.12.0"}}

 :aliases
 {:dev
  {:extra-paths ["dev/src/clj" "dev/resources"]
   :extra-deps
   {;; Development tools
    djblue/portal {:mvn/version "0.62.2"}
    io.github.tonsky/clj-reload {:mvn/version "1.0.0"}

    ;; Sandestin ecosystem
    io.github.brianium/sandestin {:git/tag "v0.5.0" :git/sha "526d4c5"}
    io.github.brianium/twk {:git/tag "v0.2.1" :git/sha "e3c5011"}
    io.github.brianium/sfere {:git/tag "v0.6.0" :git/sha "2114511"}
    io.github.brianium/kaiin {:git/tag "v0.1.0" :git/sha "088a6af"}

    ;; Web infrastructure
    http-kit/http-kit {:mvn/version "2.8.0"}
    dev.data-star.clojure/http-kit {:mvn/version "1.0.0-RC7"}
    dev.onionpancakes/chassis {:mvn/version "1.0.365"}
    metosin/reitit {:mvn/version "0.10.0"}
    metosin/malli {:mvn/version "0.20.0"}}}

  :test
  {:extra-paths ["test/src/clj"]
   :extra-deps
   {io.github.cognitect-labs/test-runner
    {:git/tag "v0.5.1" :git/sha "dfb30dd"}}
   :main-opts ["-m" "cognitect.test-runner"]
   :exec-fn cognitect.test-runner.api/test}}}
```

## File Structure

Following the kaiin demo pattern:

```
dev/src/clj/
├── user.clj              # REPL entry point, clj-reload init
├── dev.clj               # Lifecycle commands, Portal setup, dispatch access
└── sandbox/
    ├── system.clj        # System state (*system* var)
    ├── app.clj           # Router, middleware, server setup
    └── views.clj         # Sandbox page
```

## Implementation

### 1. user.clj - REPL Entry Point

```clojure
(ns user
  (:require [clj-reload.core :as reload]))

(reload/init
 {:dirs ["src/clj" "dev/src/clj" "test/src/clj"]
  :no-reload '#{user}})

(defn dev
  "Load and switch to the dev namespace."
  []
  (require 'dev)
  (in-ns 'dev))
```

### 2. dev.clj - Development Namespace

```clojure
(ns dev
  (:require [clj-reload.core :as reload]
            [portal.api :as p]
            [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [sandbox.app :as app]
            [sandbox.system :as system]))

;; Portal setup (reload-safe)
(defonce portal (p/open))
(defonce _setup-tap (add-tap #'p/submit))

;; System lifecycle
(defn start
  ([] (start 3000))
  ([port] (app/start-system port)))

(defn stop [] (app/stop-system))

(defn reload []
  (reload/reload))

(defn restart []
  (stop)
  (reload)
  (start))

;; clj-reload hooks
(defn before-ns-unload []
  (stop))

(defn after-ns-reload []
  (start))

;; Dispatch access for REPL use
(defn dispatch
  "Dispatch effects via the running system."
  ([effects]
   (when-let [d (:dispatch system/*system*)]
     (d effects)))
  ([system effects]
   (when-let [d (:dispatch system/*system*)]
     (d system effects)))
  ([system dispatch-data effects]
   (when-let [d (:dispatch system/*system*)]
     (d system dispatch-data effects))))

;; Preview helpers for component development
(defn preview!
  "Replace the sandbox preview area with new hiccup content.
   Broadcasts to all connected browsers/devices."
  [hiccup]
  (dispatch {} {}
    [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
      [::twk/patch-elements [:div#preview hiccup]]]]))

(defn preview-append!
  "Append hiccup content to the sandbox preview area."
  [hiccup]
  (dispatch {} {}
    [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
      [::twk/patch-elements hiccup
       {twk/selector "#preview" twk/patch-mode twk/pm-append}]]]))

(defn preview-clear!
  "Clear the sandbox preview area."
  []
  (preview! [:p {:style "color: #999"} "Preview area"]))
```

### 3. sandbox/system.clj - System State

```clojure
(ns sandbox.system)

(def ^:dynamic *system*
  "Dynamic var holding the running system.
   Contains :store, :dispatch, and :server keys."
  nil)
```

### 4. sandbox/app.clj - Application Setup

```clojure
(ns sandbox.app
  (:require [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [starfederation.datastar.clojure.adapter.http-kit :as ds-hk]
            [org.httpkit.server :as hk]
            [reitit.ring :as rr]
            [sandbox.system :as system]
            [sandbox.views :as views]))

(defn- create-store []
  (sfere/store {:type :caffeine
                :duration-ms 1800000  ;; 30 minutes for dev
                :expiry-mode :sliding}))

(defn- create-dispatch [store]
  (s/create-dispatch
   [(twk/registry)
    (sfere/registry store)]))

(defn- wrap-request-logging [handler]
  (fn [request]
    (tap> {:uri (:uri request)
           :method (:request-method request)})
    (handler request)))

;; Handlers
(defn index [_request]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/sandbox-page)})

(defn sse-connect [_request]
  "Establish SSE connection for REPL-driven updates.
   Each connection gets a unique ID so multiple browsers/devices
   can view the sandbox simultaneously (like browser-sync)."
  {::sfere/key [:sandbox (str (random-uuid))]
   ::twk/fx [[::twk/patch-elements
              [:div#status "Connected - ready for REPL updates"]]]})

;; Routes
(def routes
  [["/" {:name ::index :get index}]
   ["/sse" {:name ::sse :post sse-connect}]])

(defn- create-router [dispatch]
  (rr/router
   routes
   {:data {:middleware [(twk/with-datastar ds-hk/->sse-response dispatch)]}}))

(defn- create-app [dispatch]
  (rr/ring-handler
   (create-router dispatch)
   (rr/create-default-handler)
   {:middleware [wrap-request-logging]}))

(defn start-system
  ([] (start-system 3000))
  ([port]
   (let [store (create-store)
         dispatch (create-dispatch store)
         handler (create-app dispatch)
         server (hk/run-server handler {:port port})]
     (alter-var-root #'system/*system*
                     (constantly {:store store
                                  :dispatch dispatch
                                  :server server}))
     (println (str "Sandbox running at http://localhost:" port))
     system/*system*)))

(defn stop-system []
  (when-let [{:keys [server]} system/*system*]
    (server)
    (alter-var-root #'system/*system* (constantly nil))
    (println "Sandbox stopped")))
```

### 5. sandbox/views.clj - Sandbox Page

```clojure
(ns sandbox.views
  (:require [ascolais.twk :as twk]
            [dev.onionpancakes.chassis.core :as c]))

(def styles
  "body { font-family: system-ui, sans-serif; margin: 2rem; }
   #preview { border: 2px dashed #ccc; padding: 1rem; min-height: 200px; margin-top: 1rem; }
   #status { color: #666; font-size: 0.9rem; margin-bottom: 1rem; }
   h1 { margin-top: 0; }")

(defn sandbox-page []
  [c/doctype-html5
   [:html {:lang "en"}
    [:head
     [:meta {:charset "UTF-8"}]
     [:title "Component Sandbox"]
     [:script {:src twk/CDN-url :type "module"}]
     [:style styles]]
    [:body {:data-init "@post('/sse')"}
     [:h1 "Component Sandbox"]
     [:div#status "Connecting..."]
     [:div#preview
      [:p {:style "color: #999"} "Preview area"]]]]])
```

## Usage

### Starting the Sandbox

```bash
clj -M:dev
```

```clojure
user=> (dev)
dev=> (start)
;; Sandbox running at http://localhost:3000
```

### REPL-Driven Component Development

Use the preview helper functions for rapid iteration:

```clojure
;; Replace preview with new content
(preview! [:h1 "Hello World"])

;; Build up multiple components
(preview-clear!)
(preview-append! [:div.card [:h3 "Card 1"] [:p "First component"]])
(preview-append! [:div.card [:h3 "Card 2"] [:p "Second component"]])

;; Complex component with inline styles
(preview!
  [:div {:style "background: white; padding: 1.5rem; border-radius: 8px;"}
   [:h2 "User Profile"]
   [:p {:style "color: #666;"} "Component description"]])
```

For lower-level control, use dispatch directly:

```clojure
;; Direct dispatch with twk effects
(dispatch {} {}
  [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
    [::twk/patch-elements [:div#preview [:h1 "Hello"]]]]])

;; Execute script in browser
(dispatch {} {}
  [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
    [::twk/execute-script "console.log('hello from REPL')"]]])
```

### Exploring the Dispatch

```clojure
;; Discover available effects
(s/describe (:dispatch system/*system*))

;; List effects only
(s/describe (:dispatch system/*system*) :effects)

;; Inspect specific effect
(s/describe (:dispatch system/*system*) ::twk/patch-elements)

;; Generate sample invocation
(s/sample (:dispatch system/*system*) ::twk/patch-elements)

;; Search for broadcast-related
(s/grep (:dispatch system/*system*) "broadcast")
```

## Success Criteria

- [x] `clj -M:dev` starts cleanly with all dependencies
- [x] `(dev)` loads the dev namespace and opens Portal
- [x] `(start)` launches server at localhost:3000
- [x] Browser shows sandbox page with SSE connection
- [x] Dispatch with twk effects updates the browser instantly
- [x] Multiple browsers/tabs all receive updates simultaneously
- [x] `(reload)` hot-reloads code without losing connection
- [x] `(restart)` cleanly restarts the entire system

## Future Enhancements

1. **Component library namespace** - Dedicated ns for reusable components
2. **Multiple preview panes** - Side-by-side component comparison
3. **Component snapshot** - Save current preview to a component file
4. **Signal state** - Manage Datastar signals from REPL
