package com.gentics.mesh.core.rest.schema;

public interface ListFieldSchema extends MicroschemaListableFieldSchema {

	String[] getAllowedSchemas();

	ListFieldSchema setAllowedSchemas(String[] allowedSchemas);

	String getListType();

	ListFieldSchema setListType(String listType);

	ListFieldSchema setMax(Integer max);

	Integer getMax();

	Integer getMin();

	ListFieldSchema setMin(Integer min);
}
