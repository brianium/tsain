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
      (is (contains? reg ::tsain/state))
      (is (map? (::s/effects reg)))
      (is (instance? clojure.lang.Atom (::tsain/state reg))))))

(deftest registry-state-initialization
  (testing "registry initializes state atom correctly"
    (let [reg (tsain/registry {:components-file test-components-file})
          state @(::tsain/state reg)]
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
          state-atom (::tsain/state reg)
          config (::tsain/config reg)
          route-data (routes/routes nil state-atom config)]
      (is (vector? route-data))
      (is (pos? (count route-data))))))

(deftest routes-have-correct-structure
  (testing "each route has path and handler map"
    (let [reg (tsain/registry {:components-file test-components-file})
          state-atom (::tsain/state reg)
          config (::tsain/config reg)
          route-data (routes/routes nil state-atom config)]
      (doseq [[path handler-map] route-data]
        (is (string? path) (str "Path should be string: " path))
        (is (map? handler-map) (str "Handler map missing for: " path))))))

(deftest routes-include-expected-paths
  (testing "routes include core sandbox paths"
    (let [reg (tsain/registry {:components-file test-components-file})
          state-atom (::tsain/state reg)
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Discovery API Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest discovery-api-uses-default-registry
  (testing "registry function populates the default registry atom"
    ;; Create a registry - this should populate *tsain-registry
    (let [reg (tsain/registry {:components-file test-components-file})]
      ;; The atom should now be set
      (is (some? @tsain/*tsain-registry))
      (is (= reg @tsain/*tsain-registry)))))

(deftest describe-function-arities
  (testing "describe with 0 args uses default registry"
    (let [_reg (tsain/registry {:components-file test-components-file})
          result (tsain/describe)]
      ;; Should return a seq (possibly empty)
      (is (seqable? result))))

  (testing "describe with 1 arg (tag) uses default registry"
    (let [_reg (tsain/registry {:components-file test-components-file})
          result (tsain/describe :nonexistent-component)]
      ;; Should return a map with :tag
      (is (map? result))
      (is (= :nonexistent-component (:tag result)))))

  (testing "describe with 2 args uses explicit registry"
    (let [reg (tsain/registry {:components-file test-components-file})
          result (tsain/describe reg :nonexistent-component)]
      (is (map? result))
      (is (= :nonexistent-component (:tag result))))))

(deftest grep-function-arities
  (testing "grep with 1 arg uses default registry"
    (let [_reg (tsain/registry {:components-file test-components-file})
          result (tsain/grep "nonexistent")]
      ;; Should return a seq (possibly empty)
      (is (seqable? result))))

  (testing "grep with 2 args uses explicit registry"
    (let [reg (tsain/registry {:components-file test-components-file})
          result (tsain/grep reg "nonexistent")]
      (is (seqable? result)))))

(deftest props-function-arities
  (testing "props with 1 arg uses default registry"
    (let [_reg (tsain/registry {:components-file test-components-file})
          result (tsain/props :variant)]
      ;; Should return a seq (possibly empty)
      (is (seqable? result))))

  (testing "props with 2 args uses explicit registry"
    (let [reg (tsain/registry {:components-file test-components-file})
          result (tsain/props reg :variant)]
      (is (seqable? result)))))

(deftest categories-function-arities
  (testing "categories with 0 args uses default registry"
    (let [_reg (tsain/registry {:components-file test-components-file})
          result (tsain/categories)]
      ;; Should return a seq (possibly empty)
      (is (seqable? result))))

  (testing "categories with 1 arg uses explicit registry"
    (let [reg (tsain/registry {:components-file test-components-file})
          result (tsain/categories reg)]
      (is (seqable? result)))))

(deftest by-category-function-arities
  (testing "by-category with 1 arg uses default registry"
    (let [_reg (tsain/registry {:components-file test-components-file})
          result (tsain/by-category "cards")]
      ;; Should return a seq (possibly empty)
      (is (seqable? result))))

  (testing "by-category with 2 args uses explicit registry"
    (let [reg (tsain/registry {:components-file test-components-file})
          result (tsain/by-category reg "cards")]
      (is (seqable? result)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; CSS Utility Tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(require '[ascolais.tsain.css :as css])

(deftest find-rule-end-line-test
  (testing "simple single-line rule"
    (is (= 1 (css/find-rule-end-line ".foo { color: red; }" 1))))

  (testing "multi-line rule"
    (let [css ".card {\n  color: red;\n  background: blue;\n}"]
      (is (= 4 (css/find-rule-end-line css 1)))))

  (testing "rule with braces in string content"
    (let [css ".icon::before {\n  content: \"}\";\n  color: red;\n}"]
      (is (= 4 (css/find-rule-end-line css 1)))))

  (testing "rule with comment containing braces"
    (let [css ".card {\n  /* } fake brace */\n  color: red;\n}"]
      (is (= 4 (css/find-rule-end-line css 1)))))

  (testing "rule at end of file without trailing newline"
    (let [css ".header { color: red; }\n.footer { color: blue; }"]
      (is (= 2 (css/find-rule-end-line css 2)))))

  (testing "multiple rules - finds correct end"
    (let [css ".first {\n  color: red;\n}\n\n.second {\n  color: blue;\n}"]
      (is (= 3 (css/find-rule-end-line css 1)))
      (is (= 7 (css/find-rule-end-line css 5))))))

(deftest remove-rules-test
  (testing "removes single rule"
    (let [css ".keep { color: red; }\n\n.remove {\n  color: blue;\n}\n\n.also-keep { color: green; }"
          parsed (css/parse-stylesheet css)
          rules-to-remove (css/find-rules-by-pattern parsed ".remove")
          [remaining _removed] (css/remove-rules css rules-to-remove)]
      (is (clojure.string/includes? remaining ".keep { color: red; }"))
      (is (clojure.string/includes? remaining ".also-keep { color: green; }"))
      (is (not (clojure.string/includes? remaining ".remove")))))

  (testing "removes multiple non-adjacent rules"
    (let [css ".card {\n  color: red;\n}\n\n.button {\n  color: blue;\n}\n\n.card-header {\n  color: green;\n}"
          parsed (css/parse-stylesheet css)
          rules-to-remove (css/find-rules-by-pattern parsed ".card")
          [remaining _removed] (css/remove-rules css rules-to-remove)]
      (is (clojure.string/includes? remaining ".button"))
      (is (not (clojure.string/includes? remaining ".card")))
      (is (not (clojure.string/includes? remaining ".card-header")))))

  (testing "removes adjacent rules"
    (let [css ".card {\n  color: red;\n}\n.card-body {\n  color: blue;\n}\n\n.other { color: green; }"
          parsed (css/parse-stylesheet css)
          rules-to-remove (css/find-rules-by-pattern parsed ".card")
          [remaining _removed] (css/remove-rules css rules-to-remove)]
      (is (clojure.string/includes? remaining ".other"))
      (is (not (clojure.string/includes? remaining ".card")))
      (is (not (clojure.string/includes? remaining ".card-body")))))

  (testing "handles empty rules-to-remove"
    (let [css ".keep { color: red; }"
          [remaining _removed] (css/remove-rules css [])]
      (is (= ".keep { color: red; }\n" remaining))))

  (testing "returns removed css"
    (let [css ".keep { color: red; }\n\n.remove {\n  color: blue;\n}"
          parsed (css/parse-stylesheet css)
          rules-to-remove (css/find-rules-by-pattern parsed ".remove")
          [_remaining removed] (css/remove-rules css rules-to-remove)]
      (is (clojure.string/includes? removed ".remove"))
      (is (clojure.string/includes? removed "color: blue")))))
