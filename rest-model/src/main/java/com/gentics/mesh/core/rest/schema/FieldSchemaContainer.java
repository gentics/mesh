package com.gentics.mesh.core.rest.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.node.FieldMap;

import io.vertx.core.json.JsonObject;

/**
 * A field schema container is a named container that contains field schemas. Typical containers are {@link SchemaModel} or {@link MicroschemaModel}.
 */
public interface FieldSchemaContainer extends RestModel {

	public static final String NAME_REGEX = "^[_a-zA-Z][_a-zA-Z0-9]*$";

	/**
	 * Return the name of the container.
	 * 
	 * @return Name of the container
	 */
	String getName();

	/**
	 * Set the container name.
	 * 
	 * @param name
	 *            Name of the container
	 */
	FieldSchemaContainer setName(String name);

	/**
	 * Return the container description.
	 * 
	 * @return Schema description
	 */
	String getDescription();

	/**
	 * Set the description of the container.
	 * 
	 * @param description
	 *            Container description
	 */
	FieldSchemaContainer setDescription(String description);

	/**
	 * Return the 'exclude from indexing' flag.
	 * 
	 * @return flag
	 */
	Boolean getNoIndex();

	/**
	 * Set the 'exclude from indexing' flag.
	 * 
	 * @param flag
	 * @return self
	 */
	FieldSchemaContainer setNoIndex(Boolean noIndex);

	/**
	 * Return the field with the given name.
	 * 
	 * @param fieldName
	 * @return
	 */
	default FieldSchema getField(String fieldName) {
		return getFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null);
	}

	/**
	 * Return the field schema with the given name.
	 * 
	 * @param fieldName
	 * @param classOfT
	 * @return
	 */
	default <T> T getField(String fieldName, Class<T> classOfT) {
		return classOfT.cast(getFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst().orElse(null));
	}

	/**
	 * Removes the field with the given name.
	 * 
	 * @param name
	 */
	default FieldSchemaContainer removeField(String name) {
		if (name == null) {
			return this;
		}
		getFields().removeIf(field -> name.equals(field.getName()));
		return this;
	}

	/**
	 * Return the list of field schemas.
	 * 
	 * @return List of field schemas
	 */
	List<FieldSchema> getFields();

	/**
	 * Return the map of field schemas.
	 * 
	 * @return
	 */
	@JsonIgnore
	default Map<String, FieldSchema> getFieldsAsMap() {
		Map<String, FieldSchema> map = new HashMap<>();
		for (FieldSchema field : getFields()) {
			map.put(field.getName(), field);
		}
		return map;
	}

	/**
	 * Add the given field schema to the list of field schemas.
	 * 
	 * @param fieldSchema
	 */
	default FieldSchemaContainer addField(FieldSchema fieldSchema) {
		Objects.requireNonNull(fieldSchema, "The field schema must not be null");
		Objects.requireNonNull(fieldSchema.getName(), "The field schema must have a valid name");
		getFields().add(fieldSchema);
		return this;
	}

	/**
	 * Add the given field schema to the list of field schemas after the field with the given name. The field will be added to the end of the list if the insert
	 * position could not be determined via the afterFieldName parameter.
	 * 
	 * @param fieldSchema
	 *            Field schema to be added to the container
	 * @param afterFieldName
	 *            Field name that identifies the position after which the field will be inserted
	 */
	default FieldSchemaContainer addField(FieldSchema fieldSchema, String afterFieldName) {
		List<FieldSchema> fields = getFields();
		int index = fields.size();
		if (afterFieldName != null) {
			for (int i = 0; i < fields.size(); i++) {
				if (afterFieldName.equals(fields.get(i).getName())) {
					index = i;
					break;
				}
			}
		}
		if (index < fields.size()) {
			index = index + 1;
		}
		fields.add(index, fieldSchema);

		return this;
	}

	/**
	 * Return the search index configuration.
	 * This includes the "_meshLanguageOverride" field and must be removed before creating the index in Elasticsearch.
	 * 
	 * @return
	 */
	JsonObject getElasticsearch();

	/**
	 * Set the search index configuration.
	 * 
	 * @param elasticsearch
	 * @return Fluent API
	 */
	FieldSchemaContainer setElasticsearch(JsonObject elasticsearch);

	/**
	 * Finds all languages that override the default index settings/mappings.
	 * This includes index settings and the mappings for each field.
	 * This also splits up lists of languages.
	 * @return
	 */
	default Stream<String> findOverriddenSearchLanguages() {
		Stream<JsonObject> settings = Stream.of(getElasticsearch());
		Stream<JsonObject> mappings = getFields().stream()
			.map(FieldSchema::getElasticsearch);

		return Stream.concat(settings, mappings)
			.flatMap(LanguageOverrideUtil::findLanguages)
			.distinct();
	}

	/**
	 * Set the list of schema fields.
	 * 
	 * @param fields
	 */
	FieldSchemaContainer setFields(List<FieldSchema> fields);

	/**
	 * Does this model describe a microschema?
	 * 
	 * @return
	 */
	boolean isMicroschema();

	/**
	 * Validate the schema for correctness.
	 */
	default void validate() {
		if (StringUtils.isEmpty(getName())) {
			throw error(BAD_REQUEST, "schema_error_no_name");
		}

		if (!getName().matches(NAME_REGEX)) {
			throw error(BAD_REQUEST, "schema_error_invalid_name", getName());
		}

		LanguageOverrideUtil.validateLanguageOverrides(getElasticsearch());

		Set<String> fieldNames = new HashSet<>();
		for (FieldSchema field : getFields()) {
			if (field.getName() != null) {
				if (!fieldNames.add(field.getName().toLowerCase())) {
					throw error(BAD_REQUEST, "schema_error_duplicate_field_name", field.getName());
				}
			}
			field.validate();
		}
	}

	/**
	 * Assert that the field map does not contain any fields which are not specified by the schema.
	 * 
	 * @param fieldMap
	 */
	default void assertForUnhandledFields(FieldMap fieldMap) {
		Set<String> allFieldsOfRequest = new HashSet<>(fieldMap.keySet());
		for (FieldSchema fieldSchema : getFields()) {
			allFieldsOfRequest.remove(fieldSchema.getName());
		}
		if (allFieldsOfRequest.size() > 0) {
			throw error(BAD_REQUEST, "node_unhandled_fields", getName(), Arrays.toString(allFieldsOfRequest.toArray()));
		}
	}
}
