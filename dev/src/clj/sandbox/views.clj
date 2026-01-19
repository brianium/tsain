(ns sandbox.views
  (:require [ascolais.twk :as twk]
            [dev.onionpancakes.chassis.core :as c]))

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

(defn- get-component-hiccup
  "Get hiccup from component data, supporting both old and new formats.
   Old format: {:hiccup [...]}
   New format: {:examples [{:label ... :hiccup [...]} ...]}"
  ([component-data]
   (get-component-hiccup component-data 0))
  ([component-data example-idx]
   (if-let [examples (:examples component-data)]
     (:hiccup (nth examples (or example-idx 0) (first examples)))
     (:hiccup component-data))))

(defn gallery-view
  "Render component gallery."
  [{:keys [library]}]
  [:div#gallery
   (if (seq library)
     [:div.gallery-grid
      (for [[component-name component-data] (sort-by key library)]
        [:div.gallery-item
         {:key (str component-name)
          :data-on:click (str "@post('/sandbox/view/component/" (name component-name) "')")}
         [:div.gallery-item-preview (get-component-hiccup component-data)]
         [:div.gallery-item-footer
          (name component-name)
          (when (seq (:description component-data))
            [:div.gallery-item-desc (:description component-data)])]])]
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
  "Render single component view with prev/next navigation and example selector."
  [{:keys [library view]}]
  (let [component-name (:name view)
        component-data (get library component-name)
        {:keys [description examples]} component-data
        example-idx (or (:example-idx view) 0)
        selected-example (when examples (nth examples example-idx (first examples)))
        hiccup (if examples
                 (:hiccup selected-example)
                 (get-component-hiccup component-data))
        {:keys [prev next]} (component-neighbors library component-name)]
    [:div.component-view
     [:div.component-nav
      (if prev
        [:button.nav-prev
         {:data-on:click (str "@post('/sandbox/view/component/" (name prev) "')")}
         "← " (name prev)]
        [:span.nav-placeholder])
      [:div.component-title
       [:h2 (name component-name)]
       ;; Example selector dropdown when multiple examples exist
       ;; Unique ID forces recreation on component change (avoids stale morph state)
       (when (and examples (> (count examples) 1))
         [:select.config-selector
          {:id (str "variant-" (name component-name))
           :data-on:change (str "@post('/sandbox/view/component/" (name component-name) "?idx=' + evt.target.value)")}
          (for [[idx {:keys [label]}] (map-indexed vector examples)]
            [:option {:value idx :selected (= idx example-idx)} (or label (str "Example " (inc idx)))])])]
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
        [:link {:rel "stylesheet" :href "/sandbox.css"}]
        [:link {:rel "stylesheet" :href "/styles.css"}]]
       [:body {:data-init (str "@post('" sse-url "')")}
        [:div#app
         [:p {:style "padding: 2rem; color: #999"} "Connecting..."]]]]])))
