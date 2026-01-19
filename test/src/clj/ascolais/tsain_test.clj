(ns ascolais.tsain-test
  (:require [clojure.test :refer [deftest is testing]]
            [ascolais.tsain :as tsain]))

(deftest greet-test
  (testing "greet returns a greeting message"
    (is (= "Hello, World!" (tsain/greet "World")))
    (is (= "Hello, Clojure!" (tsain/greet "Clojure")))))
