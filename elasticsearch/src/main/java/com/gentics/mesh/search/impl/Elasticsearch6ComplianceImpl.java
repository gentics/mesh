package com.gentics.mesh.search.impl;

import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;

import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;

/**
 * A compliance implementation of Elasticsearch 6.x
 */
public class Elasticsearch6ComplianceImpl implements Compliance {

	@Override
	public ComplianceMode getMode() {
		return ComplianceMode.ES_6;
	}

	@Override
	public void prepareGenericRequest(JsonObject settings) {
		settings.put("_type", SearchProvider.DEFAULT_TYPE);
	}

	@Override
	public JsonObject prepareTypeMapping(JsonObject typeMapping) {
		JsonObject mapping = new JsonObject();
		mapping.put(DEFAULT_TYPE, typeMapping);
		return mapping;
	}

	@Override
	public JsonObject getDefaultTypeMapping(JsonObject mapping) {
		return mapping.getJsonObject(DEFAULT_TYPE);
	}

	@Override
	public String getDefaultTypeKey() {
		return DEFAULT_TYPE;
	}

	@Override
	public void prepareTokenizer(JsonObject tokenizer) {
		tokenizer.put("type", "nGram");
	}

	@Override
	public void prepareTemplate(JsonObject json, String templateName) {
		json.put("template", templateName);
	}

	@Override
	public void putTotal(JsonObject hitsInfo, long value) {
		hitsInfo.put("total", value);
	}

	@Override
	public long getTotal(JsonObject hitsInfo) {
		return hitsInfo.getLong("total");
	}
}
