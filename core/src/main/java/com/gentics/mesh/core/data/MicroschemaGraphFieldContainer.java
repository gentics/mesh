package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.node.field.nesting.MicroschemaGraphField;

public interface MicroschemaGraphFieldContainer {

	MicroschemaGraphField createMicroschema(String key);

	MicroschemaGraphField getMicroschema(String key);

}
