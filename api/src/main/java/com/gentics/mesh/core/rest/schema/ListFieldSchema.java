package com.gentics.mesh.core.rest.schema;

public interface ListFieldSchema extends MicroschemaListableFieldSchema {

	String[] getAllowedSchemas();

	void setAllowedSchemas(String... allowedSchemas);

	String getListType();

	void setListType(String listType);

	void setMax(Integer max);

	Integer getMax();

	Integer getMin();

	void setMin(Integer min);
}
