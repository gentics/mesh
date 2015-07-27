package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.basic.StringField;

public interface StringFieldList extends ListField<StringField> {

	public static final String TYPE = "string";

	StringField createString(String string);

	StringField getString(int index);
}
