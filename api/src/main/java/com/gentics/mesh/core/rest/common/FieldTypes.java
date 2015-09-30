package com.gentics.mesh.core.rest.common;

import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.ListField;
import com.gentics.mesh.core.rest.node.field.MicroschemaField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.SelectField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.ListFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.MicroschemaFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.SelectFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.SelectFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SelectFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

/**
 * This enum stores all needed references to interfaces and implementations for the various field types. Each field has a fieldschema, fieldschema impl, rest
 * pojo model interface, rest pojo model implementation.
 */
public enum FieldTypes {
	STRING(StringFieldSchema.class, StringFieldSchemaImpl.class, StringField.class, StringFieldImpl.class), HTML(HtmlFieldSchema.class,
			HtmlFieldSchemaImpl.class, HtmlField.class, HtmlFieldImpl.class), NUMBER(NumberFieldSchema.class, NumberFieldSchemaImpl.class,
					NumberField.class,
					NumberFieldImpl.class), DATE(DateFieldSchema.class, DateFieldSchemaImpl.class, DateField.class, DateFieldImpl.class), BOOLEAN(
							BooleanFieldSchema.class, BooleanFieldSchemaImpl.class, BooleanField.class,
							BooleanFieldImpl.class), SELECT(SelectFieldSchema.class, SelectFieldSchemaImpl.class, SelectField.class,
									SelectFieldImpl.class), NODE(NodeFieldSchema.class, NodeFieldSchemaImpl.class, NodeField.class,
											NodeFieldImpl.class), LIST(ListFieldSchema.class, ListFieldSchemaImpl.class, ListField.class,
													ListFieldImpl.class), MICROSCHEMA(MicroschemaFieldSchema.class, MicroschemaFieldSchemaImpl.class,
															MicroschemaField.class, MicroschemaFieldImpl.class);

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

	public static FieldTypes valueByName(String name) {
		for (FieldTypes type : values()) {
			if (type.toString().equals(name)) {
				return type;
			}
		}
		return null;
	}

}
