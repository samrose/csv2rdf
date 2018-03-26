(ns csv2rdf.tabular.csv
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [csv2rdf.metadata.dialect :as dialect]
            [csv2rdf.tabular.csv.reader :as reader]
            [csv2rdf.metadata.table :as table]
            [csv2rdf.tabular.cell :as cell]
            [csv2rdf.util :as util]
            [csv2rdf.metadata.column :as column]
            [csv2rdf.metadata.uri-template-property :as template-property]))

(defn ^{:table-spec "8.6"} get-skipped-rows-comments [skipped-rows]
  (remove nil? (map (fn [{:keys [type comment content] :as row}]
                      (cond (= :comment type) comment
                            (not (.isEmpty content)) content
                            :else nil))
                    skipped-rows)))

(defn ^{:table-spec "8.7"} get-header-row-columns [header-rows]
  (let [comment-rows (filter reader/is-comment-row? header-rows)
        title-rows (remove reader/is-comment-row? header-rows)
        columns (apply mapv (fn [& titles]
                            {:titles (vec (remove string/blank? titles))})
                      (map :cells title-rows))]
    {:comments (mapv :comment comment-rows)
     :columns columns}))

(defn ^{:table-spec "8.10.3"} get-data-comments [data-rows]
  (map :comment (filter reader/is-comment-row? data-rows)))

;;TODO: section 8.10.4.5.1 - add any extra columns for rows not defined in the input table
(defn ^{:table-spec "8"} extract-embedded-metadata
  ([csv-source] (extract-embedded-metadata csv-source {:doubleQuote false}))
  ([csv-source {:keys [encoding] :as dialect}]
   (with-open [r (io/reader csv-source :encoding encoding)]
     (let [{:keys [skipRows num-header-rows] :as options} (dialect/dialect->options dialect)
           rows (reader/read-rows r options)
           [skipped-rows remaining-rows] (split-at skipRows rows)
           skipped-row-comments (get-skipped-rows-comments skipped-rows)
           [header-rows data-rows] (split-at num-header-rows remaining-rows)
           {:keys [columns] :as header} (get-header-row-columns header-rows)
           data-row-comments (get-data-comments data-rows)
           comments (vec (concat skipped-row-comments (:comments header) data-row-comments))
           metadata {:tableSchema {:columns columns}}
           metadata (if (empty? comments)
                      metadata
                      (assoc metadata :comments comments))]
       metadata))))

;;TODO: move this
(def index->row-number inc)

(defn annotate-row [row-index {:keys [source-row-number cells] :as data-row} table {:keys [skipColumns] :as csv-options}]
  (let [columns (table/columns table)
        row-number (index->row-number row-index)
        cell-values (drop skipColumns cells)

        ;;extend cells to cover any virtual columns
        ;;TODO: handle virtual and non-virtual columns separately?
        padded-cell-values (concat cell-values (repeat ""))
        parsed-cells (map cell/parse-cell padded-cell-values columns)
        column-value-bindings (into {} (map (fn [cell column]
                                              ;;TODO: need 'canonical value' according to XML schema
                                              ;;see metadata spec 5.1.3
                                              [(util/percent-decode (:name column)) (cell/semantic-value cell)])
                                            parsed-cells columns))
        row-bindings (assoc column-value-bindings :_row row-number :_sourceRow source-row-number)
        cells (map-indexed
                (fn [col-index [cell column]]
                  (let [column-number (column/index->column-number col-index)
                        bindings (assoc row-bindings :_name (util/percent-decode (:name column))
                                                     :_column column-number
                                                     :_sourceColumn (+ skipColumns column-number))
                        property-urls (into {} (remove nil? (map (fn [[k uri-template]]
                                                                   (if (some? uri-template)
                                                                     [k (template-property/resolve-uri-template-property uri-template bindings column)]))
                                                                 (select-keys column [:aboutUrl :propertyUrl :valueUrl]))))]
                    (-> cell
                        (merge property-urls)
                        (assoc :column column))))
                (map vector parsed-cells columns))]
    {:number        row-number
     :source-number source-row-number
     :cells         (vec cells)}))

(defn ^{:table-spec "8"} annotated-rows [reader table {:keys [skipRows num-header-rows skipBlankRows] :as csv-options}]
  (let [should-skip-row? (fn [{:keys [cells] :as row}] (and skipBlankRows (every? string/blank? cells)))
        rows (reader/read-rows reader csv-options)
        row-offset (+ skipRows num-header-rows)
        data-rows (remove should-skip-row? (drop row-offset rows))]
    (map-indexed (fn [row-index row]
                   (annotate-row row-index row table csv-options))
         data-rows)))

