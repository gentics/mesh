package com.gentics.mesh.core.rest.schema;

import java.util.List;

import com.gentics.mesh.core.rest.node.field.SelectField;

public interface SelectFieldSchema extends SelectField, MicroschemaListableFieldSchema {

	List<String> getOptions();

}
