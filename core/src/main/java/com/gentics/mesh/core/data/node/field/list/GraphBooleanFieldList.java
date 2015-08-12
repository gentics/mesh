package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;

public interface GraphBooleanFieldList extends GraphListField<BooleanGraphField> {

	public static final String TYPE = "boolean";

	BooleanGraphField getBoolean(int index);

	BooleanGraphField createBoolean(Boolean flag);
}
