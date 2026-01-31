(ns myapp.ui
  "Schema-driven UI components using html.yeah.

   This namespace defines component structure using html.yeah's `defelem` macro.
   Use namespaced keywords for config props that are elided from HTML output.

   ## defelem Pattern

   ```clojure
   (hy/defelem my-card
     [:map {:doc \"A card component with title and body\"
            :keys [my-card/title my-card/body my-card/icon]}
      [:my-card/title :string]
      [:my-card/body :string]
      [:my-card/icon {:optional true} [:maybe :string]]]
     [:div.my-card
      [:div.my-card-header
       (when my-card/icon [:span.my-card-icon my-card/icon])
       [:h3.my-card-title my-card/title]]
      [:p.my-card-body my-card/body]
      (hy/children)])
   ```

   ## Config vs HTML Attrs

   - **Namespaced attrs** (`:my-card/title`) = config props, elided from HTML
   - **Regular attrs** (`:class`, `:data-on:click`) = pass through to HTML

   ## Example Usage

   ```clojure
   [:myapp.ui/my-card
    {:my-card/title \"Hello World\"
     :my-card/body \"Component content\"
     :data-signals:selected \"false\"
     :data-on:click \"$selected = !$selected\"}]
   ```

   ## Discovery

   Components defined with `defelem` are discoverable:

   ```clojure
   (tsain/describe)              ;; List all components with schemas
   (tsain/describe ::my-card)    ;; Get details for specific component
   (tsain/grep \"card\")           ;; Search by keyword
   ```"
  (:require [html.yeah :as hy]))

;; Define your components below using hy/defelem
;; Use (tsain/describe) in the REPL to discover all components
;; Use (tsain/grep \"button\") to search for specific components
