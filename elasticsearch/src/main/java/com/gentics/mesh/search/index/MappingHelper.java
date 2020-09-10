package com.gentics.mesh.search.index;

import io.vertx.core.json.JsonObject;

/**
 * Utility class which contains some methods and constants that can be used to build ES index mappings.
 */
public final class MappingHelper {

	// Common keys
	public static final String UUID_KEY = "uuid";
	public static final String VERSION_KEY = "version";
	public static final String BUCKET_ID_KEY = "bucket_id";
	public static final String NAME_KEY = "name";
	public static final String DESCRIPTION_KEY = "description";

	// Field Types
	public static final String OBJECT = "object";
	public static final String KEYWORD = "keyword";
	public static final String NESTED = "nested";
	public static final String TEXT = "text";
	public static final String BOOLEAN = "boolean";
	public static final String DATE = "date";
	public static final String INTEGER = "integer";
	public static final String LONG = "long";
	public static final String DOUBLE = "double";
	public static final String BINARY = "binary";
	public static final String GEOPOINT = "geo_point";

	// Index Types
	public static final boolean DONT_INDEX_VALUE = false;
	public static final boolean INDEX_VALUE = true;

	// Analyzer
	public static final String TRIGRAM_ANALYZER = "trigrams";

	/**
	 * Return a JSON mapping field type.
	 * 
	 * @param type
	 *            Type of the field
	 * @param analyzeField
	 *            Flag which indicates whether the field should be analyzed
	 * @param analyzer
	 *            Name of the analyzer to be used
	 * @return
	 */
	public static JsonObject fieldType(String type, boolean analyzeField, String analyzer) {
		JsonObject indexFieldInfo = new JsonObject();
		indexFieldInfo.put("type", type);
		indexFieldInfo.put("index", analyzeField);
		indexFieldInfo.put("analyzer", analyzer);
		return indexFieldInfo;
	}

	/**
	 * Return a trigram analyzer type for text.
	 * 
	 * @return
	 */
	public static JsonObject trigramTextType() {
		return addRawInfo(fieldType(TEXT, INDEX_VALUE, TRIGRAM_ANALYZER));
	}

	/**
	 * Add the raw field info to the given mapping element.
	 *
	 * @param fieldInfo
	 * @return The modified field info object
	 */
	public static JsonObject addRawInfo(JsonObject fieldInfo) {
		JsonObject rawInfo = new JsonObject();
		rawInfo.put("type", KEYWORD);
		rawInfo.put("index", INDEX_VALUE);
		JsonObject rawFieldInfo = new JsonObject();
		rawFieldInfo.put("raw", rawInfo);
		fieldInfo.put("fields", rawFieldInfo);
		return fieldInfo;
	}

	public static JsonObject notAnalyzedType(String type) {
		return notAnalyzedType(type, null);
	}

	/**
	 * Return a JSON mapping field type which is set to not_analyzed.
	 * 
	 * @param type
	 * @param customFields
	 * @return
	 */
	public static JsonObject notAnalyzedType(String type, JsonObject customFields) {
		if (type.equals(TEXT)) {
			throw new RuntimeException("Type {text} is invalid for this operation. You most likly want {keyword}");
		}
		JsonObject indexFieldInfo = new JsonObject();
		indexFieldInfo.put("type", type);
		indexFieldInfo.put("index", INDEX_VALUE);
		if (customFields != null) {
			indexFieldInfo.put("fields", customFields);
		}
		return indexFieldInfo;
	}

}
