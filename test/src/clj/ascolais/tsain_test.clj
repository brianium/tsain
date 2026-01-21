(ns ascolais.tsain-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [ascolais.tsain :as tsain]
            [ascolais.sandestin :as s]
            [clojure.java.io :as io]
            [malli.core :as m]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test Fixtures
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def test-components-file "test-resources/test-components.edn")

(defn clean-test-files [f]
  (let [test-file (io/file test-components-file)]
    (when (.exists test-file)
      (.delete test-file)))
  (f)
  (let [test-file (io/file test-components-file)]
    (when (.exists test-file)
      (.delete test-file))))

(use-fixtures :each clean-test-files)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Configuration Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest load-config-test
  (testing "load-config returns defaults when no tsain.edn exists"
    ;; This test assumes we're in a directory without tsain.edn OR
    ;; that tsain.edn exists - either way it should return valid config
    (let [config (tsain/load-config)]
      (is (contains? config :ui-namespace))
      (is (contains? config :components-file))
      (is (contains? config :stylesheet))
      (is (contains? config :port))))

  (testing "load-config merges overrides"
    (let [config (tsain/load-config {:port 4000 :custom-key "custom-value"})]
      (is (= 4000 (:port config)))
      (is (= "custom-value" (:custom-key config))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Registry Factory Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest registry-returns-valid-sandestin-registry
  (testing "registry returns a map with required sandestin keys"
    (let [reg (tsain/registry {:components-file test-components-file})]
      (is (map? reg))
      (is (contains? reg ::s/effects))
      (is (contains? reg ::s/state))
      (is (map? (::s/effects reg)))
      (is (instance? clojure.lang.Atom (::s/state reg))))))

(deftest registry-state-initialization
  (testing "registry initializes state atom correctly"
    (let [reg (tsain/registry {:components-file test-components-file})
          state @(::s/state reg)]
      (is (map? state))
      (is (contains? state :preview))
      (is (contains? state :view))
      (is (contains? state :library))
      (is (= {:type :preview} (:view state)))
      (is (= {:hiccup nil} (:preview state))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Effect Registration Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def expected-effects
  #{::tsain/preview
    ::tsain/preview-append
    ::tsain/preview-clear
    ::tsain/commit
    ::tsain/uncommit
    ::tsain/show
    ::tsain/show-gallery
    ::tsain/show-preview
    ::tsain/sync-view
    ::tsain/patch-signals
    ::tsain/toggle-sidebar
    ::tsain/show-components})

(deftest all-effects-registered
  (testing "all expected effects are registered"
    (let [reg (tsain/registry {:components-file test-components-file})
          registered-effects (set (keys (::s/effects reg)))]
      (doseq [effect expected-effects]
        (is (contains? registered-effects effect)
            (str "Missing effect: " effect))))))

(deftest effects-have-descriptions
  (testing "all effects have non-empty descriptions"
    (let [reg (tsain/registry {:components-file test-components-file})
          effects (::s/effects reg)]
      (doseq [[effect-key effect-data] effects]
        (is (string? (::s/description effect-data))
            (str "Effect " effect-key " missing description"))
        (is (not (empty? (::s/description effect-data)))
            (str "Effect " effect-key " has empty description"))))))

(deftest effects-have-handlers
  (testing "all effects have function handlers"
    (let [reg (tsain/registry {:components-file test-components-file})
          effects (::s/effects reg)]
      (doseq [[effect-key effect-data] effects]
        (is (fn? (::s/handler effect-data))
            (str "Effect " effect-key " missing handler"))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema Validation Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest effects-have-valid-schemas
  (testing "all effects have valid Malli schemas"
    (let [reg (tsain/registry {:components-file test-components-file})
          effects (::s/effects reg)]
      (doseq [[effect-key effect-data] effects]
        (let [schema (::s/schema effect-data)]
          (is (some? schema)
              (str "Effect " effect-key " missing schema"))
          (is (try
                (m/schema schema)
                true
                (catch Exception e
                  (println "Invalid schema for" effect-key ":" (.getMessage e))
                  false))
              (str "Effect " effect-key " has invalid schema")))))))

(deftest schema-validation-rejects-invalid-inputs
  (testing "preview schema rejects invalid inputs"
    (let [schema (::s/schema (::tsain/preview (::s/effects (tsain/registry {:components-file test-components-file}))))]
      ;; Valid inputs
      (is (m/validate schema [::tsain/preview [:div "hello"]]))
      (is (m/validate schema [::tsain/preview [:h1 {:class "title"} "Title"]]))
      ;; Invalid inputs
      (is (not (m/validate schema [:wrong/key [:div "hello"]])))
      (is (not (m/validate schema [::tsain/preview])))))  ; missing hiccup

  (testing "commit schema validates component name"
    (let [schema (::s/schema (::tsain/commit (::s/effects (tsain/registry {:components-file test-components-file}))))]
      ;; Valid inputs
      (is (m/validate schema [::tsain/commit :my-card nil]))
      (is (m/validate schema [::tsain/commit :my-card "description"]))
      (is (m/validate schema [::tsain/commit :my-card {:description "desc"}]))
      ;; Invalid - not a keyword
      (is (not (m/validate schema [::tsain/commit "my-card" nil]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Schema Sample Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest hiccup-schema-has-gen-elements
  (testing "HiccupSchema has :gen/elements for sample generation"
    (let [schema-props (m/properties tsain/HiccupSchema)]
      (is (contains? schema-props :gen/elements))
      (is (vector? (:gen/elements schema-props)))
      (is (pos? (count (:gen/elements schema-props)))))))

(deftest component-name-schema-has-gen-elements
  (testing "ComponentNameSchema has :gen/elements"
    (let [schema-props (m/properties tsain/ComponentNameSchema)]
      (is (contains? schema-props :gen/elements))
      (is (every? keyword? (:gen/elements schema-props))))))

(deftest signal-map-schema-has-gen-elements
  (testing "SignalMapSchema has :gen/elements"
    (let [schema-props (m/properties tsain/SignalMapSchema)]
      (is (contains? schema-props :gen/elements))
      (is (every? map? (:gen/elements schema-props))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Integration Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest registry-can-be-used-with-create-dispatch
  (testing "registry can be composed into a dispatch function"
    (let [reg (tsain/registry {:components-file test-components-file})
          ;; We can't fully test dispatch without twk/sfere registries,
          ;; but we can verify the registry structure is compatible
          effects (::s/effects reg)]
      (is (every? (fn [[_ v]]
                    (and (contains? v ::s/schema)
                         (contains? v ::s/handler)
                         (contains? v ::s/description)))
                  effects)))))

(deftest registry-includes-config
  (testing "registry stores config for route factory access"
    (let [reg (tsain/registry {:components-file test-components-file :port 4000})]
      (is (contains? reg ::tsain/config))
      (is (= 4000 (get-in reg [::tsain/config :port]))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Route Factory Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(require '[ascolais.tsain.routes :as routes])

(deftest routes-returns-vector
  (testing "routes returns a vector of route definitions"
    (let [reg (tsain/registry {:components-file test-components-file})
          state-atom (::s/state reg)
          config (::tsain/config reg)
          route-data (routes/routes nil state-atom config)]
      (is (vector? route-data))
      (is (pos? (count route-data))))))

(deftest routes-have-correct-structure
  (testing "each route has path and handler map"
    (let [reg (tsain/registry {:components-file test-components-file})
          state-atom (::s/state reg)
          config (::tsain/config reg)
          route-data (routes/routes nil state-atom config)]
      (doseq [[path handler-map] route-data]
        (is (string? path) (str "Path should be string: " path))
        (is (map? handler-map) (str "Handler map missing for: " path))))))

(deftest routes-include-expected-paths
  (testing "routes include core sandbox paths"
    (let [reg (tsain/registry {:components-file test-components-file})
          state-atom (::s/state reg)
          config (::tsain/config reg)
          route-data (routes/routes nil state-atom config)
          paths (set (map first route-data))]
      (is (contains? paths "/sandbox"))
      (is (contains? paths "/sandbox/sse"))
      (is (contains? paths "/sandbox/c/:name"))
      (is (contains? paths "/sandbox/commit"))
      (is (contains? paths "/sandbox/copy/:name"))
      (is (contains? paths "/sandbox.css")))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; View Rendering Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(require '[ascolais.tsain.views :as views])

(deftest render-view-returns-hiccup
  (testing "render-view returns hiccup structure"
    (let [state {:preview {:hiccup [:div "test"]}
                 :view {:type :preview}
                 :library {}
                 :sidebar-collapsed? false}
          result (views/render-view state)]
      (is (vector? result))
      (is (= :div#app (first result))))))

(deftest sandbox-page-returns-full-html
  (testing "sandbox-page returns doctype + html structure"
    (let [result (views/sandbox-page)]
      (is (vector? result))
      ;; Structure should be [doctype [:html ...]]
      (is (= 2 (count result)))
      (is (= :html (first (second result)))))))

(deftest render-view-handles-all-view-types
  (testing "render-view handles preview view"
    (let [state {:preview {:hiccup [:div "test"]}
                 :view {:type :preview}
                 :library {}
                 :sidebar-collapsed? false}]
      (is (vector? (views/render-view state)))))

  (testing "render-view handles gallery view"
    (let [state {:preview {:hiccup nil}
                 :view {:type :gallery}
                 :library {}
                 :sidebar-collapsed? false}]
      (is (vector? (views/render-view state)))))

  (testing "render-view handles components view"
    (let [state {:preview {:hiccup nil}
                 :view {:type :components :name :test-card :example-idx 0}
                 :library {:test-card {:description "Test"
                                       :examples [{:label "Default"
                                                   :hiccup [:div "test"]}]}}
                 :sidebar-collapsed? false}]
      (is (vector? (views/render-view state))))))
