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
