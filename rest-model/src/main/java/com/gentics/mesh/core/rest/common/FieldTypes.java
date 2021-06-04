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

	STRING(StringFieldSchema.class, StringFieldSchemaImpl.class, StringField.class, StringFieldImpl.class),

	HTML(HtmlFieldSchema.class, HtmlFieldSchemaImpl.class, HtmlField.class, HtmlFieldImpl.class),

	NUMBER(NumberFieldSchema.class, NumberFieldSchemaImpl.class, NumberField.class, NumberFieldImpl.class),

	DATE(DateFieldSchema.class, DateFieldSchemaImpl.class, DateField.class, DateFieldImpl.class),

	BOOLEAN(BooleanFieldSchema.class, BooleanFieldSchemaImpl.class, BooleanField.class, BooleanFieldImpl.class),

	NODE(NodeFieldSchema.class, NodeFieldSchemaImpl.class, NodeField.class, NodeFieldImpl.class),

	LIST(ListFieldSchema.class, ListFieldSchemaImpl.class, ListField.class, ListField.class),

	BINARY(BinaryFieldSchema.class, BinaryFieldSchemaImpl.class, BinaryField.class, BinaryFieldImpl.class),

	S3BINARY(S3BinaryFieldSchema.class, S3BinaryFieldSchemaImpl.class, S3BinaryField.class, S3BinaryFieldImpl.class),

	MICRONODE(MicronodeFieldSchema.class, MicronodeFieldSchemaImpl.class, MicronodeField.class, MicronodeResponse.class);

	private Class<? extends FieldSchema> schemaInterfaceClazz;

	private Class<? extends FieldSchema> schemaImplementationClazz;

	private Class<? extends Field> fieldInterfaceClass;
	
	private Class<? extends Field> fieldImplementationClass;

	private FieldTypes(Class<? extends FieldSchema> schemaInterfaceClazz, Class<? extends FieldSchema> schemaImplementationClazz,
			Class<? extends Field> fieldInterfaceClass, Class<? extends Field> fieldImplementationClass) {
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

	public Class<? extends Field> getFieldImplementationClass() {
		return fieldImplementationClass;
	}

	public Class<? extends Field> getFieldInterfaceClass() {
		return fieldInterfaceClass;
	}

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
