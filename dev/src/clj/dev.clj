(ns dev
  (:require [clj-reload.core :as reload]
            [portal.api :as p]
            [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [sandbox.app :as app]
            [sandbox.registry :as registry]
            [sandbox.system :as system]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Portal Setup (reload-safe)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defonce portal (p/open))
(defonce _setup-tap (add-tap #'p/submit))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; System Lifecycle
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn start
  "Start the sandbox server."
  ([] (start 3000))
  ([port] (app/start-system port)))

(defn stop
  "Stop the sandbox server."
  []
  (app/stop-system))

(defn reload
  "Reload changed namespaces."
  []
  (reload/reload))

(defn restart
  "Full restart: stop, reload, and start."
  []
  (stop)
  (reload)
  (start))

;; clj-reload hooks
(defn before-ns-unload []
  (stop))

(defn after-ns-reload []
  (start))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Dispatch Access
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn dispatch
  "Dispatch effects via the running system.
   Convenience wrapper around system dispatch."
  ([effects]
   (when-let [d (:dispatch system/*system*)]
     (d effects)))
  ([system effects]
   (when-let [d (:dispatch system/*system*)]
     (d system effects)))
  ([system dispatch-data effects]
   (when-let [d (:dispatch system/*system*)]
     (d system dispatch-data effects))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Preview Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn preview!
  "Replace the sandbox preview area with new hiccup content.
   Broadcasts to all connected browsers/devices.

   Usage:
     (preview! [:h1 \"Hello World\"])
     (preview! [:div
                 [:h2 \"Card Title\"]
                 [:p \"Card description\"]])

   The hiccup is rendered inside #preview, replacing any existing content.
   Use preview-append! to add content without clearing."
  [hiccup]
  (dispatch [[::registry/preview hiccup]]))

(defn preview-append!
  "Append hiccup content to the sandbox preview area.
   Broadcasts to all connected browsers/devices.

   Usage:
     (preview-append! [:div.card \"Card 1\"])
     (preview-append! [:div.card \"Card 2\"])

   Content is appended to #preview without clearing existing content.
   Use preview! to reset/replace all content."
  [hiccup]
  (dispatch [[::registry/preview-append hiccup]]))

(defn preview-clear!
  "Clear the sandbox preview area.
   Broadcasts to all connected browsers/devices."
  []
  (dispatch [[::registry/preview-clear]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component Library
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn commit!
  "Commit component to the library.
   Saves to both memory and components.edn for persistence.

   Simple (current preview as single example):
     (commit! :my-card)
     (commit! :my-card \"Card component\")

   With multiple examples:
     (commit! :my-card
       {:description \"Card component\"
        :examples [{:label \"Basic\" :hiccup [:div.card \"Basic\"]}
                   {:label \"With image\" :hiccup [:div.card ...]}]})"
  ([component-name]
   (commit! component-name nil))
  ([component-name opts]
   (dispatch [[::registry/commit component-name opts]])))

(defn uncommit!
  "Remove a component from the library.
   Deletes from both memory and components.edn.

   Usage:
     (uncommit! :primary-button)"
  [component-name]
  (dispatch [[::registry/uncommit component-name]]))

(defn show!
  "Show a single component in the browser.
   Broadcasts view change to all connected clients.

   Usage:
     (show! :primary-button)
     (show! :primary-button 1)  ;; Show second example"
  ([component-name]
   (show! component-name 0))
  ([component-name example-idx]
   (dispatch [[::registry/show component-name example-idx]])))

(defn show-all!
  "Show the component gallery in the browser.
   Broadcasts view change to all connected clients."
  []
  (dispatch [[::registry/show-gallery]]))

(defn components
  "List all committed component names.

   Usage:
     (components)
     ;; => (:primary-button :user-card)"
  []
  (when-let [state (:state system/*system*)]
    (keys (:library @state))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Signal Testing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn patch-signals!
  "Patch Datastar signals on all connected browsers.
   Useful for testing interactive components from the REPL.

   Usage:
     (patch-signals! {:count 42})
     (patch-signals! {:open true})
     (patch-signals! {:form {:email \"test@example.com\"}})"
  [signals]
  (dispatch [[::registry/patch-signals signals]]))
