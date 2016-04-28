package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LABEL_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LIST_TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.REQUIRED_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.CHANGEFIELDTYPE;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.EMPTY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.MapDifference.ValueDifference;
import com.google.common.collect.Maps;

public abstract class AbstractFieldSchema implements FieldSchema {

	private String name;

	private String label;

	private boolean required = false;

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
	public void apply(Map<String, Object> fieldProperties) {
		if (fieldProperties.get(SchemaChangeModel.REQUIRED_KEY) != null) {
			setRequired(Boolean.valueOf(String.valueOf(fieldProperties.get(REQUIRED_KEY))));
		}
		String label = (String) fieldProperties.get(LABEL_KEY);
		if (label != null) {
			setLabel(label);
		}
	}

	@Override
	public void validate() {
		if (StringUtils.isEmpty(getName())) {
			throw error(BAD_REQUEST, "schema_error_fieldname_not_set");
		}
	}

	protected SchemaChangeModel createTypeChange(FieldSchema fieldSchema) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException {
		//Create the initial empty change
		SchemaChangeModel change = new SchemaChangeModel(EMPTY, getName());

		Map<String, Object> schemaPropertiesA = getAllChangeProperties();
		Map<String, Object> schemaPropertiesB = fieldSchema.getAllChangeProperties();

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

		// Check whether the field type has been changed
		if (!fieldSchema.getType().equals(getType())) {
			change.setOperation(CHANGEFIELDTYPE);
			change.setProperty(TYPE_KEY, fieldSchema.getType());
			if (fieldSchema instanceof ListFieldSchema) {
				change.getProperties().put(LIST_TYPE_KEY, ((ListFieldSchema) fieldSchema).getListType());
			}
			// Add fieldB properties which are new
			change.getProperties().putAll(schemaPropertiesB);
			return change;
		}

		// Check whether fields have been updated
		Map<String, ValueDifference<Object>> differentProperties = diff.entriesDiffering();
		if (!differentProperties.isEmpty()) {
			change.setOperation(UPDATEFIELD);
			for (String key : differentProperties.keySet()) {
				change.getProperties().put(key, differentProperties.get(key).rightValue());
			}
		}
		return change;
	}

	@Override
	public Map<String, Object> getAllChangeProperties() {
		Map<String, Object> map = new HashMap<>();
		map.put(LABEL_KEY, getLabel());
		map.put(REQUIRED_KEY, isRequired());
		return map;
	}

}
