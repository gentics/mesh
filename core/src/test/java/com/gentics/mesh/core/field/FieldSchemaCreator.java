package com.gentics.mesh.core.field;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;

/**
 * Test helper for schema field creation.
 */
@FunctionalInterface
public interface FieldSchemaCreator {
	public final static FieldSchemaCreator CREATEBINARY = name -> FieldUtil.createBinaryFieldSchema(name);
	public final static FieldSchemaCreator CREATEBOOLEAN = name -> FieldUtil.createBooleanFieldSchema(name);
	public final static FieldSchemaCreator CREATEBOOLEANLIST = name -> FieldUtil.createListFieldSchema(name, "boolean");
	public final static FieldSchemaCreator CREATEDATE = name -> FieldUtil.createDateFieldSchema(name);
	public final static FieldSchemaCreator CREATEDATELIST = name -> FieldUtil.createListFieldSchema(name, "date");
	public final static FieldSchemaCreator CREATEHTML = name -> FieldUtil.createHtmlFieldSchema(name);
	public final static FieldSchemaCreator CREATEHTMLLIST = name -> FieldUtil.createListFieldSchema(name, "html");
	public final static FieldSchemaCreator CREATEMICRONODE = name -> {
		MicronodeFieldSchema schema = FieldUtil.createMicronodeFieldSchema(name);
		schema.setAllowedMicroSchemas(new String[] { "vcard" });
		return schema;
	};
	public final static FieldSchemaCreator CREATEMICRONODELIST = name -> {
		ListFieldSchema schema = FieldUtil.createListFieldSchema(name, "micronode");
		schema.setAllowedSchemas(new String[] { "vcard" });
		return schema;
	};
	public final static FieldSchemaCreator CREATENODE = name -> FieldUtil.createNodeFieldSchema(name);
	public final static FieldSchemaCreator CREATENODELIST = name -> FieldUtil.createListFieldSchema(name, "node");
	public final static FieldSchemaCreator CREATENUMBER = name -> FieldUtil.createNumberFieldSchema(name);
	public final static FieldSchemaCreator CREATENUMBERLIST = name -> FieldUtil.createListFieldSchema(name, "number");
	public final static FieldSchemaCreator CREATESTRING = name -> FieldUtil.createStringFieldSchema(name);
	public final static FieldSchemaCreator CREATESTRINGLIST = name -> FieldUtil.createListFieldSchema(name, "string");

	FieldSchema create(String name);
}
