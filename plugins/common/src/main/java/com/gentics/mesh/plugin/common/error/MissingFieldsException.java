package com.gentics.mesh.plugin.common.error;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.plugin.common.MeshPluginException;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception for bad requests with missing fields.
 */
public class MissingFieldsException extends MeshPluginException {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The list of missing fields in the request")
	private List<String> missingFields = new ArrayList<>();

	public MissingFieldsException(String missingField) {
		missingFields.add(missingField);
	}

	public MissingFieldsException(List<String> missingFields) {
		this.missingFields = missingFields;
	}

	public List<String> getMissingFields() {
		return missingFields;
	}

	public MissingFieldsException setMissingFields(List<String> missingFields) {
		this.missingFields = missingFields;

		return this;
	}

	@Override
	public String toJson() {
		return new JsonObject().put("errors", missingFields).encode();
	}

	@Override
	public String toString() {
		return toJson();
	}
}
