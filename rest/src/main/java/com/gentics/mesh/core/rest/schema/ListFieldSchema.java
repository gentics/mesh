package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.node.field.ListField;

public interface ListFieldSchema extends ListField, FieldSchema {

	String[] getAllowedSchemas();

	void setAllowedSchemas(String[] allowedSchemas);

	String getListType();

	void setListType(String listType);

	void setMax(Integer max);

	Integer getMax();

	Integer getMin();

	void setMin(Integer min);
}
