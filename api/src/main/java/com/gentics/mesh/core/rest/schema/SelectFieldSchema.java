package com.gentics.mesh.core.rest.schema;

import java.util.List;

public interface SelectFieldSchema extends MicroschemaListableFieldSchema {

	List<String> getOptions();

	void setSelections(List<String> selections);

	List<String> getSelections();

}
