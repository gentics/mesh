package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;

public interface GraphStringFieldList extends GraphListField<StringGraphField, StringFieldListImpl> {

	public static final String TYPE = "string";

	StringGraphField createString(String string);

	StringGraphField getString(int index);
}
