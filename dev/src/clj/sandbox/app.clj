(ns sandbox.app
  (:require [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [ring.middleware.resource :refer [wrap-resource]]
            [starfederation.datastar.clojure.adapter.http-kit :as ds-hk]
            [org.httpkit.server :as hk]
            [reitit.ring :as rr]
            [sandbox.state :as state]
            [sandbox.registry :as registry]
            [sandbox.system :as system]
            [sandbox.views :as views]
            [sandbox.watcher :as watcher]))

(defn- create-store []
  (sfere/store {:type :caffeine
                :duration-ms 1800000  ;; 30 minutes for dev
                :expiry-mode :sliding}))

(defn- create-dispatch [store state-atom]
  (s/create-dispatch
   [(twk/registry)
    (sfere/registry store)
    (registry/registry state-atom)]))

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
   :body (views/sandbox-page :gallery)})

(defn component-page [{{:keys [name]} :path-params}]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/sandbox-page [:component (keyword name)])})

;; SSE connection - syncs client to current state or initial view from URL
(defn sse-connect [{:keys [query-params]}]
  (let [state-atom (:state system/*system*)
        view-param (get query-params "view")
        name-param (get query-params "name")
        ;; For deep links, temporarily set view for this connection
        initial-view (case view-param
                       "gallery" {:type :gallery}
                       "component" {:type :component :name (keyword name-param)}
                       (:view @state-atom))
        render-state (assoc @state-atom :view initial-view)]
    {::sfere/key [:sandbox (str (random-uuid))]
     ::twk/fx [[::twk/patch-elements (views/render-view render-state)]]}))

;; View switching handlers
(defn view-preview [{:keys [dispatch]}]
  (dispatch [[::registry/show-preview]])
  {::twk/with-open-sse? true})

(defn view-gallery [{:keys [dispatch]}]
  (dispatch [[::registry/show-gallery]])
  {::twk/with-open-sse? true})

(defn view-component [{{:keys [name]} :path-params :keys [dispatch]}]
  (dispatch [[::registry/show (keyword name)]])
  {::twk/with-open-sse? true})

;; Action handlers
(defn commit-handler [{:keys [signals dispatch]}]
  (let [component-name (keyword (:commitName signals))]
    (when (and component-name (not= component-name (keyword "")))
      (dispatch [[::registry/commit component-name nil]])))
  {::twk/with-open-sse? true})

(defn clear-handler [{:keys [dispatch]}]
  (dispatch [[::registry/preview-clear]])
  {::twk/with-open-sse? true})

(defn uncommit-handler [{{:keys [name]} :path-params :keys [dispatch]}]
  (dispatch [[::registry/uncommit (keyword name)]])
  (dispatch [[::registry/show-gallery]])
  {::twk/with-open-sse? true})

(defn- add-dispatch-to-request [dispatch]
  (fn [handler]
    (fn [request]
      (handler (assoc request :dispatch dispatch)))))

(def routes
  [;; Page shells - each with appropriate initial view
   ["/sandbox" {:name ::sandbox :get preview-page}]
   ["/sandbox/components" {:name ::gallery :get gallery-page}]
   ["/sandbox/c/:name" {:name ::component :get component-page}]

   ;; SSE connection
   ["/sandbox/sse" {:name ::sse :post sse-connect}]

   ;; View switching (from browser nav)
   ["/sandbox/view/preview" {:name ::view-preview :post view-preview}]
   ["/sandbox/view/gallery" {:name ::view-gallery :post view-gallery}]
   ["/sandbox/view/component/:name" {:name ::view-component :post view-component}]

   ;; Actions
   ["/sandbox/commit" {:name ::commit :post commit-handler}]
   ["/sandbox/clear" {:name ::clear :post clear-handler}]
   ["/sandbox/uncommit/:name" {:name ::uncommit :post uncommit-handler}]

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
      (wrap-resource "public")
      wrap-request-logging))

(defn start-system
  ([] (start-system 3000))
  ([port]
   (let [state-atom (state/initial-state)
         store      (create-store)
         dispatch   (create-dispatch store state-atom)
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
