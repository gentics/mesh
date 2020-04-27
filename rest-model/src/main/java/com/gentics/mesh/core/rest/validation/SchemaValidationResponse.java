package com.gentics.mesh.core.rest.validation;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.RestModel;

import io.vertx.core.json.JsonObject;

/**
 * Response of a schema / microschema validation.
 */
public class SchemaValidationResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Status of the validation.")
	ValidationStatus status;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Detailed validation message.")
	GenericMessageResponse message;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The generated default elasticsearch index configuration which includes the used analyzers and mappings.")
	JsonObject elasticsearch;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The generated elasticsearch index configuration for each language.")
	Map<String, JsonObject> languageElasticsearch;

	/**
	 * Return the elasticsearch index configuration which was generated using the schema.
	 * 
	 * @return
	 */
	public JsonObject getElasticsearch() {
		return elasticsearch;
	}

	/**
	 * Set the elasticsearch index configuration.
	 * 
	 * @param elasticsearch
	 * @return
	 */
	public SchemaValidationResponse setElasticsearch(JsonObject elasticsearch) {
		this.elasticsearch = elasticsearch;
		return this;
	}

	public Map<String, JsonObject> getLanguageElasticsearch() {
		return languageElasticsearch;
	}

	public void setLanguageElasticsearch(Map<String, JsonObject> languageElasticsearch) {
		this.languageElasticsearch = languageElasticsearch;
	}

	/**
	 * Return the validation status.
	 * 
	 * @return
	 */
	public ValidationStatus getStatus() {
		return status;
	}

	/**
	 * Set the status.
	 * 
	 * @param status
	 * @return Fluent API
	 */
	public SchemaValidationResponse setStatus(ValidationStatus status) {
		this.status = status;
		return this;
	}

	/**
	 * Return the validation message.
	 * 
	 * @return
	 */
	public GenericMessageResponse getMessage() {
		return message;
	}

	/**
	 * Set the message.
	 * 
	 * @param message
	 * @return Fluent API
	 */
	public SchemaValidationResponse setMessage(GenericMessageResponse message) {
		this.message = message;
		return this;
	}
}
