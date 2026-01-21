(ns ascolais.tsain
  "Tsain registry for REPL-driven component development.

  Provides a sandestin registry with discoverable effects for:
  - Preview: rapid hiccup iteration with broadcast
  - Component library: commit, show, and navigate components
  - Signal testing: patch Datastar signals

  Usage:
    (require '[ascolais.tsain :as tsain])
    (require '[ascolais.sandestin :as s])

    ;; Create registry (reads tsain.edn for config)
    (def tsain-reg (tsain/registry))
    (def dispatch (s/create-dispatch [tsain-reg (twk/registry) ...]))

    ;; Get state atom for routes
    (def tsain-state (::s/state tsain-reg))
    (def tsain-config (::tsain/config tsain-reg))

    ;; Discover available effects
    (s/describe dispatch)
    (s/sample dispatch ::tsain/preview)"
  (:require [ascolais.sandestin :as s]
            [ascolais.sfere :as sfere]
            [ascolais.tsain.views :as views]
            [ascolais.twk :as twk]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-config
  {:ui-namespace 'sandbox.ui
   :components-file "resources/components.edn"
   :stylesheet "dev/resources/public/styles.css"
   :port 3000})

(defn load-config
  "Load configuration from tsain.edn, merging with defaults."
  ([]
   (load-config {}))
  ([overrides]
   (let [f (io/file "tsain.edn")
         file-config (when (.exists f)
                       (edn/read-string (slurp f)))]
     (merge default-config file-config overrides))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Persistence (component library)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- load-library
  "Load component library from EDN file."
  [path]
  (let [f (io/file path)]
    (if (.exists f)
      (edn/read-string (slurp f))
      {})))

(defn- save-library!
  "Save component library to EDN file."
  [path library]
  (io/make-parents path)
  (spit path (with-out-str (pprint/pprint library))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View Rendering
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- broadcast-view!
  "Broadcast current view state to all connected clients."
  [dispatch state-atom]
  (dispatch {} {}
            [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
              [::twk/patch-elements (views/render-view @state-atom)]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schemas
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def HiccupSchema
  "Malli schema for hiccup elements."
  [:vector {:description "Hiccup element - a vector starting with keyword tag"
            :gen/elements [[:div [:h1 "Hello World"]]
                           [:div.card [:h2 "Title"] [:p "Content"]]
                           [:button {:data-on:click "@post('/action')"} "Click me"]]}
   :any])

(def ComponentNameSchema
  "Schema for component names."
  [:keyword {:description "Component name keyword (e.g., :game-card, :player-hud)"
             :gen/elements [:game-card :player-hud :action-button]}])

(def ExampleSchema
  "Schema for a single component example."
  [:map {:description "A labeled example variant of a component"}
   [:label :string]
   [:hiccup :any]])

(def ComponentOptsSchema
  "Schema for commit options."
  [:maybe {:description "Optional description string or options map"}
   [:or
    [:string {:description "Simple description"}]
    [:map {:description "Full options"}
     [:description {:optional true} :string]
     [:examples {:optional true} [:vector ExampleSchema]]]]])

(def SignalMapSchema
  "Schema for Datastar signal patches."
  [:map {:description "Map of signal names to values"
         :gen/elements [{:count 0}
                        {:selected true}
                        {:form {:email "test@example.com"}}]}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Registry Factory
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn registry
  "Create tsain registry with state management.

  Reads configuration from tsain.edn at project root.
  Accepts optional overrides map to merge with file config.

  Returns a sandestin registry map with effects for:
  - ::preview, ::preview-append, ::preview-clear
  - ::commit, ::uncommit
  - ::show, ::show-components, ::show-preview
  - ::patch-signals, ::toggle-sidebar, ::sync-view

  Usage:
    (tsain/registry)              ;; Use tsain.edn config
    (tsain/registry {:port 3001}) ;; Override port"
  ([]
   (registry {}))
  ([overrides]
   (let [config (load-config overrides)
         components-file (:components-file config)
         state-atom (atom {:preview {:hiccup nil}
                           :view {:type :preview}
                           :library (load-library components-file)
                           :sidebar-collapsed? false})]

     {::s/state state-atom
      ::config config

      ::s/effects
      {::preview
       {::s/description
        "Replace the sandbox preview area with hiccup content.

Broadcasts to all connected browsers/devices simultaneously.
Use this when iterating on new components before committing.
For adding to existing content, use ::preview-append.

Example:
  [::tsain/preview [:div.my-card [:h2 \"Title\"] [:p \"Body\"]]]"

        ::s/schema [:tuple [:= ::preview] HiccupSchema]

        ::s/handler
        (fn [{:keys [dispatch]} _system hiccup]
          (swap! state-atom assoc
                 :preview {:hiccup hiccup}
                 :view {:type :preview})
          (broadcast-view! dispatch state-atom))}

       ::preview-append
       {::s/description
        "Append hiccup content to the sandbox preview area.

Broadcasts to all connected browsers/devices.
Content is wrapped in a div alongside existing preview content.
Use ::preview to replace all content, or ::preview-clear to reset.

Example:
  [::tsain/preview-append [:div.card \"Another card\"]]"

        ::s/schema [:tuple [:= ::preview-append] HiccupSchema]

        ::s/handler
        (fn [{:keys [dispatch]} _system hiccup]
          (swap! state-atom update-in [:preview :hiccup]
                 (fn [existing]
                   (if existing
                     [:div existing hiccup]
                     hiccup)))
          (swap! state-atom assoc :view {:type :preview})
          (broadcast-view! dispatch state-atom))}

       ::preview-clear
       {::s/description
        "Clear the sandbox preview area.

Broadcasts to all connected browsers/devices.
After clearing, preview will show empty state message.

Example:
  [::tsain/preview-clear]"

        ::s/schema [:tuple [:= ::preview-clear]]

        ::s/handler
        (fn [{:keys [dispatch]} _system]
          (swap! state-atom assoc
                 :preview {:hiccup nil}
                 :view {:type :preview})
          (broadcast-view! dispatch state-atom))}

       ::commit
       {::s/description
        "Commit a component to the library.

Saves to both in-memory library and components.edn for persistence.
Components should use chassis aliases (see :ui-namespace in tsain.edn).

Accepts three forms:
1. Name only: uses current preview as single 'Default' example
2. Name + description string: adds description to preview-based example
3. Name + options map: full control with :description and :examples

Examples:
  [::tsain/commit :my-card]
  [::tsain/commit :my-card \"Card component\"]
  [::tsain/commit :my-card {:description \"...\" :examples [...]}]"

        ::s/schema [:tuple [:= ::commit] ComponentNameSchema ComponentOptsSchema]

        ::s/handler
        (fn [{:keys [dispatch]} _system component-name opts]
          (let [{:keys [preview]} @state-atom
                hiccup (:hiccup preview)
                component-data
                (cond
                  ;; Map with explicit examples
                  (and (map? opts) (:examples opts))
                  {:description (or (:description opts) "")
                   :examples (:examples opts)
                   :created-at (java.util.Date.)}

                  ;; String description (old API)
                  (string? opts)
                  {:description opts
                   :examples [{:label "Default" :hiccup hiccup}]
                   :created-at (java.util.Date.)}

                  ;; Map without examples (description only)
                  (map? opts)
                  {:description (or (:description opts) "")
                   :examples [{:label "Default" :hiccup hiccup}]
                   :created-at (java.util.Date.)}

                  ;; nil - no description, use preview
                  :else
                  {:description ""
                   :examples [{:label "Default" :hiccup hiccup}]
                   :created-at (java.util.Date.)})]
            (when (or hiccup (:examples opts))
              (swap! state-atom assoc-in [:library component-name] component-data)
              (save-library! components-file (:library @state-atom))
              (broadcast-view! dispatch state-atom))))}

       ::uncommit
       {::s/description
        "Remove a component from the library.

Deletes from both in-memory library and components.edn.
Use with caution - this cannot be undone (though git can help).

Example:
  [::tsain/uncommit :my-card]"

        ::s/schema [:tuple [:= ::uncommit] ComponentNameSchema]

        ::s/handler
        (fn [{:keys [dispatch]} _system component-name]
          (swap! state-atom update :library dissoc component-name)
          (save-library! components-file (:library @state-atom))
          (broadcast-view! dispatch state-atom))}

       ::show
       {::s/description
        "Show a single component in the browser.

Navigates to the component view and optionally selects an example index.
Broadcasts view change to all connected clients.
Use ::show-components for sidebar navigation, ::show for legacy direct view.

Examples:
  [::tsain/show :my-card]
  [::tsain/show :my-card 1]  ;; Show second example"

        ::s/schema [:tuple [:= ::show] ComponentNameSchema [:maybe :int]]

        ::s/handler
        (fn [{:keys [dispatch]} _system component-name example-idx]
          (swap! state-atom assoc :view {:type :component
                                         :name component-name
                                         :example-idx (or example-idx 0)})
          (broadcast-view! dispatch state-atom))}

       ::show-gallery
       {::s/description
        "Show the component gallery grid view.

Displays all committed components in a grid layout.
Broadcasts view change to all connected clients.
Prefer ::show-components for the sidebar-based navigation.

Example:
  [::tsain/show-gallery]"

        ::s/schema [:tuple [:= ::show-gallery]]

        ::s/handler
        (fn [{:keys [dispatch]} _system]
          (swap! state-atom assoc :view {:type :gallery})
          (broadcast-view! dispatch state-atom))}

       ::show-preview
       {::s/description
        "Switch to the preview view.

Returns to the live preview area for REPL-driven iteration.
Broadcasts view change to all connected clients.

Example:
  [::tsain/show-preview]"

        ::s/schema [:tuple [:= ::show-preview]]

        ::s/handler
        (fn [{:keys [dispatch]} _system]
          (swap! state-atom assoc :view {:type :preview})
          (broadcast-view! dispatch state-atom))}

       ::sync-view
       {::s/description
        "Sync a client to current view state.

Returns TWK effects to render current state for a connecting client.
Used internally by SSE connection handler.

Example:
  [::tsain/sync-view]"

        ::s/schema [:tuple [:= ::sync-view]]

        ::s/handler
        (fn [_ctx _system]
          [[::twk/patch-elements (views/render-view @state-atom)]])}

       ::patch-signals
       {::s/description
        "Patch Datastar signals on all connected browsers.

Broadcasts a signal update to all sandbox clients.
Useful for testing interactive components from the REPL.

Examples:
  [::tsain/patch-signals {:count 42}]
  [::tsain/patch-signals {:open true}]
  [::tsain/patch-signals {:form {:email \"test@example.com\"}}]"

        ::s/schema [:tuple [:= ::patch-signals] SignalMapSchema]

        ::s/handler
        (fn [{:keys [dispatch]} _system signals]
          (dispatch {} {}
                    [[::sfere/broadcast {:pattern [:* [:sandbox :*]]}
                      [::twk/patch-signals signals]]]))}

       ::toggle-sidebar
       {::s/description
        "Toggle the sidebar collapsed state.

Broadcasts view change to all connected clients.
Used by sidebar collapse/expand button in UI.

Example:
  [::tsain/toggle-sidebar]"

        ::s/schema [:tuple [:= ::toggle-sidebar]]

        ::s/handler
        (fn [{:keys [dispatch]} _system]
          (swap! state-atom update :sidebar-collapsed? not)
          (broadcast-view! dispatch state-atom))}

       ::show-components
       {::s/description
        "Show the components view with sidebar navigation.

Displays the component library with a collapsible sidebar.
If component-name is nil or not found, selects the first component.
Preserves example-idx when viewing the same component.

Examples:
  [::tsain/show-components nil]           ;; Show first component
  [::tsain/show-components :my-card]      ;; Show specific component"

        ::s/schema [:tuple [:= ::show-components] [:maybe ComponentNameSchema]]

        ::s/handler
        (fn [{:keys [dispatch]} _system component-name]
          (let [library (:library @state-atom)
                current-view (:view @state-atom)
                target-name (if (and component-name (contains? library component-name))
                              component-name
                              (first (sort (keys library))))
                example-idx (if (and (= (:name current-view) target-name)
                                     (:example-idx current-view))
                              (:example-idx current-view)
                              0)]
            (swap! state-atom assoc :view {:type :components
                                           :name target-name
                                           :example-idx example-idx})
            (broadcast-view! dispatch state-atom)))}}})))
