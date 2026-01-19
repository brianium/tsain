(ns sandbox.app
  (:require [ascolais.sandestin :as s]
            [ascolais.twk :as twk]
            [ascolais.sfere :as sfere]
            [ring.middleware.resource :refer [wrap-resource]]
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

(defn- create-dispatch [store]
  (s/create-dispatch
   [(twk/registry)
    (sfere/registry store)]))

(defn- wrap-request-logging [handler]
  (fn [request]
    (tap> {:uri (:uri request)
           :method (:request-method request)})
    (handler request)))

(defn index [_request]
  {:status 200
   :headers {"content-type" "text/html"}
   :body (views/sandbox-page)})

(defn sse-connect [_request]
  {::sfere/key [:sandbox (str (random-uuid))]
   ::twk/fx [[::twk/patch-elements
              [:div#status "Connected - ready for REPL updates"]]]})

(def routes
  [["/" {:name ::index :get index}]
   ["/sse" {:name ::sse :post sse-connect}]])

(defn- create-router [dispatch]
  (rr/router
   routes
   {:data {:middleware [(twk/with-datastar ds-hk/->sse-response dispatch)]}}))

(defn- create-app [dispatch]
  (-> (rr/ring-handler
       (create-router dispatch)
       (rr/create-default-handler))
      (wrap-resource "public")
      wrap-request-logging))

(defn start-system
  ([] (start-system 3000))
  ([port]
   (let [store    (create-store)
         dispatch (create-dispatch store)
         handler  (create-app dispatch)
         server   (hk/run-server handler {:port port})
         watch    (watcher/watcher
                   {:paths ["dev/resources/public"]})]
     (alter-var-root #'system/*system*
                     (constantly {:store    store
                                  :dispatch dispatch
                                  :server   server
                                  :watcher  watch}))
     (println (str "Sandbox running at http://localhost:" port))
     (when watch
       (println "Watching dev/resources/public for changes"))
     system/*system*)))

(defn stop-system []
  (when-let [{:keys [server watcher]} system/*system*]
    (watcher/stop! watcher)
    (server)
    (alter-var-root #'system/*system* (constantly nil))
    (println "Sandbox stopped")))
