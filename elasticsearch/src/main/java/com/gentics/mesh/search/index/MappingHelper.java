package com.gentics.mesh.search.index;

import io.vertx.core.json.JsonObject;

/**
 * Utility class which contains some methods and constants that can be used to build ES index mappings.
 */
public final class MappingHelper {

	// Common keys
	public static final String UUID_KEY = "uuid";
	public static final String NAME_KEY = "name";
	public static final String DESCRIPTION_KEY = "description";

	// Field Types
	public static final String OBJECT = "object";
	public static final String NESTED = "nested";
	public static final String STRING = "string";
	public static final String BOOLEAN = "boolean";
	public static final String DATE = "date";
	public static final String LONG = "long";
	public static final String DOUBLE = "double";

	// Index Types
	public static final String NOT_ANALYZED = "not_analyzed";
	public static final String ANALYZED = "analyzed";

	// Analyzer
	public static final String TRIGRAM_ANALYZER = "trigrams";

	/**
	 * Return a JSON mapping field type.
	 * 
	 * @param type
	 *            Type of the field
	 * @param indexType
	 *            Index type of the field
	 * @param analyzer
	 *            Name of the analyzer to be used
	 * @return
	 */
	public static JsonObject fieldType(String type, String indexType, String analyzer) {
		JsonObject indexFieldInfo = new JsonObject();
		indexFieldInfo.put("type", type);
		indexFieldInfo.put("index", indexType);
		indexFieldInfo.put("analyzer", analyzer);
		return indexFieldInfo;
	}

	/**
	 * Return a trigram analyzer type for strings.
	 * 
	 * @return
	 */
	public static JsonObject trigramStringType() {
		return fieldType(STRING, ANALYZED, TRIGRAM_ANALYZER);
	}

	/**
	 * Return a JSON mapping field type which is set to not_analyzed.
	 * 
	 * @param type
	 * @return
	 */
	public static JsonObject notAnalyzedType(String type) {
		JsonObject indexFieldInfo = new JsonObject();
		indexFieldInfo.put("type", type);
		indexFieldInfo.put("index", NOT_ANALYZED);
		return indexFieldInfo;
	}

}
