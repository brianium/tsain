(ns sandbox.ui
  "Chassis alias definitions for sandbox components.

   Aliases define structure and styling only. Datastar attributes
   (data-signals, data-on, etc.) are passed through via attrs.

   Usage:
     ;; Static (structure only)
     [:sandbox.ui/counter]

     ;; Interactive (with Datastar attributes)
     [:sandbox.ui/counter {:data-signals:count \"0\"}
      [:button {:data-on:click \"$count--\"} \"−\"]
      [:span {:data-text \"$count\"}]
      [:button {:data-on:click \"$count++\"} \"+\"]]"
  (:require [dev.onionpancakes.chassis.core :as c]))

;; Example: Counter component
;; Structure: container with decrement, value display, and increment
(defmethod c/resolve-alias ::counter
  [_ attrs _content]
  [:div.counter attrs
   [:button.counter-btn "−"]
   [:span.counter-value "0"]
   [:button.counter-btn "+"]])

;; Example: Toggle button
;; Structure: button that can be styled active/inactive
(defmethod c/resolve-alias ::toggle
  [_ attrs content]
  [:button.toggle attrs
   (or content "Toggle")])

;; Example: Card component
;; Structure: container with optional header, body, footer
(defmethod c/resolve-alias ::card
  [_ attrs content]
  [:div.card attrs
   content])

;; Example: Accordion component
;; Structure: collapsible section with header and content
(defmethod c/resolve-alias ::accordion
  [_ attrs content]
  (let [[header body] (if (and (sequential? content)
                               (>= (count content) 2))
                        [(first content) (rest content)]
                        ["Section" content])]
    [:div.accordion attrs
     [:button.accordion-header header]
     [:div.accordion-content body]]))

;; Example: Tabs component
;; Structure: tab bar with content panel
(defmethod c/resolve-alias ::tabs
  [_ attrs content]
  [:div.tabs attrs
   [:div.tab-bar
    [:button.tab-item.active "Tab 1"]
    [:button.tab-item "Tab 2"]
    [:button.tab-item "Tab 3"]]
   [:div.tab-content
    (or content [:p "Tab content"])]])
