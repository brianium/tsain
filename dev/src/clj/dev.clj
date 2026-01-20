(ns dev
  (:require [clj-reload.core :as reload]
            [portal.api :as p]
            [ascolais.sandestin :as s]
            [ascolais.tsain :as tsain]
            [sandbox.app :as app]
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
;; Sandestin Discovery Aliases
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def describe
  "List and inspect registered effects/actions.

  Usage:
    (describe dispatch)              ;; List all items
    (describe dispatch :effects)     ;; List effects only
    (describe dispatch ::tsain/preview)  ;; Inspect specific effect"
  s/describe)

(def sample
  "Generate example invocations.

  Usage:
    (sample dispatch ::tsain/preview)     ;; One sample
    (sample dispatch ::tsain/preview 3)   ;; Multiple samples"
  s/sample)

(def grep
  "Search registry by pattern.

  Usage:
    (grep dispatch \"component\")
    (grep dispatch #\"preview|commit\")"
  s/grep)
