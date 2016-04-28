package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LABEL_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LIST_TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.REQUIRED_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.EMPTY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

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

	/**
	 * Create a type specific change.
	 * 
	 * @param fieldSchema
	 * @return
	 * @throws IOException
	 */
	protected SchemaChangeModel createTypeChange(FieldSchema fieldSchema) throws IOException {
		SchemaChangeModel change = SchemaChangeModel.createChangeFieldTypeChange(fieldSchema.getName(), fieldSchema.getType());
		if (fieldSchema instanceof ListFieldSchema) {
			change.getProperties().put(LIST_TYPE_KEY, ((ListFieldSchema) fieldSchema).getListType());
		}
		return change;
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

	@Override
	public SchemaChangeModel compareTo(FieldSchema fieldSchema) throws IOException {
		//Create the initial empty change
		SchemaChangeModel change = new SchemaChangeModel(EMPTY, getName());

		// Check for label changes
		if (!Objects.equals(getLabel(), fieldSchema.getLabel())) {
			change.setOperation(UPDATEFIELD);
			change.setProperty(LABEL_KEY, fieldSchema.getLabel());
		}
		// Check for required field changes
		if (isRequired() != fieldSchema.isRequired()) {
			change.setOperation(UPDATEFIELD);
			change.setProperty(SchemaChangeModel.REQUIRED_KEY, fieldSchema.isRequired());
		}
		return change;
	}

}
