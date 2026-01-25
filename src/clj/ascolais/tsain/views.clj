(ns ascolais.tsain.views
  "View rendering functions for tsain sandbox.

  These functions render the sandbox UI including:
  - Navigation bar with view controls
  - Preview area for REPL iteration
  - Gallery view for component browsing
  - Component detail view with sidebar

  Usage:
    (require '[ascolais.tsain.views :as views])

    ;; Render full view based on state
    (views/render-view state)

    ;; Render page shell
    (views/sandbox-page)
    (views/sandbox-page :components)
    (views/sandbox-page [:component :my-card])"
  (:require [ascolais.twk :as twk]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [dev.onionpancakes.chassis.core :as c]
            [html.yeah :as hy]
            [malli.core :as m]
            [ascolais.tsain.icons :as icons]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Props Extraction from Malli
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- format-type
  "Format a malli schema type for display."
  [schema]
  (try
    (case (m/type schema)
      :string "string"
      :int "int"
      :keyword "keyword"
      :boolean "boolean"
      :any "any"
      :enum (str/join " | " (map pr-str (m/children schema)))
      :map "map"
      :vector "vector"
      :sequential "seq"
      :set "set"
      :tuple "tuple"
      ;; Default: show simplified form
      (let [form (m/form schema)]
        (if (keyword? form)
          (name form)
          "complex")))
    (catch Exception _
      "unknown")))

(defn- extract-props
  "Extract prop information from a malli schema.
  Returns a seq of {:name :type :required} maps."
  [schema]
  (when (and schema (vector? schema) (= :map (first schema)))
    (for [entry (rest schema)
          :when (vector? entry)
          :let [[key props-or-schema & rest] entry
                ;; Handle both [:key schema] and [:key {:optional true} schema]
                [props child-schema] (if (map? props-or-schema)
                                       [props-or-schema (first rest)]
                                       [{} props-or-schema])]]
      {:name (name key)
       :type (format-type child-schema)
       :required (not (:optional props))})))

(defn- format-hiccup
  "Format hiccup for display with pretty-printing."
  [hiccup]
  (with-out-str (pprint/pprint hiccup)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Navigation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- commit-form
  "Render the commit form with category selection."
  [categories]
  [:div.commit-form
   {:data-signals "{commitName: '', commitCategory: '', newCategory: '', showNewCategory: false}"}
   [:input.commit-name
    {:type "text"
     :placeholder "component-name"
     :data-bind "commitName"}]
   [:div.category-select-wrapper
    [:select.category-select
     {:data-bind "commitCategory"
      :data-on:change "if ($commitCategory === '__new__') { $showNewCategory = true } else { $showNewCategory = false; $newCategory = '' }"}
     [:option {:value ""} "Category..."]
     (for [cat categories]
       [:option {:value cat} cat])
     [:option {:value "__new__"} "+ New category..."]]
    [:input.new-category-input
     {:type "text"
      :placeholder "New category name"
      :data-bind "newCategory"
      :data-show "$showNewCategory"}]]
   [:button {:data-on:click "@post('/sandbox/commit')"
             :data-attr-disabled "!$commitName"}
    (icons/icon :save {:class "btn-icon"}) "Commit"]])

(defn nav-bar
  "Navigation bar with view controls."
  [{:keys [view preview committed? categories]}]
  (let [view-type (:type view)
        has-preview? (some? (:hiccup preview))]
    [:nav.sandbox-nav
     [:a {:class (when (= view-type :preview) "active")
          :data-on:click "@post('/sandbox/view/preview')"}
      (icons/icon :eye {:class "nav-icon"}) "Preview"]
     [:a {:class (when (#{:gallery :components :component} view-type) "active")
          :data-on:click "@post('/sandbox/view/components')"}
      (icons/icon :grid-3x3 {:class "nav-icon"}) "Components"]
     [:div.spacer]
     ;; Background color picker (only on component views, not preview)
     (when (#{:gallery :components :component} view-type)
       [:div.color-picker
        (icons/icon :palette {:class "color-icon"})
        [:input {:type "color"
                 :data-bind "bgColor"
                 :data-on:change "localStorage.setItem('sandbox-bg-color', $bgColor)"
                 :title "Preview background color"}]])
     (when (and (= view-type :preview) has-preview?)
       [[:span {:class (if committed? "commit-badge committed" "commit-badge uncommitted")}
         (if committed? "committed" "uncommitted")]
        (commit-form categories)])
     (when (= view-type :preview)
       [:button {:data-on:click "@post('/sandbox/clear')"}
        (icons/icon :x {:class "btn-icon"}) "Clear"])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Category Grouping
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- grouped-components
  "Group components by category.
  Returns a sorted map of {category -> [[component-name component-data] ...]}
  Uncategorized components are placed in 'Other'."
  [library]
  (let [groups (group-by (fn [[_ data]]
                           (or (:category data) "Other"))
                         library)
        ;; Sort categories alphabetically, but put "Other" last
        category-set (set (keys groups))
        sorted-keys (-> (disj category-set "Other")
                        sort
                        vec
                        (cond-> (contains? category-set "Other") (conj "Other")))]
    (into (array-map)
          (map (fn [cat]
                 [cat (sort-by (comp name first) (get groups cat))]))
          sorted-keys)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Preview View
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn preview-view
  "Render preview area."
  [{:keys [preview]}]
  (let [hiccup (:hiccup preview)]
    [:div#preview
     (if hiccup
       hiccup
       [:p.empty-state "Preview area - use (dispatch [[::tsain/preview hiccup]]) from REPL"])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Gallery View
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
         {:data-on:click (str "@post('/sandbox/view/component/" (name component-name) "')")}
         [:div.gallery-item-preview {:data-style:background-color "$bgColor"} (get-component-hiccup component-data)]
         [:div.gallery-item-footer
          (name component-name)
          (when (seq (:description component-data))
            [:div.gallery-item-desc (:description component-data)])]])]
     [:p.empty-state "No components yet - commit some from the preview!"])])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component View
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- component-neighbors
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
         (icons/icon :chevron-left {:class "btn-icon"}) (name prev)]
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
         (name next) (icons/icon :chevron-right {:class "btn-icon"})]
        [:span.nav-placeholder])]
     (when (seq description)
       [:p.component-desc description])
     [:div.component-render {:data-style:background-color "$bgColor"}
      (if hiccup
        hiccup
        [:p.empty-state "Component not found"])]
     [:div.component-actions
      [:button {:data-on:click "@post('/sandbox/view/components')"}
       (icons/icon :chevron-left {:class "btn-icon"}) "Back"]
      [:button.copy-btn
       {:data-on:click (str "fetch('/sandbox/copy/" (name component-name) "?idx=" example-idx "')"
                            ".then(r => r.text())"
                            ".then(t => {"
                            "  navigator.clipboard.writeText(t);"
                            "  const btn = evt.target.closest('button');"
                            "  btn.querySelector('.copy-label').textContent = 'Copied!';"
                            "  setTimeout(() => btn.querySelector('.copy-label').textContent = 'Copy', 1500);"
                            "})")}
       (icons/icon :copy {:class "btn-icon"}) [:span.copy-label "Copy"]]
      [:button {:data-on:click (str "@post('/sandbox/uncommit/" (name component-name) "')")}
       (icons/icon :trash-2 {:class "btn-icon"}) "Delete"]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component Detail (Sidebar Layout)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- preview-tab
  "Render the preview tab content."
  [hiccup]
  [:div.tab-content.tab-preview
   {:data-show "$activeTab === 'preview'"}
   [:div.component-render {:data-style:background-color "$bgColor"}
    (if hiccup
      hiccup
      [:p.empty-state "Component not found"])]])

(defn- code-tab
  "Render the code tab content with formatted hiccup."
  [component-name hiccup example-idx]
  [:div.tab-content.tab-code
   {:data-show "$activeTab === 'code'"}
   [:div.code-toolbar
    [:button.copy-btn
     {:data-on:click (str "fetch('/sandbox/copy/" (name component-name) "?idx=" example-idx "')"
                          ".then(r => r.text())"
                          ".then(t => {"
                          "  navigator.clipboard.writeText(t);"
                          "  const btn = evt.target.closest('button');"
                          "  btn.querySelector('.copy-label').textContent = 'Copied!';"
                          "  setTimeout(() => btn.querySelector('.copy-label').textContent = 'Copy', 1500);"
                          "})")}
     (icons/icon :copy {:class "btn-icon"}) [:span.copy-label "Copy"]]
    [:button {:data-on:click (str "@post('/sandbox/uncommit/" (name component-name) "')")}
     (icons/icon :trash-2 {:class "btn-icon"}) "Delete"]]
   [:pre.code-block
    [:code (format-hiccup hiccup)]]])

(defn- props-tab
  "Render the props tab content with attribute documentation."
  [component-name]
  (let [hy-data (hy/element component-name)
        props (when hy-data (extract-props (:attributes hy-data)))
        doc (:doc hy-data)]
    [:div.tab-content.tab-props
     {:data-show "$activeTab === 'props'"}
     (when doc
       [:p.props-doc doc])
     (if (seq props)
       [:table.props-table
        [:thead
         [:tr
          [:th "Name"]
          [:th "Type"]
          [:th "Required"]]]
        [:tbody
         (for [{:keys [name type required]} props]
           [:tr
            [:td.prop-name name]
            [:td.prop-type type]
            [:td.prop-required
             (if required
               [:span.badge-required "required"]
               [:span.badge-optional "optional"])]])]]
       [:p.empty-state "No props defined"])]))

(defn- component-detail
  "Render component detail panel with tabs (used in sidebar layout)."
  [{:keys [library view]}]
  (let [component-name (:name view)
        component-data (get library component-name)
        {:keys [examples]} component-data
        example-idx (or (:example-idx view) 0)
        selected-example (when examples (nth examples example-idx (first examples)))
        hiccup (if examples
                 (:hiccup selected-example)
                 (get-component-hiccup component-data))
        {:keys [prev next]} (component-neighbors library component-name)
        hy-data (hy/element component-name)
        doc (:doc hy-data)]
    [:div.component-detail
     {:data-signals "{activeTab: 'preview'}"}

     ;; Header with navigation and title
     [:div.component-nav
      (if prev
        [:button.nav-prev
         {:data-on:click (str "@post('/sandbox/view/component/" (name prev) "')")}
         (icons/icon :chevron-left {:class "btn-icon"}) (name prev)]
        [:span.nav-placeholder])
      [:div.component-title
       [:h2 (name component-name)]
       (when (and examples (> (count examples) 1))
         [:select.config-selector
          {:id (str "variant-" (name component-name))
           :data-on:change (str "@post('/sandbox/view/component/" (name component-name) "?idx=' + evt.target.value)")}
          (for [[idx {:keys [label]}] (map-indexed vector examples)]
            [:option {:value idx :selected (= idx example-idx)} (or label (str "Example " (inc idx)))])])]
      (if next
        [:button.nav-next
         {:data-on:click (str "@post('/sandbox/view/component/" (name next) "')")}
         (name next) (icons/icon :chevron-right {:class "btn-icon"})]
        [:span.nav-placeholder])]

     ;; Description below header
     (when (seq doc)
       [:p.component-desc doc])

     ;; Tab bar
     [:div.tab-bar
      [:button.tab-btn
       {:data-class:active "$activeTab === 'preview'"
        :data-on:click "$activeTab = 'preview'"}
       (icons/icon :eye {:class "tab-icon"}) "Preview"]
      [:button.tab-btn
       {:data-class:active "$activeTab === 'code'"
        :data-on:click "$activeTab = 'code'"}
       (icons/icon :code {:class "tab-icon"}) "Code"]
      [:button.tab-btn
       {:data-class:active "$activeTab === 'props'"
        :data-on:click "$activeTab = 'props'"}
       (icons/icon :list {:class "tab-icon"}) "Props"]]

     ;; Tab content (all rendered, visibility controlled by data-show)
     (preview-tab hiccup)
     (code-tab component-name hiccup example-idx)
     (props-tab component-name)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Components View (Sidebar Layout)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- category-slug
  "Convert category name to a valid JavaScript identifier."
  [category]
  (-> category
      str/lower-case
      (str/replace #"[^a-z0-9]+" "_")))

(defn- sidebar-category
  "Render a collapsible category section in the sidebar."
  [category components current-name]
  (let [slug (category-slug category)]
    [:div.sidebar-category
     {:data-show (str "$searchQuery === '' || "
                      (str/join " || "
                                (map (fn [[comp-name _]]
                                       (str "'" (name comp-name) "'.toLowerCase().includes($searchQuery.toLowerCase())"))
                                     components)))}
     [:button.category-header
      {:data-on:click (str "$sidebarState." slug " = !$sidebarState." slug)}
      (icons/icon :chevron-down {:class "category-chevron"
                                 :data-class:rotated (str "!$sidebarState." slug)})
      [:span.category-name category]
      [:span.category-count (count components)]]
     [:div.category-items
      {:data-show (str "$sidebarState." slug)}
      (for [[component-name _] components]
        [:a.sidebar-item
         {:class (when (= component-name current-name) "active")
          :data-on:click (str "@post('/sandbox/view/component/" (name component-name) "')")
          :data-show (str "$searchQuery === '' || '" (name component-name) "'.toLowerCase().includes($searchQuery.toLowerCase())")}
         (name component-name)])]]))

(defn- build-initial-sidebar-state
  "Build initial sidebar state with all categories expanded."
  [grouped]
  (let [slugs (map (comp category-slug first) grouped)]
    (str "{"
         (str/join ", " (map #(str % ": true") slugs))
         "}")))

(defn components-view
  "Render sidebar + component view layout."
  [{:keys [library view sidebar-collapsed?]}]
  (let [current-name (:name view)
        grouped (grouped-components library)
        initial-state (build-initial-sidebar-state grouped)]
    [:div.components-layout
     {:class (when sidebar-collapsed? "sidebar-collapsed")
      :data-signals (str "{searchQuery: '', sidebarState: " initial-state "}")}

     ;; Sidebar
     [:aside.sidebar
      [:div.sidebar-header
       [:span.sidebar-title "Components"]
       [:button.sidebar-toggle
        {:data-on:click "@post('/sandbox/sidebar/toggle')"}
        (if sidebar-collapsed?
          (icons/icon :chevrons-right {:class "toggle-icon"})
          (icons/icon :chevrons-left {:class "toggle-icon"}))]]

      ;; Search input
      [:div.sidebar-search
       (icons/icon :search {:class "search-icon"})
       [:input {:type "text"
                :placeholder "Search..."
                :data-bind "searchQuery"
                :autocomplete "off"}]]

      [:nav.sidebar-list
       (for [[category components] grouped]
         (sidebar-category category components current-name))]]

     ;; Main content
     [:main.component-main
      (if current-name
        (component-detail {:library library :view view})
        [:div.empty-state "Select a component from the sidebar"])]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Main Render
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- extract-categories
  "Extract unique categories from library."
  [library]
  (->> library
       vals
       (keep :category)
       distinct
       sort))

(defn render-view
  "Render the appropriate view based on state."
  [state]
  (let [view-type (get-in state [:view :type])
        categories (extract-categories (:library state))
        nav-state (assoc state :categories categories)]
    [:div#app
     {:data-signals "{bgColor: localStorage.getItem('sandbox-bg-color') || '#f5f5f5'}"}
     (nav-bar nav-state)
     (case view-type
       :preview   [:div#content (preview-view state)]
       :gallery   [:div#content (gallery-view state)]
       :component [:div#content (component-view state)]
       :components (components-view state)
       [:div#content (preview-view state)])]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page Shell
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sandbox-page
  "Full page shell - content populated via SSE.
   Optional initial-view determines the starting view:
   - nil or :preview -> preview view (default)
   - :gallery or :components -> components view with sidebar
   - [:component name] -> specific component view with sidebar"
  ([] (sandbox-page nil))
  ([initial-view]
   (let [sse-url (case initial-view
                   :gallery "/sandbox/sse?view=components"
                   :components "/sandbox/sse?view=components"
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
