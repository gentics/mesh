package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;

public interface BooleanGraphFieldList extends ListGraphField<BooleanGraphField, BooleanFieldListImpl> {

	public static final String TYPE = "boolean";

	BooleanGraphField getBoolean(int index);

	BooleanGraphField createBoolean(Boolean flag);
}
