package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.*;
import com.gentics.mesh.core.rest.node.field.impl.*;
import com.gentics.mesh.core.rest.schema.*;
import com.gentics.mesh.core.rest.schema.impl.*;

/**
 * This enum stores all needed references to interfaces and implementations for the various field types. Each field has a fieldschema, fieldschema impl, rest
 * POJO model interface, rest POJO model implementation.
 */
public enum FieldTypes {

	STRING(StringFieldSchema.class, StringFieldSchemaImpl.class, StringFieldModel.class, StringFieldImpl.class),

	HTML(HtmlFieldSchema.class, HtmlFieldSchemaImpl.class, HtmlFieldModel.class, HtmlFieldImpl.class),

	NUMBER(NumberFieldSchema.class, NumberFieldSchemaImpl.class, NumberFieldModel.class, NumberFieldImpl.class),

	DATE(DateFieldSchema.class, DateFieldSchemaImpl.class, DateFieldModel.class, DateFieldImpl.class),

	BOOLEAN(BooleanFieldSchema.class, BooleanFieldSchemaImpl.class, BooleanFieldModel.class, BooleanFieldImpl.class),

	NODE(NodeFieldSchema.class, NodeFieldSchemaImpl.class, NodeFieldModel.class, NodeFieldImpl.class),

	LIST(ListFieldSchema.class, ListFieldSchemaImpl.class, ListFieldModel.class, ListFieldModel.class),

	BINARY(BinaryFieldSchema.class, BinaryFieldSchemaImpl.class, BinaryFieldModel.class, BinaryFieldImpl.class),

	S3BINARY(S3BinaryFieldSchema.class, S3BinaryFieldSchemaImpl.class, S3BinaryFieldModel.class, S3BinaryFieldImpl.class),

	MICRONODE(MicronodeFieldSchema.class, MicronodeFieldSchemaImpl.class, MicronodeFieldModel.class, MicronodeResponse.class);

	private Class<? extends FieldSchema> schemaInterfaceClazz;

	private Class<? extends FieldSchema> schemaImplementationClazz;

	private Class<? extends FieldModel> fieldInterfaceClass;

	private Class<? extends FieldModel> fieldImplementationClass;

	private FieldTypes(Class<? extends FieldSchema> schemaInterfaceClazz, Class<? extends FieldSchema> schemaImplementationClazz,
		Class<? extends FieldModel> fieldInterfaceClass, Class<? extends FieldModel> fieldImplementationClass) {
		this.schemaInterfaceClazz = schemaInterfaceClazz;
		this.schemaImplementationClazz = schemaImplementationClazz;
		this.fieldImplementationClass = fieldImplementationClass;
		this.fieldInterfaceClass = fieldInterfaceClass;
	}

	public Class<? extends FieldSchema> getSchemaImplementationClazz() {
		return schemaImplementationClazz;
	}

	public Class<? extends FieldSchema> getSchemaInterfaceClazz() {
		return schemaInterfaceClazz;
	}

	public Class<? extends FieldModel> getFieldImplementationClass() {
		return fieldImplementationClass;
	}

	public Class<? extends FieldModel> getFieldInterfaceClass() {
		return fieldInterfaceClass;
	}

	/**
	 * Return the name of the type.
	 */
	public String toString() {
		return name().toLowerCase();
	}

	/**
	 * Convert the given field type name to a field type object.
	 * 
	 * @param name
	 * @return
	 */
	public static FieldTypes valueByName(String name) {
		for (FieldTypes type : values()) {
			if (type.toString().equals(name)) {
				return type;
			}
		}
		return null;
	}

}
