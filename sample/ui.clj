(ns myapp.ui
  "Chassis aliases for UI components.

   This namespace defines component structure using chassis aliases.
   Use namespaced keywords for config props that should be elided from HTML output.

   ## Alias Pattern

   ```clojure
   (defmethod c/resolve-alias ::my-card [_ attrs _]
     (let [{:my-card/keys [title body icon]} attrs]
       [:div.my-card attrs  ;; namespaced keys auto-elided by chassis
        [:div.my-card-header
         [:span.my-card-icon icon]
         [:h3.my-card-title title]]
        [:p.my-card-body body]]))
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
   ```"
  (:require [dev.onionpancakes.chassis.core :as c]))

;; Define your component aliases below
;; Use (describe dispatch) in the REPL to discover available effects
;; Use (sample dispatch ::tsain/preview) to generate example invocations
