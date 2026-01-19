(ns sandbox.views
  (:require [ascolais.twk :as twk]
            [dev.onionpancakes.chassis.core :as c]))

(def dev-only-styles
  "Styles for sandbox chrome - component styles go in styles.css"
  ":root {
     --bg-primary: #0f1115;
     --bg-secondary: #1a1d23;
     --bg-elevated: #23272f;
     --border-subtle: #2d323c;
     --border-muted: #383e4a;
     --text-primary: #f0f2f5;
     --text-secondary: #9ca3af;
     --text-muted: #6b7280;
     --accent: #f59e0b;
     --accent-hover: #fbbf24;
     --accent-subtle: rgba(245, 158, 11, 0.12);
     --success: #10b981;
     --radius-sm: 6px;
     --radius-md: 10px;
     --radius-lg: 14px;
     --shadow-sm: 0 1px 2px rgba(0,0,0,0.3);
     --shadow-md: 0 4px 12px rgba(0,0,0,0.4);
     --shadow-lg: 0 8px 24px rgba(0,0,0,0.5);
     --transition: 0.15s ease;
   }
   * { box-sizing: border-box; }
   body {
     font-family: 'Inter', system-ui, -apple-system, sans-serif;
     margin: 0;
     background: var(--bg-primary);
     color: var(--text-primary);
     line-height: 1.5;
     -webkit-font-smoothing: antialiased;
   }
   .sandbox-nav {
     display: flex;
     gap: 0.5rem;
     padding: 0.75rem 1.5rem;
     background: var(--bg-secondary);
     border-bottom: 1px solid var(--border-subtle);
     align-items: center;
   }
   .sandbox-nav a {
     color: var(--text-secondary);
     text-decoration: none;
     cursor: pointer;
     padding: 0.5rem 1rem;
     border-radius: var(--radius-sm);
     font-size: 0.9rem;
     font-weight: 500;
     transition: all var(--transition);
   }
   .sandbox-nav a:hover {
     color: var(--text-primary);
     background: var(--bg-elevated);
   }
   .sandbox-nav a.active {
     color: var(--accent);
     background: var(--accent-subtle);
   }
   .sandbox-nav .spacer { flex: 1; }
   .sandbox-nav button {
     padding: 0.5rem 1rem;
     cursor: pointer;
     border: 1px solid var(--border-muted);
     border-radius: var(--radius-sm);
     background: var(--bg-elevated);
     color: var(--text-secondary);
     font-size: 0.85rem;
     font-weight: 500;
     transition: all var(--transition);
   }
   .sandbox-nav button:hover {
     background: var(--border-subtle);
     color: var(--text-primary);
     border-color: var(--border-muted);
   }
   .uncommitted-badge {
     background: var(--accent-subtle);
     color: var(--accent);
     padding: 0.35rem 0.75rem;
     border-radius: 99px;
     font-size: 0.8rem;
     font-weight: 600;
     letter-spacing: 0.02em;
   }
   #content { padding: 2rem 2.5rem; }
   #preview { min-height: 200px; }
   .gallery-grid {
     display: grid;
     grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
     gap: 1.5rem;
   }
   .gallery-item {
     background: var(--bg-secondary);
     border: 1px solid var(--border-subtle);
     border-radius: var(--radius-lg);
     overflow: hidden;
     cursor: pointer;
     transition: all var(--transition);
   }
   .gallery-item:hover {
     border-color: var(--border-muted);
     box-shadow: var(--shadow-md);
     transform: translateY(-2px);
   }
   .gallery-item-preview {
     padding: 1.5rem;
     min-height: 180px;
     background: #fafafa;
     display: flex;
     align-items: center;
     justify-content: center;
   }
   .gallery-item-footer {
     background: var(--bg-secondary);
     padding: 1rem 1.25rem;
     border-top: 1px solid var(--border-subtle);
   }
   .gallery-item-footer > span:first-child,
   .gallery-item-footer > :not(.gallery-item-desc):first-child {
     font-weight: 600;
     font-size: 0.95rem;
     color: var(--text-primary);
   }
   .gallery-item-desc {
     font-size: 0.85rem;
     color: var(--text-muted);
     font-weight: normal;
     margin-top: 0.35rem;
     line-height: 1.4;
   }
   .component-view { max-width: 900px; }
   .component-header { margin-bottom: 1.5rem; }
   .component-header h2 {
     margin: 0 0 0.5rem 0;
     font-weight: 600;
   }
   .component-header p {
     color: var(--text-secondary);
     margin: 0;
   }
   .component-render {
     background: #fafafa;
     border: 1px solid var(--border-subtle);
     padding: 2.5rem;
     border-radius: var(--radius-lg);
     margin-bottom: 1.25rem;
     display: flex;
     align-items: center;
     justify-content: center;
   }
   .component-actions { display: flex; gap: 0.5rem; }
   .component-actions button {
     padding: 0.5rem 1rem;
     cursor: pointer;
     border: 1px solid var(--border-muted);
     border-radius: var(--radius-sm);
     background: var(--bg-elevated);
     color: var(--text-secondary);
     font-size: 0.85rem;
     font-weight: 500;
     transition: all var(--transition);
   }
   .component-actions button:hover {
     background: var(--border-subtle);
     color: var(--text-primary);
   }
   .empty-state {
     color: var(--text-muted);
     font-style: italic;
     text-align: center;
     padding: 3rem;
   }
   .commit-form {
     display: flex;
     gap: 0.5rem;
     align-items: center;
   }
   .commit-form input {
     padding: 0.5rem 0.75rem;
     border: 1px solid var(--border-muted);
     border-radius: var(--radius-sm);
     width: 160px;
     background: var(--bg-elevated);
     color: var(--text-primary);
     font-size: 0.85rem;
     transition: border-color var(--transition);
   }
   .commit-form input:focus {
     outline: none;
     border-color: var(--accent);
   }
   .commit-form input::placeholder { color: var(--text-muted); }
   .commit-form button {
     background: var(--accent) !important;
     color: #000 !important;
     border-color: var(--accent) !important;
     font-weight: 600 !important;
   }
   .commit-form button:hover {
     background: var(--accent-hover) !important;
     border-color: var(--accent-hover) !important;
   }
   .commit-form button:disabled {
     opacity: 0.5;
     cursor: not-allowed;
   }
   .component-nav {
     display: flex;
     align-items: center;
     justify-content: space-between;
     gap: 1rem;
     margin-bottom: 1.25rem;
   }
   .component-nav h2 {
     margin: 0;
     flex: 1;
     text-align: center;
     font-weight: 600;
     font-size: 1.25rem;
   }
   .component-nav .nav-placeholder { width: 140px; }
   .nav-prev, .nav-next {
     padding: 0.5rem 1rem;
     cursor: pointer;
     background: var(--bg-elevated);
     border: 1px solid var(--border-muted);
     border-radius: var(--radius-sm);
     min-width: 140px;
     color: var(--text-secondary);
     font-size: 0.85rem;
     font-weight: 500;
     transition: all var(--transition);
   }
   .nav-prev:hover, .nav-next:hover {
     background: var(--border-subtle);
     color: var(--text-primary);
   }
   .nav-prev { text-align: left; }
   .nav-next { text-align: right; }
   .component-desc {
     color: var(--text-secondary);
     margin: 0 0 1.25rem 0;
     text-align: center;
     font-size: 0.95rem;
   }")

