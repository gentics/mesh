package com.gentics.mesh.util;

import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;

public final class FieldUtil {

	public static StringField createStringField(String string) {
		StringField field = new StringFieldImpl();
		field.setText(string);
		return field;
	}

}
