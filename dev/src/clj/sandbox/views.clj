(ns sandbox.views
  (:require [ascolais.twk :as twk]
            [dev.onionpancakes.chassis.core :as c]))

(def sandbox-styles
  "Minimal styles for sandbox chrome - component styles go in styles.css"
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
     [:link {:rel "stylesheet" :href "/styles.css"}]
     [:style sandbox-styles]]
    [:body {:data-init "@post('/sse')"}
     [:h1 "Component Sandbox"]
     [:div#status "Connecting..."]
     [:div#preview
      [:p {:style "color: #999"} "Preview area"]]]]])
