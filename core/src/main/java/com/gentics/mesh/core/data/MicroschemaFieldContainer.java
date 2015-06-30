package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaField;

public interface MicroschemaFieldContainer {

	MicroschemaField createMicroschema(String key);

	MicroschemaField getMicroschema(String key);

}
