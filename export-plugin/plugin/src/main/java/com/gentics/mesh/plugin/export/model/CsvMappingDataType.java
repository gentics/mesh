package com.gentics.mesh.plugin.export.model;

/**
 * The data type for csv mappings
 */
public enum CsvMappingDataType {
    /**
     * Exports the content of a string field without any formatting.
     */
    STRING(false),
    /**
     * Exports the content into a defined template string by replacing
     * the token '{{value}}' and returning the result.
     */
    TEMPLATE(false),
    /**
     * Matches the value (converts it to string if needed) and replaces
     * the values in the `regexpTemplate` field with the selected values.
     */
    REGEXP(false),
    /**
     * Exports the content of a boolean field as "1"/"0" strings.
     */
    BOOLEAN(false),
    /**
     * Exports the content of a number field simply converted to a number.
     */
    NUMBER(false),
    /**
     * Exports the content of a date field formatted as Excel-Compatible string
     * or via a format determined via the mapping.
     */
    DATE(false),
    /**
     * Exports the content of a string field and attempts to translate the string
     * with the values of a specified setting-name.
     */
    CONFIG_VALUE(false),
    /**
     * Exports the content of a list field.
     * Allows the content to be expanded over multiple rows.
     * Requires additional type via mapping.
     */
    LIST(false),
    /**
     * Exports the content of a field inside a micronode or node field.
     * Further details have to be provided via the sub-mapping.
     */
    SUB_NODE(true),
    /**
     * Exports the content of a field inside a foreign-referenced node.
     * Further details have to be provided via the sub-mapping.
     */
    FOREIGN_REFERENCE(true);

    private boolean _isMultiColumn;

    CsvMappingDataType(boolean isMultiColumm) {
        this._isMultiColumn = isMultiColumm;
    }

    public boolean isMultiColumn() {
        return _isMultiColumn;
    }
}
