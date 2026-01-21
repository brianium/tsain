(ns ascolais.tsain.routes
  "Route factory for tsain sandbox.

  Generates reitit route data parameterized by dispatch function and state atom.
  Routes handle sandbox pages, SSE connections, view switching, and actions.

  Usage:
    (require '[ascolais.tsain.routes :as tsain.routes])

    ;; Generate routes
    (def routes (tsain.routes/routes dispatch state-atom config))

    ;; Compose with your app routes
    (def router
      (rr/router
        (into my-app-routes routes)
        {:data {:middleware [(twk/with-datastar ds-hk/->sse-response dispatch)]}}))"
  (:require [ascolais.tsain :as tsain]
            [ascolais.tsain.views :as views]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Page Handlers - HTML shell, content via SSE
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- preview-page-handler
  "Handler for preview page."
  [_config]
  (fn [_request]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (views/sandbox-page)}))

(defn- gallery-page-handler
  "Handler for gallery/components page."
  [_config]
  (fn [_request]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (views/sandbox-page :components)}))

(defn- component-page-handler
  "Handler for single component page."
  [_config]
  (fn [{{:keys [name]} :path-params}]
    {:status 200
     :headers {"content-type" "text/html"}
     :body (views/sandbox-page [:component (keyword name)])}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; SSE Connection Handler
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- sse-connect-handler
  "Handler for SSE connection - syncs client to current state."
  [state-atom]
  (fn [{:keys [query-params]}]
    (let [view-param (get query-params "view")
          name-param (get query-params "name")
          library (:library @state-atom)
          ;; For deep links, temporarily set view for this connection
          initial-view (case view-param
                         "gallery" {:type :gallery}
                         "components" (let [first-component (first (sort (keys library)))]
                                        {:type :components :name first-component :example-idx 0})
                         "component" {:type :components :name (keyword name-param) :example-idx 0}
                         (:view @state-atom))
          render-state (assoc @state-atom :view initial-view)]
      {::sfere/key [:sandbox (str (random-uuid))]
       ::twk/fx [[::twk/patch-elements (views/render-view render-state)]]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View Switching Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- view-preview-handler
  "Handler to switch to preview view."
  []
  (fn [{:keys [dispatch]}]
    (dispatch [[::tsain/show-preview]])
    {::twk/with-open-sse? true}))

(defn- view-gallery-handler
  "Handler to switch to gallery view."
  []
  (fn [{:keys [dispatch]}]
    (dispatch [[::tsain/show-gallery]])
    {::twk/with-open-sse? true}))

(defn- view-components-handler
  "Handler to switch to components view."
  []
  (fn [{:keys [dispatch]}]
    (dispatch [[::tsain/show-components nil]])
    {::twk/with-open-sse? true}))

(defn- view-component-handler
  "Handler to view a specific component."
  [state-atom]
  (fn [{{:keys [name]} :path-params :keys [dispatch query-params]}]
    (let [idx (when-let [idx-str (get query-params "idx")]
                (try (Integer/parseInt idx-str) (catch Exception _ 0)))
          component-name (keyword name)]
      ;; Update state with example index, then broadcast
      (swap! state-atom assoc :view {:type :components
                                     :name component-name
                                     :example-idx (or idx 0)})
      (dispatch [[::tsain/show-components component-name]])
      {::twk/with-open-sse? true})))

(defn- toggle-sidebar-handler
  "Handler to toggle sidebar collapsed state."
  []
  (fn [{:keys [dispatch]}]
    (dispatch [[::tsain/toggle-sidebar]])
    {::twk/with-open-sse? true}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Action Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- commit-handler
  "Handler to commit current preview to library."
  []
  (fn [{:keys [signals dispatch]}]
    (let [component-name (keyword (:commitName signals))]
      (when (and component-name (not= component-name (keyword "")))
        (dispatch [[::tsain/commit component-name nil]])))
    {::twk/with-open-sse? true}))

(defn- clear-handler
  "Handler to clear preview."
  []
  (fn [{:keys [dispatch]}]
    (dispatch [[::tsain/preview-clear]])
    {::twk/with-open-sse? true}))

(defn- uncommit-handler
  "Handler to remove a component from library."
  [state-atom]
  (fn [{{:keys [name]} :path-params :keys [dispatch]}]
    (let [deleted-name (keyword name)
          library (:library @state-atom)
          current-view (:view @state-atom)
          ;; If we're in components view and deleting the current component, select another
          next-component (when (and (= :components (:type current-view))
                                    (= deleted-name (:name current-view)))
                           (let [sorted-names (vec (sort (keys library)))
                                 idx (.indexOf sorted-names deleted-name)
                                 remaining (remove #{deleted-name} sorted-names)]
                             (when (seq remaining)
                               (nth (vec remaining) (min idx (dec (count remaining)))))))]
      (dispatch [[::tsain/uncommit deleted-name]])
      ;; Stay in components view, select next component if available
      (if next-component
        (dispatch [[::tsain/show-components next-component]])
        (dispatch [[::tsain/show-components nil]])))
    {::twk/with-open-sse? true}))

(defn- get-example-hiccup
  "Get hiccup from component data, supporting both old and new formats."
  [component-data example-idx]
  (if-let [examples (:examples component-data)]
    (:hiccup (nth examples (or example-idx 0) (first examples)))
    (:hiccup component-data)))

(defn- copy-hiccup-handler
  "Handler to return formatted hiccup for clipboard copy."
  [state-atom]
  (fn [{{:keys [name]} :path-params :keys [query-params]}]
    (let [component-name (keyword name)
          idx (when-let [idx-str (get query-params "idx")]
                (try (Integer/parseInt idx-str) (catch Exception _ 0)))
          component-data (get-in @state-atom [:library component-name])
          hiccup (when component-data (get-example-hiccup component-data idx))]
      (if hiccup
        {:status 200
         :headers {"Content-Type" "text/plain; charset=utf-8"}
         :body (with-out-str (pprint/pprint hiccup))}
        {:status 404
         :headers {"Content-Type" "text/plain"}
         :body "Component not found"}))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Asset Handlers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- sandbox-css-handler
  "Serve sandbox.css from classpath (tsain library resource)."
  []
  (fn [_request]
    (if-let [resource (io/resource "tsain/sandbox.css")]
      {:status 200
       :headers {"Content-Type" "text/css; charset=utf-8"}
       :body (slurp resource)}
      {:status 404
       :headers {"Content-Type" "text/plain"}
       :body "sandbox.css not found"})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Route Factory
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn routes
  "Generate reitit route data for tsain sandbox.

  Parameters:
    dispatch   - Sandestin dispatch function
    state-atom - Tsain state atom (from registry)
    config     - Configuration map (from tsain.edn)

  Returns vector of reitit route data:
    [[path {:method handler}] ...]

  The routes expect the twk/with-datastar middleware to be applied
  and a middleware that adds :dispatch to the request.

  Example:
    (def router
      (rr/router
        (into app-routes (tsain.routes/routes dispatch state-atom config))
        {:data {:middleware [(add-dispatch-to-request dispatch)
                             (twk/with-datastar ds-hk/->sse-response dispatch)]}}))"
  [_dispatch state-atom config]
  [;; Page shells - each with appropriate initial view
   ["/sandbox" {:name ::sandbox :get (preview-page-handler config)}]
   ["/sandbox/components" {:name ::components :get (gallery-page-handler config)}]
   ["/sandbox/c/:name" {:name ::component :get (component-page-handler config)}]

   ;; SSE connection
   ["/sandbox/sse" {:name ::sse :post (sse-connect-handler state-atom)}]

   ;; View switching (from browser nav)
   ["/sandbox/view/preview" {:name ::view-preview :post (view-preview-handler)}]
   ["/sandbox/view/gallery" {:name ::view-gallery :post (view-gallery-handler)}]
   ["/sandbox/view/components" {:name ::view-components :post (view-components-handler)}]
   ["/sandbox/view/component/:name" {:name ::view-component :post (view-component-handler state-atom)}]

   ;; Sidebar toggle
   ["/sandbox/sidebar/toggle" {:name ::sidebar-toggle :post (toggle-sidebar-handler)}]

   ;; Actions
   ["/sandbox/commit" {:name ::commit :post (commit-handler)}]
   ["/sandbox/clear" {:name ::clear :post (clear-handler)}]
   ["/sandbox/uncommit/:name" {:name ::uncommit :post (uncommit-handler state-atom)}]
   ["/sandbox/copy/:name" {:name ::copy :get (copy-hiccup-handler state-atom)}]

   ;; Assets
   ["/sandbox.css" {:name ::sandbox-css :get (sandbox-css-handler)}]

   ;; Redirect root to sandbox
   ["/" {:get (fn [_] {:status 302 :headers {"location" "/sandbox"}})}]])
