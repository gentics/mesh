package com.gentics.mesh.util;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

/**
 * Utility class that is commonly used for tests and the RAML generator.
 */
public final class FieldUtil {

	/**
	 * Create a new string field schema.
	 * 
	 * @param name
	 *            Name of the field schema
	 * @return
	 */
	public static StringFieldSchema createStringFieldSchema(String name) {
		StringFieldSchema fieldSchema = new StringFieldSchemaImpl();
		fieldSchema.setName(name);
		return fieldSchema;
	}

	public static StringField createStringField(String string) {
		StringField field = new StringFieldImpl();
		field.setString(string);
		return field;
	}

	public static Field createHtmlField(String html) {
		HtmlField field = new HtmlFieldImpl();
		field.setHTML(html);
		return field;
	}

	public static Field createNumberField(Number number) {
		NumberField field = new NumberFieldImpl();
		field.setNumber(number);
		return field;
	}

	public static Field createBooleanField(Boolean value) {
		BooleanField field = new BooleanFieldImpl();
		field.setValue(value);
		return field;
	}

	public static Field createDateField(Long date) {
		DateField field = new DateFieldImpl();
		field.setDate(date);
		return field;
	}

	public static Field createNodeField(String uuid) {
		NodeFieldImpl field = new NodeFieldImpl();
		field.setUuid(uuid);
		return field;
	}

	public static Field createNodeListField(String... uuids) {
		NodeFieldListImpl field = new NodeFieldListImpl();
		for (String uuid : uuids) {
			field.add(new NodeFieldListItemImpl(uuid));
		}
		return field;
	}

	public static Field createBooleanListField(Boolean... values) {
		BooleanFieldListImpl field = new BooleanFieldListImpl();
		for (Boolean value : values) {
			field.add(value);
		}
		return field;
	}

	public static Field createDateListField(Long... values) {
		DateFieldListImpl field = new DateFieldListImpl();
		for (Long value : values) {
			field.add(value);
		}
		return field;
	}

	public static Field createNumberListField(Number... numbers) {
		NumberFieldListImpl field = new NumberFieldListImpl();
		for (Number number : numbers) {
			field.add(number);
		}
		return field;
	}

	public static Field createHtmlListField(String... values) {
		HtmlFieldListImpl field = new HtmlFieldListImpl();
		for (String value : values) {
			field.add(value);
		}
		return field;
	}

	public static Field createStringListField(String... strings) {
		StringFieldListImpl field = new StringFieldListImpl();
		for (String string : strings) {
			field.add(string);
		}
		return field;
	}

	@SafeVarargs
	public static MicronodeField createNewMicronodeField(String microschema, Tuple<String, Field>... fields) {
		MicronodeResponse field = new MicronodeResponse();
		MicroschemaReference microschemaReference = new MicroschemaReference();
		microschemaReference.setName(microschema);
		microschemaReference.setUuid(UUIDUtil.randomUUID());
		field.setMicroschema(microschemaReference);

		for (Tuple<String, Field> tuple : fields) {
			field.getFields().put(tuple.v1(), tuple.v2());
		}

		return field;
	}

	@SafeVarargs
	public static MicronodeField createMicronodeField(String microschema, Tuple<String, Field>... fields) {
		MicronodeResponse field = (MicronodeResponse) createNewMicronodeField(microschema, fields);
		field.setUuid(UUIDUtil.randomUUID());

		return field;
	}

	public static Field createMicronodeListField(MicronodeField... micronodes) {
		MicronodeFieldListImpl field = new MicronodeFieldListImpl();
		for (MicronodeField micronode : micronodes) {
			field.add(micronode);
		}
		return field;
	}

	public static BinaryFieldSchema createBinaryFieldSchema(String name) {
		BinaryFieldSchema field = new BinaryFieldSchemaImpl();
		field.setName(name);
		return field;
	}
}