(defn nav-bar
  "Navigation bar with view controls."
  [{:keys [view preview]}]
  (let [view-type (:type view)
        has-preview? (some? (:hiccup preview))]
    [:nav.sandbox-nav
     [:a {:class (when (= view-type :preview) "active")
          :data-on:click "@post('/sandbox/view/preview')"}
      "Preview"]
     [:a {:class (when (= view-type :gallery) "active")
          :data-on:click "@post('/sandbox/view/gallery')"}
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
         [:button {:data-on:click "@post('/sandbox/commit')"
                   :data-attr-disabled "!$commitName"}
          "Commit"]]])
     (when (= view-type :preview)
       [:button {:data-on:click "@post('/sandbox/clear')"}
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
          :data-on:click (str "@post('/sandbox/view/component/" (name component-name) "')")}
         [:div.gallery-item-preview hiccup]
         [:div.gallery-item-footer
          (name component-name)
          (when (seq description)
            [:div.gallery-item-desc description])]])]
     [:p.empty-state "No components yet - commit some from the preview!"])])

(defn component-neighbors
  "Get previous and next component names for navigation.
   Returns {:prev keyword-or-nil :next keyword-or-nil}."
  [library current-name]
  (let [sorted-names (vec (sort (keys library)))
        idx (.indexOf sorted-names current-name)
        total (count sorted-names)]
    (when (and (>= idx 0) (> total 1))
      {:prev (nth sorted-names (mod (dec idx) total))
       :next (nth sorted-names (mod (inc idx) total))})))

(defn component-view
  "Render single component view with prev/next navigation."
  [{:keys [library view]}]
  (let [component-name (:name view)
        {:keys [hiccup description]} (get library component-name)
        {:keys [prev next]} (component-neighbors library component-name)]
    [:div.component-view
     [:div.component-nav
      (if prev
        [:button.nav-prev
         {:data-on:click (str "@post('/sandbox/view/component/" (name prev) "')")}
         "← " (name prev)]
        [:span.nav-placeholder])
      [:h2 (name component-name)]
      (if next
        [:button.nav-next
         {:data-on:click (str "@post('/sandbox/view/component/" (name next) "')")}
         (name next) " →"]
        [:span.nav-placeholder])]
     (when (seq description)
       [:p.component-desc description])
     [:div.component-render
      (if hiccup
        hiccup
        [:p.empty-state "Component not found"])]
     [:div.component-actions
      [:button {:data-on:click "@post('/sandbox/view/gallery')"}
       "← Back to Gallery"]
      [:button {:data-on:click (str "@post('/sandbox/uncommit/" (name component-name) "')")}
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
