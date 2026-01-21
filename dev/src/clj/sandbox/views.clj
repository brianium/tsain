(ns sandbox.views
  "Project-specific views for tsain sandbox.

  Re-exports library views and requires project UI aliases.
  The sandbox.ui require ensures aliases are registered before hiccup is rendered."
  (:require [ascolais.tsain.views :as lib-views]
            ;; Require sandbox.ui to register chassis aliases
            sandbox.ui))

;; Re-export library view functions
(def nav-bar lib-views/nav-bar)
(def preview-view lib-views/preview-view)
(def gallery-view lib-views/gallery-view)
(def component-view lib-views/component-view)
(def components-view lib-views/components-view)
(def render-view lib-views/render-view)
(def sandbox-page lib-views/sandbox-page)
