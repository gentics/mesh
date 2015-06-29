package com.gentics.mesh.core.rest.common.response;

import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.HTMLFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HTMLFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SelectFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

public enum FieldTypes {
	STRING(StringFieldSchema.class, StringFieldSchemaImpl.class), HTML(HTMLFieldSchema.class, HTMLFieldSchemaImpl.class), NUMBER(
			NumberFieldSchema.class, NumberFieldSchemaImpl.class), DATE(DateFieldSchema.class, DateFieldSchemaImpl.class), BOOLEAN(
			BooleanFieldSchema.class, BooleanFieldSchemaImpl.class), SELECT(SelectFieldSchema.class, SelectFieldSchemaImpl.class), NODE(
			NodeFieldSchema.class, NodeFieldSchemaImpl.class), LIST(ListFieldSchema.class, ListFieldSchemaImpl.class), MICROSCHEMA(
			MicroschemaFieldSchema.class, MicroschemaFieldSchemaImpl.class);

	private Class<? extends FieldSchema> interfaceClazz;

	private Class<? extends FieldSchema> implementationClazz;

	private FieldTypes(Class<? extends FieldSchema> interfaceClazz, Class<? extends FieldSchema> implementationClazz) {
		this.interfaceClazz = interfaceClazz;
		this.implementationClazz = implementationClazz;
	}

	public Class<? extends FieldSchema> getSchemaInterface() {
		return interfaceClazz;
	}

	public Class<? extends FieldSchema> getSchemaClass() {
		return implementationClazz;
	}

	public String toString() {
		return name().toLowerCase();
	}

	public static FieldTypes valueByName(String name) {
		for (FieldTypes type : values()) {
			if (type.toString().equals(name)) {
				return type;
			}
		}
		return null;
	}

}
