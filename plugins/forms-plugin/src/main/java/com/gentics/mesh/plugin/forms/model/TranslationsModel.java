package com.gentics.mesh.plugin.forms.model;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Model for translations managed by the forms plugin.
 *
 * <p>Each language entry is represented by a {@link TranslationLanguageModel}
 * holding the language key (e.g. "de") and its translation map.</p>
 */
public class TranslationsModel implements RestModel {

	@JsonProperty("default")
	public TranslationLanguageModel defaultTranslations = new TranslationLanguageModel();

	@JsonProperty("types")
	public Map<String, TranslationLanguageModel> types = new LinkedHashMap<>();

}
