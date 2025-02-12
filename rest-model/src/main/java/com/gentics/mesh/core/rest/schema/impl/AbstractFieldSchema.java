package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ELASTICSEARCH_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LABEL_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LIST_TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.NO_INDEX_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.REQUIRED_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.CHANGEFIELDTYPE;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.EMPTY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.LanguageOverrideUtil;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;

import io.vertx.core.json.JsonObject;

/**
 * Abstract class for field schema implementations. (eg.: {@link HtmlFieldSchemaImpl})
 */
public abstract class AbstractFieldSchema implements FieldSchema {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Name of the field")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Label of the field")
	private String label;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the field is required or not. A create request will fail if no field value has been specified. The update request will fail when the value is omitted and the field has never been saved before.")
	private boolean required = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the field is excluded from indexing or not.")
	private boolean noIndex = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Additional elasticsearch index field configuration. This can be used to add custom fields with custom analyzers to the search index.")
	private JsonObject elasticsearch;

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public AbstractFieldSchema setLabel(String label) {
		this.label = label;
		return this;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public AbstractFieldSchema setName(String name) {
		this.name = name;
		return this;
	}

	@Override
	public boolean isRequired() {
		return required;
	}

	@Override
	public AbstractFieldSchema setRequired(boolean flag) {
		this.required = flag;
		return this;
	}

	@Override
	public boolean isNoIndex() {
		return noIndex;
	}

	@Override
	public FieldSchema setNoIndex(boolean isNoIndex) {
		this.noIndex = isNoIndex;
		return this;
	}

	@Override
	public JsonObject getElasticsearch() {
		return elasticsearch;
	}

	@Override
	public AbstractFieldSchema setElasticsearch(JsonObject elasticsearch) {
		this.elasticsearch = elasticsearch;
		return this;
	}

	@Override
	public void apply(Map<String, Object> fieldProperties) {
		if (fieldProperties.get(SchemaChangeModel.REQUIRED_KEY) != null) {
			setRequired(Boolean.valueOf(String.valueOf(fieldProperties.get(REQUIRED_KEY))));
		}
		if (fieldProperties.get(SchemaChangeModel.NO_INDEX_KEY) != null) {
			setNoIndex(Boolean.valueOf(String.valueOf(fieldProperties.get(NO_INDEX_KEY))));
		}
		if (fieldProperties.get(SchemaChangeModel.ELASTICSEARCH_KEY) != null) {
			Object value = fieldProperties.get(ELASTICSEARCH_KEY);
			JsonObject options = value instanceof JsonObject ? (JsonObject) value : new JsonObject((String) value);
			setElasticsearch(options);
		}

		String label = (String) fieldProperties.get(LABEL_KEY);
		if (label != null) {
			setLabel(label);
		}

		String name = (String) fieldProperties.get(NAME_KEY);
		if (name != null) {
			setName(name);
		}
	}

	@Override
	public void validate() {
		if (StringUtils.isEmpty(getName())) {
			throw error(BAD_REQUEST, "schema_error_fieldname_not_set");
		}
		LanguageOverrideUtil.validateLanguageOverrides(getElasticsearch());
	}

	@Override
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) {
		// Create the initial empty change
		SchemaChangeModel change = new SchemaChangeModel(EMPTY, getName());

		Map<String, Object> schemaPropertiesA = getAllChangeProperties();
		Map<String, Object> schemaPropertiesB = fieldSchema.getAllChangeProperties();

		// Check whether the field type has been changed
		if (!fieldSchema.getType().equals(getType())) {
			change.setOperation(CHANGEFIELDTYPE);
			change.setProperty(TYPE_KEY, fieldSchema.getType());
			if (fieldSchema instanceof ListFieldSchema) {
				change.getProperties().put(LIST_TYPE_KEY, ((ListFieldSchema) fieldSchema).getListType());
			}
			// Add fieldB properties which are new
			change.getProperties().putAll(schemaPropertiesB);
		} else {

			// Generate a structural diff. This way it is easy to determine which field properties have been updated, added or removed.
			MapDifference<String, Object> diff = Maps.difference(schemaPropertiesA, schemaPropertiesB, new Equivalence<Object>() {

				@Override
				protected boolean doEquivalent(Object a, Object b) {
					return Objects.deepEquals(a, b);
				}

				@Override
				protected int doHash(Object t) {
					return t.hashCode();
				}

			});

			// Check whether fields have been updated
			Map<String, ValueDifference<Object>> differentProperties = diff.entriesDiffering();
			if (!differentProperties.isEmpty()) {
				change.setOperation(UPDATEFIELD);
				for (String key : differentProperties.keySet()) {
					change.getProperties().put(key, differentProperties.get(key).rightValue());
				}
			}
		}
		return change;
	}

	@Override
	@JsonIgnore
	public Map<String, Object> getAllChangeProperties() {
		Map<String, Object> map = new HashMap<>();
		map.put(LABEL_KEY, getLabel());
		map.put(REQUIRED_KEY, isRequired());
		map.put(NO_INDEX_KEY, isNoIndex());
		// empty object and null/missing should be treated the same
		map.put(ELASTICSEARCH_KEY, getElasticsearch() == null || getElasticsearch().size() == 0 ? new JsonObject() : getElasticsearch());
		return map;
	}

}
