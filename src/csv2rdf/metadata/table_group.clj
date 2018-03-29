(ns csv2rdf.metadata.table-group
  (:require [csv2rdf.metadata.validator :refer [array-of eq]]
            [csv2rdf.metadata.types :refer [object-property id table-direction note contextual-object]]
            [csv2rdf.metadata.inherited :refer [metadata-of]]
            [csv2rdf.metadata.schema :as schema]
            [csv2rdf.metadata.transformation :as transformation]
            [csv2rdf.metadata.dialect :as dialect]
            [csv2rdf.metadata.table :as table]
            [csv2rdf.validation :as v]
            [csv2rdf.metadata.inherited :as inherited]))

(def table-group
  (metadata-of
    {:required {:tables (array-of table/table {:min-length 1})}
     :optional {:dialect         (object-property dialect/dialect)
                :notes           (array-of note)
                :tableDirection  table-direction
                :tableSchema     (object-property schema/schema)
                :transformations (array-of transformation/transformation)
                :id             id
                :type           (eq "TableGroup")}
     :defaults {:tableDirection "auto"}}))

(defn looks-like-table-group-json? [doc]
  (contains? doc "tables"))

(defn expand-properties
  "Expands all properties for this table group by expanding the properties of its contained tables. There is no
   parent, so any inherited properties not specified directly will use their default values."
  [table-group]
  (let [with-defaults (inherited/inherit-defaults table-group)]
    (update with-defaults :tables (fn [tables]
                                  (mapv (fn [t] (table/expand-properties with-defaults t)) tables)))))

(defn parse-table-group-json [context doc]
  (v/fmap expand-properties ((contextual-object true table-group) context doc)))

(defn from-table
  "Creates a table group from the given table"
  [table]
  (expand-properties {:tables         [table]
                      :tableDirection "auto"}))