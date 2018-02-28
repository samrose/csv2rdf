(ns csv2rdf.tabular.csv.dialect-test
  (:require [csv2rdf.tabular.csv.dialect :refer :all :as dialect]
            [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check :as tc])
  (:import [clojure.lang ExceptionInfo]))

(deftest dialect->options-trim-test
  (testing "None"
    (is (= :all (:trim-mode (dialect->options {})))))

  (testing "skipInitialSpace"
    (are [skip expected] (= expected (:trim-mode (dialect->options {:skipInitialSpace skip})))
      true :start
      false :none))

  (testing "Explicit trim"
    (let [p (prop/for-all [[trim skip] (gen/tuple (s/gen ::dialect/trim) (s/gen ::dialect/skipInitialSpace))]
                          (= trim (:trim-mode (dialect->options {:trim trim :skipInitialSpace skip}))))
          {:keys [result]} (tc/quick-check 100 p)]
      (is result))))

(deftest dialect->options-num-header-rows-test
  (testing "None"
    (is (= 1 (:num-header-rows (dialect->options {})))))

  (testing "header"
    (are [header expected] (= expected (:num-header-rows (dialect->options {:header header})))
      true 1
      false 0))

  (testing "Explcit headerRowCount"
    (let [p (prop/for-all [[header row-count] (gen/tuple gen/boolean gen/nat)]
                          (= row-count (:num-header-rows (dialect->options {:header header :headerRowCount row-count}))))
          {:keys [result]} (tc/quick-check 100 p)]
      (is result))))

(deftest dialect->options-quote-escape-char-test
  (testing "None"
    (let [{:keys [quoteChar escapeChar]} (dialect->options {})]
      (is (= \" quoteChar))
      (is (= \" escapeChar))))

  (testing "doubleQuote true without quoteChar"
    (let [{:keys [quoteChar escapeChar]} (dialect->options {:doubleQuote true})]
      (is (= \" quoteChar))
      (is (= \" escapeChar))))

  (testing "doubleQuote false without quoteChar"
    (let [{:keys [quoteChar escapeChar]} (dialect->options {:doubleQuote false})]
      (is (= \" quoteChar))
      (is (= \\ escapeChar))))

  (testing "quoteChar nil"
    (let [{:keys [quoteChar escapeChar]} (dialect->options {:quoteChar nil})]
      (is (nil? quoteChar))
      (is (nil? escapeChar))))

  (testing "doubleQuote and quoteChar set"
    (let [{:keys [quoteChar escapeChar]} (dialect->options {:quoteChar \@ :doubleQuote false})]
      (is (= \@ quoteChar))
      (is (= \\ escapeChar))))

  (testing "quoteChar nil when doubleQuote set"
    (is (thrown? ExceptionInfo (dialect->options {:quoteChar nil :doubleQuote false})))))

(defn with-instrumentation [f]
  (let [to-instrument [`calculate-dialect-options `expand-dialect]]
    (doseq [sym to-instrument]
      (stest/instrument sym))
    (try
      (f)
      (finally
        (doseq [sym to-instrument]
          (stest/unstrument sym))))))

(use-fixtures :once with-instrumentation)


