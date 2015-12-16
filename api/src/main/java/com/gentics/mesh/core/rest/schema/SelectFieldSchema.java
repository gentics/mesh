package com.gentics.mesh.core.rest.schema;

import java.util.List;

public interface SelectFieldSchema extends FieldSchema {

	//TODO field schema should not have any values?
	List<String> getOptions();

	//TODO field schema should not have any values?
	void setSelections(List<String> selections);

	//TODO field schema should not have any values?
	List<String> getSelections();

}
