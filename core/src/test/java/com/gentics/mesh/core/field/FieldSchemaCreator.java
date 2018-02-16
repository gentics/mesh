package com.gentics.mesh.core.field;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;

@FunctionalInterface
public interface FieldSchemaCreator {
	FieldSchemaCreator CREATEBINARY = FieldUtil::createBinaryFieldSchema;
	FieldSchemaCreator CREATEBOOLEAN = FieldUtil::createBooleanFieldSchema;
	FieldSchemaCreator CREATEBOOLEANLIST = name -> FieldUtil.createListFieldSchema(name, "boolean");
	FieldSchemaCreator CREATEDATE = FieldUtil::createDateFieldSchema;
	FieldSchemaCreator CREATEDATELIST = name -> FieldUtil.createListFieldSchema(name, "date");
	FieldSchemaCreator CREATEHTML = FieldUtil::createHtmlFieldSchema;
	FieldSchemaCreator CREATEHTMLLIST = name -> FieldUtil.createListFieldSchema(name, "html");
	FieldSchemaCreator CREATEMICRONODE = name -> {
		MicronodeFieldSchema schema = FieldUtil.createMicronodeFieldSchema(name);
		schema.setAllowedMicroSchemas(new String[] { "vcard" });
		return schema;
	};
	FieldSchemaCreator CREATEMICRONODELIST = name -> {
		ListFieldSchema schema = FieldUtil.createListFieldSchema(name, "micronode");
		schema.setAllowedSchemas(new String[] { "vcard" });
		return schema;
	};
	FieldSchemaCreator CREATENODE = FieldUtil::createNodeFieldSchema;
	FieldSchemaCreator CREATENODELIST = name -> FieldUtil.createListFieldSchema(name, "node");
	FieldSchemaCreator CREATENUMBER = FieldUtil::createNumberFieldSchema;
	FieldSchemaCreator CREATENUMBERLIST = name -> FieldUtil.createListFieldSchema(name, "number");
	FieldSchemaCreator CREATESTRING = FieldUtil::createStringFieldSchema;
	FieldSchemaCreator CREATESTRINGLIST = name -> FieldUtil.createListFieldSchema(name, "string");

	FieldSchema create(String name);
}
