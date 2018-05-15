(ns csv2rdf.metadata.column-test
  (:require [clojure.test :refer :all]
            [csv2rdf.metadata.column :refer :all]
            [csv2rdf.metadata.validator :refer [invalid?]]
            [csv2rdf.logging :as logging]
            [csv2rdf.metadata.test-common :refer [test-context validation-error]]))

(deftest column-name-test
  (testing "valid column name"
    (let [col-name "description"
          {:keys [warnings result]} (logging/capture-warnings (column-name test-context col-name))]
      (is (empty? warnings))
      (is (= col-name result))))

  (testing "starts with underscore"
    (let [{:keys [warnings result]} (logging/capture-warnings (column-name test-context "_invalid"))]
      (is (= 1 (count warnings)))
      (is (invalid? result))))

  (testing "invalid template variable"
    (let [{:keys [warnings result]} (logging/capture-warnings (column-name test-context "not a valid variable name"))]
      (is (= 1 (count warnings)))
      (is (invalid? result))))

  (testing "invalid type"
    (let [{:keys [warnings result]} (logging/capture-warnings (column-name test-context ["not" "a" "string"]))]
      (is (= 1 (count warnings)))
      (is (invalid? result)))))

(deftest columns-test
  (testing "valid columns"
    (let [{:keys [warnings result]} (logging/capture-warnings (columns test-context [{"name" "col1"}
                                                                                     {"name" "col2" "titles" "title" "virtual" false}
                                                                                     {"name" "col3" "virtual" true}]))]
      (is (empty? warnings))
      (is (= [{:name "col1"}
              {:name "col2" :titles {"und" ["title"]} :virtual false}
              {:name "col3" :virtual true}]
             result))))

  (testing "duplicate column naes"
    (is (validation-error (columns test-context [{"name" "col" "titles" "title"}
                                                 {"name" "col" "titles" "other"}]))))

  (testing "non-virtual column following virtual column"
    (is (validation-error (columns test-context [{"name" "col1" "titles" "title"}
                                                 {"name" "col2" "virtual" true}
                                                 {"name" "col3"}])))))


