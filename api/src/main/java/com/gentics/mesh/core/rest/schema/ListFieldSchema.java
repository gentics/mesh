package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.node.field.ListField;
import com.gentics.mesh.core.rest.node.field.ListableField;

public interface ListFieldSchema<T extends ListableField> extends ListField<T>, MicroschemaListableFieldSchema {

	String[] getAllowedSchemas();

	void setAllowedSchemas(String[] allowedSchemas);

	String getListType();

	void setListType(String listType);

	void setMax(Integer max);

	Integer getMax();

	Integer getMin();

	void setMin(Integer min);
}
