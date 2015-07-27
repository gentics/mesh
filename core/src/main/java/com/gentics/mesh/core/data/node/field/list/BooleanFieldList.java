package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.BooleanField;

public interface BooleanFieldList extends ListField<BooleanField> {

	public static final String TYPE = "boolean";

	BooleanField getBoolean(int index);

	BooleanField createBoolean(Boolean flag);
}
