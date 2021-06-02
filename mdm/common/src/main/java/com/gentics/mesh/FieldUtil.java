package com.gentics.mesh;

import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.MicroschemaVersionModel;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.field.*;
import com.gentics.mesh.core.rest.node.field.impl.*;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.*;
import com.gentics.mesh.core.rest.schema.impl.*;
import com.gentics.mesh.util.Tuple;

/**
 * Utility class that is commonly used for tests and the RAML generator.
 */
public final class FieldUtil {

	/**
	 * Create a minimal valid test schema..
	 * 
	 * @return
	 */
	public static SchemaCreateRequest createMinimalValidSchemaCreateRequest() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("test");
		request.validate();
		return request;
	}

	public static SchemaVersionModel createMinimalValidSchema() {
		SchemaVersionModel request = new SchemaModelImpl();
		request.setName("test");
		request.validate();
		return request;
	}

	/**
	 * Create a minimal valid test schema create request.
	 * 
	 * @return
	 */
	public static SchemaCreateRequest createSchemaCreateRequest() {
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("test");
		request.validate();
		return request;
	}

	/**
	 * Create a minimal valid test microschema.
	 * 
	 * @return
	 */
	public static MicroschemaVersionModel createMinimalValidMicroschema() {
		MicroschemaVersionModel schema = new MicroschemaModelImpl();
		schema.setName("test");
		schema.validate();
		return schema;
	}

	/**
	 * Create a minimal valid test microschema.
	 * 
	 * @return
	 */
	public static MicroschemaUpdateRequest createMinimalValidMicroschemaUpdateRequest() {
		MicroschemaUpdateRequest schema = new MicroschemaUpdateRequest();
		schema.setName("test");
		schema.validate();
		return schema;
	}

	/**
	 * Create minimal create request.
	 * 
	 * @return
	 */
	public static MicroschemaCreateRequest createMinimalValidMicroschemaCreateRequest() {
		MicroschemaCreateRequest schema = new MicroschemaCreateRequest();
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

	/**
	 * Create a string field and set the given value.
	 * 
	 * @param stringValue
	 * @return
	 */
	public static StringField createStringField(String stringValue) {
		StringField field = new StringFieldImpl();
		field.setString(stringValue);
		return field;
	}

	public static HtmlField createHtmlField(String htmlValue) {
		HtmlField field = new HtmlFieldImpl();
		field.setHTML(htmlValue);
		return field;
	}

	public static BinaryField createBinaryField(String uuid, String fileName, String hashSum) {
		BinaryField field = new BinaryFieldImpl();
		field.setBinaryUuid(uuid);
		field.setFileName(fileName);
		field.setSha512sum(hashSum);
		return field;
	}

	public static S3BinaryField createS3BinaryField(String fileName) {
		S3BinaryField field = new S3BinaryFieldImpl();
		field.setFileName(fileName);
		return field;
	}

	/**
	 * Create a number field and set the given value.
	 * 
	 * @param numberValue
	 * @return
	 */
	public static NumberField createNumberField(Number numberValue) {
		NumberField field = new NumberFieldImpl();
		field.setNumber(numberValue);
		return field;
	}

	/**
	 * Create a boolean field and set the given value.
	 * 
	 * @param value
	 * @return
	 */
	public static BooleanField createBooleanField(Boolean value) {
		BooleanField field = new BooleanFieldImpl();
		field.setValue(value);
		return field;
	}

	public static DateField createDateField(String iso8601Date) {
		DateField field = new DateFieldImpl();
		field.setDate(iso8601Date);
		return field;
	}

	/**
	 * Create a node field and set the uuid as node reference.
	 * 
	 * @param uuid
	 * @return
	 */
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

	public static DateFieldListImpl createDateListField(String... values) {
		DateFieldListImpl field = new DateFieldListImpl();
		for (String value : values) {
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
		MicroschemaReferenceImpl microschemaReference = new MicroschemaReferenceImpl();
		microschemaReference.setName(microschema);
		field.setMicroschema(microschemaReference);

		for (Tuple<String, Field> tuple : fields) {
			field.getFields().put(tuple.v1(), tuple.v2());
		}

		return field;
	}

	@SafeVarargs
	public static MicronodeField createMicronodeField(String microschema, Tuple<String, Field>... fields) {
		MicronodeResponse field = (MicronodeResponse) createNewMicronodeField(microschema, fields);
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

	public static S3BinaryFieldSchema createS3BinaryFieldSchema(String name) {
		S3BinaryFieldSchema field = new S3BinaryFieldSchemaImpl();
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
