(ns sandbox.views
  (:require [ascolais.twk :as twk]
            [dev.onionpancakes.chassis.core :as c]))

(def dev-only-styles
  "Styles for sandbox chrome - component styles go in styles.css"
  "body { font-family: system-ui, sans-serif; margin: 0; }
   .sandbox-nav {
     display: flex; gap: 1rem; padding: 1rem;
     background: #f5f5f5; border-bottom: 1px solid #ddd;
     align-items: center;
   }
   .sandbox-nav a { color: #0066cc; text-decoration: none; cursor: pointer; }
   .sandbox-nav a:hover { text-decoration: underline; }
   .sandbox-nav a.active { font-weight: bold; }
   .sandbox-nav .spacer { flex: 1; }
   .sandbox-nav button {
     padding: 0.5rem 1rem; cursor: pointer;
     border: 1px solid #ccc; border-radius: 4px;
     background: white;
   }
   .sandbox-nav button:hover { background: #f0f0f0; }
   .uncommitted-badge {
     background: #fff3cd; color: #856404;
     padding: 0.25rem 0.5rem; border-radius: 4px;
     font-size: 0.85rem;
   }
   #content { padding: 2rem; }
   #preview { min-height: 200px; }
   .gallery-grid {
     display: grid;
     grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
     gap: 1.5rem;
   }
   .gallery-item {
     border: 1px solid #ddd; border-radius: 8px;
     overflow: hidden; cursor: pointer;
     transition: box-shadow 0.2s;
   }
   .gallery-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
   .gallery-item-preview {
     padding: 1rem; min-height: 100px;
     background: #fafafa;
   }
   .gallery-item-footer {
     background: white; padding: 0.75rem;
     border-top: 1px solid #eee;
     font-weight: 500;
   }
   .gallery-item-desc {
     font-size: 0.85rem; color: #666;
     font-weight: normal; margin-top: 0.25rem;
   }
   .component-view { max-width: 800px; }
   .component-header { margin-bottom: 1.5rem; }
   .component-header h2 { margin: 0 0 0.5rem 0; }
   .component-header p { color: #666; margin: 0; }
   .component-render {
     border: 2px dashed #ddd; padding: 2rem;
     border-radius: 8px; margin-bottom: 1rem;
   }
   .component-actions { display: flex; gap: 0.5rem; }
   .empty-state { color: #999; font-style: italic; }
   .commit-form {
     display: flex; gap: 0.5rem; align-items: center;
   }
   .commit-form input {
     padding: 0.5rem; border: 1px solid #ccc;
     border-radius: 4px; width: 150px;
   }
   .commit-form input::placeholder { color: #999; }")

(defn nav-bar
  "Navigation bar with view controls."
  [{:keys [view preview]}]
  (let [view-type (:type view)
        has-preview? (some? (:hiccup preview))]
    [:nav.sandbox-nav
     [:a {:class (when (= view-type :preview) "active")
          :data-on-click "@post('/sandbox/view/preview')"}
      "Preview"]
     [:a {:class (when (= view-type :gallery) "active")
          :data-on-click "@post('/sandbox/view/gallery')"}
      "Gallery"]
     [:div.spacer]
     (when (and (= view-type :preview) has-preview?)
       [:<>
        [:span.uncommitted-badge "uncommitted"]
        [:div.commit-form
         {:data-signals "{commitName: ''}"}
         [:input {:type "text"
                  :placeholder "component-name"
                  :data-bind "commitName"}]
         [:button {:data-on-click "@post('/sandbox/commit')"
                   :data-attr-disabled "!$commitName"}
          "Commit"]]])
     (when (= view-type :preview)
       [:button {:data-on-click "@post('/sandbox/clear')"}
        "Clear"])]))

(defn preview-view
  "Render preview area."
  [{:keys [preview]}]
  (let [hiccup (:hiccup preview)]
    [:div#preview
     (if hiccup
       hiccup
       [:p.empty-state "Preview area - use (preview! hiccup) from REPL"])]))

(defn gallery-view
  "Render component gallery."
  [{:keys [library]}]
  [:div#gallery
   (if (seq library)
     [:div.gallery-grid
      (for [[component-name {:keys [hiccup description]}] (sort-by key library)]
        [:div.gallery-item
         {:key (str component-name)
          :data-on-click (str "@post('/sandbox/view/component/" (name component-name) "')")}
         [:div.gallery-item-preview hiccup]
         [:div.gallery-item-footer
          (name component-name)
          (when (seq description)
            [:div.gallery-item-desc description])]])]
     [:p.empty-state "No components yet - commit some from the preview!"])])

(defn component-view
  "Render single component view."
  [{:keys [library view]}]
  (let [component-name (:name view)
        {:keys [hiccup description]} (get library component-name)]
    [:div.component-view
     [:div.component-header
      [:h2 (name component-name)]
      (when (seq description)
        [:p description])]
     [:div.component-render
      (if hiccup
        hiccup
        [:p.empty-state "Component not found"])]
     [:div.component-actions
      [:button {:data-on-click "@post('/sandbox/view/gallery')"}
       "Back to Gallery"]
      [:button {:data-on-click (str "@post('/sandbox/uncommit/" (name component-name) "')")}
       "Delete"]]]))

(defn render-view
  "Render the appropriate view based on state."
  [state]
  (let [view-type (get-in state [:view :type])]
    [:div#app
     (nav-bar state)
     [:div#content
      (case view-type
        :preview   (preview-view state)
        :gallery   (gallery-view state)
        :component (component-view state)
        (preview-view state))]]))

(defn sandbox-page
  "Full page shell - content populated via SSE.
   Optional initial-view determines the starting view:
   - nil or :preview -> preview view (default)
   - :gallery -> gallery view
   - [:component name] -> specific component view"
  ([] (sandbox-page nil))
  ([initial-view]
   (let [sse-url (case initial-view
                   :gallery "/sandbox/sse?view=gallery"
                   (if (and (vector? initial-view) (= :component (first initial-view)))
                     (str "/sandbox/sse?view=component&name=" (name (second initial-view)))
                     "/sandbox/sse"))]
     [c/doctype-html5
      [:html {:lang "en"}
       [:head
        [:meta {:charset "UTF-8"}]
        [:title "Component Sandbox"]
        [:script {:src twk/CDN-url :type "module"}]
        [:link {:rel "stylesheet" :href "/styles.css"}]
        [:style dev-only-styles]]
       [:body {:data-init (str "@post('" sse-url "')")}
        [:div#app
         [:p {:style "padding: 2rem; color: #999"} "Connecting..."]]]]])))
