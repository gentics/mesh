package com.gentics.mesh.plugin.forms.model;

import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Model for languages fallback order for rendering forms
 */
public class LanguagesModel implements RestModel {
	private List<String> languages;

	/**
	 * List of language codes for rendering fallback
	 * @return list of codes
	 */
	public List<String> getLanguages() {
		return languages;
	}

	/**
	 * Set the list of language codes for the rendering fallback
	 * @param languages list of codes
	 * @return fluent API
	 */
	public LanguagesModel setLanguages(List<String> languages) {
		this.languages = languages;
		return this;
	}
}
