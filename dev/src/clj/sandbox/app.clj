(ns sandbox.app
  (:require [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [ascolais.tsain :as tsain]
            [clojure.pprint :as pprint]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [starfederation.datastar.clojure.adapter.http-kit :as ds-hk]
            [org.httpkit.server :as hk]
            [reitit.ring :as rr]
            [sandbox.system :as system]
            [sandbox.views :as views]
            [sandbox.watcher :as watcher]))

(defn- create-store []
  (sfere/store {:type :caffeine
                :duration-ms 1800000  ;; 30 minutes for dev
                :expiry-mode :sliding}))

(defn- create-dispatch [store tsain-registry]
  (s/create-dispatch
   [(twk/registry)
    (sfere/registry store)
    tsain-registry]))

(defn- wrap-request-logging [handler]
  (fn [request]
    (tap> {:uri (:uri request)
           :method (:request-method request)})
    (handler request)))

;; Page handlers - return shell, content loaded via SSE
(defn preview-page [_request]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/sandbox-page)})

(defn gallery-page [_request]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/sandbox-page :components)})

(defn component-page [{{:keys [name]} :path-params}]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/sandbox-page [:component (keyword name)])})

;; SSE connection - syncs client to current state or initial view from URL
(defn sse-connect [{:keys [query-params]}]
  (let [state-atom (:state system/*system*)
        view-param (get query-params "view")
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
     ::twk/fx [[::twk/patch-elements (views/render-view render-state)]]}))

;; View switching handlers
(defn view-preview [{:keys [dispatch]}]
  (dispatch [[::tsain/show-preview]])
  {::twk/with-open-sse? true})

(defn view-gallery [{:keys [dispatch]}]
  (dispatch [[::tsain/show-gallery]])
  {::twk/with-open-sse? true})

(defn view-components [{:keys [dispatch]}]
  (dispatch [[::tsain/show-components nil]])
  {::twk/with-open-sse? true})

(defn view-component [{{:keys [name]} :path-params :keys [dispatch query-params]}]
  (let [idx (when-let [idx-str (get query-params "idx")]
              (try (Integer/parseInt idx-str) (catch Exception _ 0)))
        state-atom (:state system/*system*)
        component-name (keyword name)]
    ;; Update state with example index, then broadcast
    (swap! state-atom assoc :view {:type :components
                                   :name component-name
                                   :example-idx (or idx 0)})
    (dispatch [[::tsain/show-components component-name]])
    {::twk/with-open-sse? true}))

(defn toggle-sidebar [{:keys [dispatch]}]
  (dispatch [[::tsain/toggle-sidebar]])
  {::twk/with-open-sse? true})

;; Action handlers
(defn commit-handler [{:keys [signals dispatch]}]
  (let [component-name (keyword (:commitName signals))]
    (when (and component-name (not= component-name (keyword "")))
      (dispatch [[::tsain/commit component-name nil]])))
  {::twk/with-open-sse? true})

(defn clear-handler [{:keys [dispatch]}]
  (dispatch [[::tsain/preview-clear]])
  {::twk/with-open-sse? true})

(defn uncommit-handler [{{:keys [name]} :path-params :keys [dispatch]}]
  (let [state-atom (:state system/*system*)
        deleted-name (keyword name)
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
  {::twk/with-open-sse? true})

(defn- get-example-hiccup
  "Get hiccup from component data, supporting both old and new formats."
  [component-data example-idx]
  (if-let [examples (:examples component-data)]
    (:hiccup (nth examples (or example-idx 0) (first examples)))
    (:hiccup component-data)))

(defn copy-hiccup-handler
  "Return formatted hiccup for a component as plain text."
  [{{:keys [name]} :path-params :keys [query-params]}]
  (let [state-atom (:state system/*system*)
        component-name (keyword name)
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
       :body "Component not found"})))

(defn- add-dispatch-to-request [dispatch]
  (fn [handler]
    (fn [request]
      (handler (assoc request :dispatch dispatch)))))

(def routes
  [;; Page shells - each with appropriate initial view
   ["/sandbox" {:name ::sandbox :get preview-page}]
   ["/sandbox/components" {:name ::components :get gallery-page}]
   ["/sandbox/c/:name" {:name ::component :get component-page}]

   ;; SSE connection
   ["/sandbox/sse" {:name ::sse :post sse-connect}]

   ;; View switching (from browser nav)
   ["/sandbox/view/preview" {:name ::view-preview :post view-preview}]
   ["/sandbox/view/gallery" {:name ::view-gallery :post view-gallery}]
   ["/sandbox/view/components" {:name ::view-components :post view-components}]
   ["/sandbox/view/component/:name" {:name ::view-component :post view-component}]

   ;; Sidebar toggle
   ["/sandbox/sidebar/toggle" {:name ::sidebar-toggle :post toggle-sidebar}]

   ;; Actions
   ["/sandbox/commit" {:name ::commit :post commit-handler}]
   ["/sandbox/clear" {:name ::clear :post clear-handler}]
   ["/sandbox/uncommit/:name" {:name ::uncommit :post uncommit-handler}]
   ["/sandbox/copy/:name" {:name ::copy :get copy-hiccup-handler}]

   ;; Redirect root to sandbox
   ["/" {:get (fn [_] {:status 302 :headers {"location" "/sandbox"}})}]])

(defn- create-router [dispatch]
  (rr/router
   routes
   {:data {:middleware [(add-dispatch-to-request dispatch)
                        (twk/with-datastar ds-hk/->sse-response dispatch)]}}))

(defn- create-app [dispatch]
  (-> (rr/ring-handler
       (create-router dispatch)
       (rr/create-default-handler))
      wrap-params
      (wrap-resource "public")
      wrap-request-logging))

(defn start-system
  ([] (start-system 3000))
  ([port]
   (let [tsain-reg (tsain/registry {:port port})
         state-atom (::s/state tsain-reg)
         store      (create-store)
         dispatch   (create-dispatch store tsain-reg)
         handler    (create-app dispatch)
         server     (hk/run-server handler {:port port})
         watch      (watcher/watcher
                     {:paths ["dev/resources/public"]})]
     (alter-var-root #'system/*system*
                     (constantly {:state    state-atom
                                  :store    store
                                  :dispatch dispatch
                                  :server   server
                                  :watcher  watch}))
     (println (str "Sandbox running at http://localhost:" port "/sandbox"))
     (when watch
       (println "Watching dev/resources/public for changes"))
     system/*system*)))

(defn stop-system []
  (when-let [{:keys [server watcher]} system/*system*]
    (watcher/stop! watcher)
    (server)
    (alter-var-root #'system/*system* (constantly nil))
    (println "Sandbox stopped")))
