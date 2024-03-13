package com.gentics.mesh.core.rest.schema;

import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.etc.config.search.ElasticSearchOptions;
import com.gentics.mesh.etc.config.search.MappingMode;

import io.vertx.core.json.JsonObject;

/**
 * A field schema is a field within a schema. In contradiction to node fields a field schema is the blueprint of a field and will not store any data. Instead it
 * only defines a field within a schema.
 */
public interface FieldSchema {

	/**
	 * Return the type of the field schema.
	 * 
	 * @return Field schema type
	 */
	@JsonProperty(required = true)
	@JsonPropertyDescription("Type of the field.")
	String getType();

	/**
	 * Return the label of the field schema.
	 * 
	 * @return Label
	 */
	@JsonProperty(required = false)
	@JsonPropertyDescription("Label of the field.")
	String getLabel();

	/**
	 * Set the label of the field schema.
	 * 
	 * @param label
	 *            Field schema label
	 * @return Fluent API
	 */
	FieldSchema setLabel(String label);

	// TODO is this not the fieldkey? is the key the name??
	/**
	 * Return the name of the field schema.
	 * 
	 * @return Name
	 */
	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the field.")
	String getName();

	/**
	 * Set the name of the field schema.
	 * 
	 * @param name
	 * @return Fluent API
	 */
	FieldSchema setName(String name);

	/**
	 * Return the required flag of the field schema.
	 * 
	 * @return
	 */
	boolean isRequired();

	/**
	 * Set the required flag.
	 * 
	 * @param isRequired
	 * @return Fluent API
	 */
	FieldSchema setRequired(boolean isRequired);

	/**
	 * Return the 'excluded from indexing' flag of the field schema.
	 * 
	 * @return
	 */
	boolean isNoIndex();

	/**
	 * Set the 'excluded from indexing' flag.
	 * 
	 * @param isNoIndex
	 * @return Fluent API
	 */
	FieldSchema setNoIndex(boolean isNoIndex);

	/**
	 * Compare the field schema with the given field schema.
	 * 
	 * @param fieldSchema
	 * @return Detected change
	 */
	SchemaChangeModel compareTo(FieldSchema fieldSchema);

	/**
	 * Apply the given field properties to the field schema.
	 * 
	 * @param fieldProperties
	 */
	void apply(Map<String, Object> fieldProperties);

	/**
	 * Validate the field properties.
	 */
	void validate();

	/**
	 * Return a map of all properties of the schema.
	 * 
	 * @return
	 */
	@JsonIgnore
	Map<String, Object> getAllChangeProperties();

	/**
	 * Return the search index fields configuration.
	 * 
	 * @return
	 */
	JsonObject getElasticsearch();

	/**
	 * Set the elasticsearch index fields configuration.
	 * 
	 * @param elasticsearch
	 * @return Fluent API
	 */
	FieldSchema setElasticsearch(JsonObject elasticsearch);

	/**
	 * Checks if the field can be used as a display field.
	 * 
	 * @return
	 */
	@JsonIgnore
	default boolean isDisplayField() {
		return false;
	}

	/**
	 * Check whether the mapping is required. A mapping can not be required when the default mappings option has been disabled and the field schema does not
	 * specify a custom mapping.
	 * 
	 * @param options
	 *            Search options
	 * @return
	 */
	@JsonIgnore
	default boolean isMappingRequired(ElasticSearchOptions options) {
		if (isNoIndex()) {
			return false;
		}
		MappingMode mode = options.getMappingMode();
		return mode == MappingMode.DYNAMIC || mode == MappingMode.STRICT && getElasticsearch() != null;
	}

	@JsonIgnore
	default Optional<ListFieldSchema> maybeGetListField() {
		return Optional.empty();
	}
}
