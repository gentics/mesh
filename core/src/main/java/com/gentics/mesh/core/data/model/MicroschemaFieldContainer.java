package com.gentics.mesh.core.data.model;

import com.gentics.mesh.core.data.model.node.field.nesting.MicroschemaField;

public interface MicroschemaFieldContainer {

	MicroschemaField createMicroschema(String key);

	MicroschemaField getMicroschema(String key);

}
