package com.gentics.mesh.core.rest.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LABEL_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LIST_TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.REQUIRED_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

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
	 * Compare the required field flag and update the change if the flag was changed.
	 * 
	 * @param change
	 *            Change to be updated
	 * @param fieldSchema
	 *            Field to compare with
	 * @param modified
	 *            modified state flag to be used when no modification was found
	 * @return True if a change was detected or the value of the <b>modified</b> argument of no change was detected
	 */
	protected boolean compareRequiredField(SchemaChangeModel change, FieldSchema fieldSchema, boolean modified) {
		if (isRequired() != fieldSchema.isRequired()) {
			change.setProperty(SchemaChangeModel.REQUIRED_KEY, fieldSchema.isRequired());
			return true;
		}
		return modified;

	}

	/**
	 * Create a type specific change.
	 * 
	 * @param fieldSchema
	 * @return
	 * @throws IOException
	 */
	protected Optional<SchemaChangeModel> createTypeChange(FieldSchema fieldSchema) throws IOException {
		SchemaChangeModel change = SchemaChangeModel.createChangeFieldTypeChange(fieldSchema.getName(), fieldSchema.getType());
			change.getProperties().put(LIST_TYPE_KEY, ((ListFieldSchema) fieldSchema).getListType());
			if (fieldSchema instanceof ListFieldSchema) {
		}
		change.loadMigrationScript();
		return Optional.of(change);
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

}
