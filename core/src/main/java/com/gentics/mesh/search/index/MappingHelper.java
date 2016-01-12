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
	public static final String STRING = "string";

	// Index Types
	public static final String NOT_ANALYZED = "not_analyzed";
	public static final String ANALYZED = "analyzed";

	/**
	 * Return a JSON mapping field type.
	 * 
	 * @param type
	 *            Type of the field
	 * @param indexType
	 *            Index type of the field
	 * @return
	 */
	public static JsonObject fieldType(String type, String indexType) {
		JsonObject indexFieldInfo = new JsonObject();
		indexFieldInfo.put("type", type);
		indexFieldInfo.put("index", indexType);
		return indexFieldInfo;
	}

}
