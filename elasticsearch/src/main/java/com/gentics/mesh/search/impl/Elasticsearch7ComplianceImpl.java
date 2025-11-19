package com.gentics.mesh.search.impl;

import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.etc.config.search.ComplianceMode;

import io.vertx.core.json.JsonObject;

/**
 * A compliance implementation of Elasticsearch 7.x
 */
public class Elasticsearch7ComplianceImpl implements Compliance {

	@Override
	public ComplianceMode getMode() {
		return ComplianceMode.ES_7;
	}

	@Override
	public void prepareTokenizer(JsonObject tokenizer) {
		tokenizer.put("type", "nGram");
	}

	@Override
	public void prepareTemplate(JsonObject json, String templateName) {
		json.put("template", templateName);
	}
}
