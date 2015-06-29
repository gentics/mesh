package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.node.field.Field;

public interface FieldSchema extends Field {
	public String getType();

	public String getLabel();

	public void setLabel(String label);

	public String getName();

	public void setName(String name);
}
