package com.gentics.mesh.plugin.forms.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.common.RestModel;

public class TranslationLanguageModel implements RestModel {

	private final Map<String, JsonNode> translations = new LinkedHashMap<>();

	@JsonAnyGetter
	public Map<String, JsonNode> getTranslations() {
		return translations;
	}

	@JsonAnySetter
	public TranslationLanguageModel setTranslations(String key, JsonNode value) {
		translations.put(key, value);
		return this;
	}
}
