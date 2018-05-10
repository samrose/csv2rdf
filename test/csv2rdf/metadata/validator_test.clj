(ns csv2rdf.metadata.validator-test
  (:require [clojure.test :refer :all]
            [csv2rdf.metadata.validator :refer :all]
            [csv2rdf.logging :as logging]
            [csv2rdf.metadata.context :as context])
  (:import [clojure.lang ExceptionInfo]
           [java.net URI]))

(deftest eq-test
  (let [v (eq "value")]
    (testing "Matches value"
      (let [{:keys [warnings result]} (logging/capture-warnings (v {} "value"))]
        (is (empty? warnings))
        (is (= "value" result))))

    (testing "Does not match value"
      (let [{:keys [warnings result]} (logging/capture-warnings (v {} "other value"))]
        (is (= 1 (count warnings)))
        (is (invalid? result))))))

(deftest type-eq-test
  (let [value "Table"
        v (type-eq value)]
    (testing "Matches value"
      (is (= value (v {} value))))

    (testing "Does not match value"
      (is (thrown? ExceptionInfo (v {} "Schema"))))))

(deftest character-test
  (is (= \c (character {} "c")))
  (is (invalid? (character {} 4)))
  (is (invalid? (character {} "too many"))))

(deftest array-of-test
  (let [v (array-of string)
        context (context/make-context (URI. "http://example"))]
    (testing "Valid array with valid elements"
      (let [arr ["foo" "bar" "baz"]
            {:keys [warnings result]} (logging/capture-warnings (v context arr))]
        (is (empty? warnings))
        (is (= arr result))))

    (testing "Array with invalid elements"
      (let [arr ["foo" 2 "bar" {} "baz" nil]
            {:keys [warnings result]} (logging/capture-warnings (v context arr))]
        (is (= 3 (count warnings)))
        (is (= ["foo" "bar" "baz"] result))))

    (testing "Non-array"
      (let [{:keys [warnings result]} (logging/capture-warnings (v context "not an array"))]
        (is (= 1 (count warnings)))
        (is (= [] result))))))

(deftest tuple-test
  (let [v (tuple string number)
        context (context/make-context (URI. "http://example"))]
    (testing "Valid array"
      (let [arr ["s" 4]
            {:keys [warnings result]} (logging/capture-warnings (v context arr))]
        (is (empty? warnings))
        (is (= arr result))))

    (testing "Array with invalid length"
      (let [{:keys [warnings result]} (logging/capture-warnings (v context ["s" 4 {} true]))]
        (is (some? (seq warnings)))
        (is (invalid? result))))

    (testing "Array with valid length and invalid elements"
      (let [{:keys [warnings result]} (logging/capture-warnings (v context [true "not a number"]))]
        (is (= 2 (count warnings)))
        (is (invalid? result))))

    (testing "Non-array"
      (let [{:keys [warnings result]} (logging/capture-warnings (v context 5))]
        (is (= 1 (count warnings)))
        (is (invalid? result))))))

(deftest nullable-test
  (let [v (nullable string)
        context (context/make-context (URI. "http://example"))]
    (testing "nil"
      (let [{:keys [warnings result]} (logging/capture-warnings (v context nil))]
        (is (empty? warnings))
        (is (nil? result))))

    (testing "Non-nil and valid for inner validator"
      (let [{:keys [warnings result]} (logging/capture-warnings (v context "s"))]
        (is (empty? warnings))
        (is (= "s" result))))

    (testing "Non-nil and invalid for inner validator"
      (let [{:keys [warnings result]} (logging/capture-warnings (v context [1,2,3]))]
        (is (some? (seq warnings)))
        (is (invalid? result))))))

(deftest try-parse-with-test
  (let [v (try-parse-with #(Integer/parseInt %))
        context (context/make-context (URI. "http://example"))]
    (testing "Valid"
      (let [{:keys [warnings result]} (logging/capture-warnings (v context "123"))]
        (is (empty? warnings))
        (is (= 123 result))))

    (testing "Invalid"
      (let [{:keys [warnings result]} (logging/capture-warnings (v context "not a number"))]
        (is (= 1 (count warnings)))
        (is (invalid? result))))))