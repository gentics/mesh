package com.gentics.mesh.core.rest.node;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for the rest model of a publish status response for a node.
 */
public class PublishStatusResponse implements RestModel {

	@JsonPropertyDescription("Map of publish status entries per language")
	private Map<String, PublishStatusModel> availableLanguages;

	public PublishStatusResponse() {
	}

	public Map<String, PublishStatusModel> getAvailableLanguages() {
		return availableLanguages;
	}

	public void setAvailableLanguages(Map<String, PublishStatusModel> availableLanguages) {
		this.availableLanguages = availableLanguages;
	}
}
