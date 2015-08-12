package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.StringGraphField;

public interface GraphStringFieldList extends GraphListField<StringGraphField> {

	public static final String TYPE = "string";

	StringGraphField createString(String string);

	StringGraphField getString(int index);
}
