package com.gentics.mesh.core.rest.schema;

import java.util.List;

import com.gentics.mesh.core.rest.node.field.ListableField;

public interface ListFieldSchema<T extends ListableField> extends MicroschemaListableFieldSchema {

	List<T> getItems();

	String[] getAllowedSchemas();

	void setAllowedSchemas(String[] allowedSchemas);

	String getListType();

	void setListType(String listType);

	void setMax(Integer max);

	Integer getMax();

	Integer getMin();

	void setMin(Integer min);
}
