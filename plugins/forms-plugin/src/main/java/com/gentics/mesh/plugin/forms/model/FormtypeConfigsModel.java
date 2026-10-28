package com.gentics.mesh.plugin.forms.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Model for formtype configurations managed by the forms plugin.
 * Serializes as a flat JSON object: { "formtype-key": { ...config }, ... }
 */
public class FormtypeConfigsModel implements RestModel {

	private final Map<String, JsonNode> configs = new LinkedHashMap<>();

	@JsonAnyGetter
	public Map<String, JsonNode> getConfigs() {
		return configs;
	}

	@JsonAnySetter
	public FormtypeConfigsModel setConfig(String key, JsonNode value) {
		configs.put(key, value);
		return this;
	}
}
