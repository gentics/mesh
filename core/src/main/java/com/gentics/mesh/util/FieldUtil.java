package com.gentics.mesh.util;

import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

public final class FieldUtil {

	public static StringFieldSchema createStringFieldSchema(String defaultString) {
		StringFieldSchema fieldSchema = new StringFieldSchemaImpl();
		fieldSchema.setString(defaultString);
		return fieldSchema;
	}

	public static StringField createStringField(String string) {
		StringField field = new StringFieldImpl();
		field.setString(string);
		return field;
	}

}
