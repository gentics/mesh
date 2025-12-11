package com.gentics.mesh.core.data.search;

import java.util.List;

import com.gentics.mesh.etc.config.search.ComplianceMode;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Elasticsearch compliance implementation
 */
public interface Compliance {

	static final String DEFAULT_TYPE_KEY = "_doc";

	/**
	 * Get compliance mode.
	 * 
	 * @return
	 */
	ComplianceMode getMode();

	/**
	 * Fill the delete request parameters, specific for this compliance mode.
	 * 
	 * @param settings
	 */
	default void prepareDeleteRequest(JsonObject settings) {
		prepareGenericRequest(settings);
	}

	/**
	 * Fill the update request parameters, specific for this compliance mode.
	 * 
	 * @param settings
	 */
	default void prepareUpdateRequest(JsonObject settings) {
		prepareGenericRequest(settings);
	}

	/**
	 * Fill the create request parameters, specific for this compliance mode.
	 * 
	 * @param settings
	 */
	default void prepareCreateRequest(JsonObject settings) {
		prepareGenericRequest(settings);
	}

	/**
	 * Fill the generic request parameters, specific for this compliance mode.
	 * 
	 * @param settings
	 */
	default void prepareGenericRequest(JsonObject settings) {}

	/**
	 * Fill the type mapping according to this compliance mode.
	 * 
	 * @param typeMapping
	 * @return
	 */
	default JsonObject prepareTypeMapping(JsonObject typeMapping) {
		return typeMapping;
	}

	/**
	 * Get the typed mapping for the given entity mapping.
	 * 
	 * @param mapping
	 * @return
	 */
	default JsonObject getDefaultTypeMapping(JsonObject mapping) {
		return mapping;
	}

	/**
	 * Fill the tokenizer according to this compliance mode.
	 * 
	 * @param tokenizer
	 */
	default void prepareTokenizer(JsonObject tokenizer) {
		tokenizer.put("type", "ngram");
	}

	/**
	 * Fill the template name according to this compliance mode.
	 * 
	 * @param json
	 * @param templateName
	 */
	default void prepareTemplate(JsonObject json, String templateName) {
		JsonArray indexPatterns = new JsonArray(List.of(templateName));
		json.put("index_patterns", indexPatterns);
	}

	/**
	 * Get the default mapping type key.
	 * 
	 * @return
	 */
	default String getDefaultTypeKey() {
		return DEFAULT_TYPE_KEY;
	}

	/**
	 * Put the total value in the hits info, following the compliance mode format.
	 * 
	 * @param hitsInfo
	 * @param value
	 */
	default void putTotal(JsonObject hitsInfo, long value) {
		hitsInfo.put("total", new JsonObject().put("value", value));
	}

	/**
	 * Get the total value from the hits info, following the compliance mode format.
	 * 
	 * @param hitsInfo
	 * @return
	 */
	default long getTotal(JsonObject hitsInfo) {
		return hitsInfo.getJsonObject("total").getLong("value");
	}
}
