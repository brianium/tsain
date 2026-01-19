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
        [:link {:rel "stylesheet" :href "/sandbox.css"}]
        [:link {:rel "stylesheet" :href "/styles.css"}]]
       [:body {:data-init (str "@post('" sse-url "')")}
        [:div#app
         [:p {:style "padding: 2rem; color: #999"} "Connecting..."]]]]])))
