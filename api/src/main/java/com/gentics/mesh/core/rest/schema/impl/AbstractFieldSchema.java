package com.gentics.mesh.core.rest.schema.impl;

import com.gentics.mesh.core.rest.schema.FieldSchema;
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
			change.setRequired(fieldSchema.isRequired());
			return true;
		}
		return modified;

	}

}
