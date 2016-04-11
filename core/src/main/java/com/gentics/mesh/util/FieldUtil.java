package com.gentics.mesh.util;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
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
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

/**
 * Utility class that is commonly used for tests and the RAML generator.
 */
public final class FieldUtil {

	/**
	 * Create a minimal valid test schema.
	 * 
	 * @return
	 */
	public static Schema createMinimalValidSchema() {
		Schema schema = new SchemaModel();
		schema.setName("test");
		schema.setDisplayField("displayFieldName");
		schema.addField(createStringFieldSchema("displayFieldName"));
		schema.validate();
		return schema;
	}

	/**
	 * Create a minimal valid test microschema.
	 * 
	 * @return
	 */
	public static Microschema createMinimalValidMicroschema() {
		Microschema schema = new MicroschemaModel();
		schema.setName("test");
		schema.validate();
		return schema;
	}

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

	public static HtmlField createHtmlField(String html) {
		HtmlField field = new HtmlFieldImpl();
		field.setHTML(html);
		return field;
	}

	public static NumberField createNumberField(Number number) {
		NumberField field = new NumberFieldImpl();
		field.setNumber(number);
		return field;
	}

	public static BooleanField createBooleanField(Boolean value) {
		BooleanField field = new BooleanFieldImpl();
		field.setValue(value);
		return field;
	}

	public static DateField createDateField(Long date) {
		DateField field = new DateFieldImpl();
		field.setDate(date);
		return field;
	}

	public static NodeFieldImpl createNodeField(String uuid) {
		NodeFieldImpl field = new NodeFieldImpl();
		field.setUuid(uuid);
		return field;
	}

	public static NodeFieldListImpl createNodeListField(String... uuids) {
		NodeFieldListImpl field = new NodeFieldListImpl();
		for (String uuid : uuids) {
			field.add(new NodeFieldListItemImpl(uuid));
		}
		return field;
	}

	public static BooleanFieldListImpl createBooleanListField(Boolean... values) {
		BooleanFieldListImpl field = new BooleanFieldListImpl();
		for (Boolean value : values) {
			field.add(value);
		}
		return field;
	}

	public static DateFieldListImpl createDateListField(Long... values) {
		DateFieldListImpl field = new DateFieldListImpl();
		for (Long value : values) {
			field.add(value);
		}
		return field;
	}

	public static NumberFieldListImpl createNumberListField(Number... numbers) {
		NumberFieldListImpl field = new NumberFieldListImpl();
		for (Number number : numbers) {
			field.add(number);
		}
		return field;
	}

	public static HtmlFieldListImpl createHtmlListField(String... values) {
		HtmlFieldListImpl field = new HtmlFieldListImpl();
		for (String value : values) {
			field.add(value);
		}
		return field;
	}

	public static StringFieldListImpl createStringListField(String... strings) {
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

	public static BooleanFieldSchema createBooleanFieldSchema(String name) {
		BooleanFieldSchema field = new BooleanFieldSchemaImpl();
		field.setName(name);
		return field;
	}

	public static DateFieldSchema createDateFieldSchema(String name) {
		DateFieldSchema field = new DateFieldSchemaImpl();
		field.setName(name);
		return field;
	}

	public static HtmlFieldSchema createHtmlFieldSchema(String name) {
		HtmlFieldSchema field = new HtmlFieldSchemaImpl();
		field.setName(name);
		return field;
	}

	public static MicronodeFieldSchema createMicronodeFieldSchema(String name) {
		MicronodeFieldSchema field = new MicronodeFieldSchemaImpl();
		field.setName(name);
		return field;
	}

	public static NodeFieldSchema createNodeFieldSchema(String name) {
		NodeFieldSchema field = new NodeFieldSchemaImpl();
		field.setName(name);
		return field;
	}

	public static NumberFieldSchema createNumberFieldSchema(String name) {
		NumberFieldSchema field = new NumberFieldSchemaImpl();
		field.setName(name);
		return field;
	}

	public static ListFieldSchema createListFieldSchema(String name, String listType) {
		ListFieldSchema field = new ListFieldSchemaImpl();
		field.setName(name);
		field.setListType(listType);
		return field;
	}

	public static ListFieldSchema createListFieldSchema(String name) {
		ListFieldSchema field = new ListFieldSchemaImpl();
		field.setName(name);
		return field;
	}
}
