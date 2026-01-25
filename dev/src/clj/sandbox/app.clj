(ns sandbox.app
  "Sandbox application - dogfoods tsain library exports.

  Demonstrates how a consumer project integrates tsain:
  1. Create tsain registry and extract state/config
  2. Create dispatch with tsain + twk + sfere registries
  3. Generate routes from tsain.routes
  4. Wire up middleware and server"
  (:require [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [ascolais.tsain :as tsain]
            [ascolais.tsain.routes :as tsain.routes]
            [clojure.string :as str]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.params :refer [wrap-params]]
            [starfederation.datastar.clojure.adapter.http-kit :as ds-hk]
            [org.httpkit.server :as hk]
            [reitit.ring :as rr]
            [sandbox.system :as system]
            [sandbox.watcher :as watcher]
            ;; Require sandbox.ui to register chassis aliases
            sandbox.ui))

(defn- create-store []
  (sfere/store {:type :caffeine
                :duration-ms 1800000  ;; 30 minutes for dev
                :expiry-mode :sliding}))

(defn- create-dispatch [store tsain-registry]
  (s/create-dispatch
   [(twk/registry)
    (sfere/registry store)
    tsain-registry]))

(defn- wrap-no-cache-css
  "Disable browser caching for CSS files in development.
  Ensures @import'ed stylesheets are re-fetched when parent is reloaded."
  [handler]
  (fn [request]
    (let [response (handler request)]
      (if (and response
               (some-> (:uri request) (str/ends-with? ".css")))
        (assoc-in response [:headers "Cache-Control"]
                  "no-cache, no-store, must-revalidate")
        response))))

(defn- wrap-request-logging [handler]
  (fn [request]
    (tap> {:uri (:uri request)
           :method (:request-method request)})
    (handler request)))

(defn- add-dispatch-to-request [dispatch]
  (fn [handler]
    (fn [request]
      (handler (assoc request :dispatch dispatch)))))

(defn- create-router [dispatch state-atom config]
  (rr/router
   (tsain.routes/routes dispatch state-atom config)
   {:data {:middleware [(add-dispatch-to-request dispatch)
                        (twk/with-datastar ds-hk/->sse-response dispatch)]}}))

(defn- create-app [dispatch state-atom config]
  (-> (rr/ring-handler
       (create-router dispatch state-atom config)
       (rr/create-default-handler))
      wrap-params
      (wrap-resource "public")
      wrap-no-cache-css
      wrap-request-logging))

(defn start-system
  ([] (start-system 3000))
  ([port]
   (let [tsain-reg  (tsain/registry {:port port})
         state-atom (::tsain/state tsain-reg)
         config     (::tsain/config tsain-reg)
         store      (create-store)
         dispatch   (create-dispatch store tsain-reg)
         handler    (create-app dispatch state-atom config)
         server     (hk/run-server handler {:port port})
         watch      (watcher/watcher
                     {:paths ["dev/resources/public" "resources/tsain"]})]
     (alter-var-root #'system/*system*
                     (constantly {:state    state-atom
                                  :config   config
                                  :store    store
                                  :dispatch dispatch
                                  :server   server
                                  :watcher  watch}))
     (println (str "Sandbox running at http://localhost:" port "/sandbox"))
     (when watch
       (println "Watching for CSS changes"))
     system/*system*)))

(defn stop-system []
  (when-let [{:keys [server watcher]} system/*system*]
    (watcher/stop! watcher)
    (server)
    (alter-var-root #'system/*system* (constantly nil))
    (println "Sandbox stopped")))
