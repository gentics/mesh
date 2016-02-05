package com.gentics.mesh.core.data.schema.impl;

import java.io.IOException;

import com.gentics.mesh.core.data.schema.UpdateFieldChange;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry which contains information for a field update. This can include field specific settings or even a field type change.
 */
public class UpdateFieldChangeImpl extends AbstractSchemaFieldChange implements UpdateFieldChange {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATEFIELD;
	private static final String FIELD_PROPERTY_PREFIX_KEY = "fieldProperty_";

	@Override
	public Schema apply(Schema schema) {
		return schema;
	}

	@Override
	public void setFieldProperty(String key, String value) {
		setProperty(FIELD_PROPERTY_PREFIX_KEY + key, value);
	}

	@Override
	public String getFieldProperty(String key) {
		return getProperty(FIELD_PROPERTY_PREFIX_KEY + key);
	}

	@Override
	public String getAutoMigrationScript() throws IOException {
		String newType = getFieldProperty("type");
		if (newType != null) {
			switch (newType) {
			case "boolean":
				return loadAutoMigrationScript("typechange_boolean.js");
			case "number":
				return loadAutoMigrationScript("typechange_number.js");
			case "html":
			case "string":
				return loadAutoMigrationScript("typechange_string.js");
			case "list":
				String newListType = getFieldProperty("listType");
				if (newListType != null) {
					switch(newListType) {
					case "boolean":
						return loadAutoMigrationScript("typechange_booleanlist.js");
					case "number":
						return loadAutoMigrationScript("typechange_numberlist.js");
					case "html":
					case "string":
						return loadAutoMigrationScript("typechange_stringlist.js");
					}
				}
			}
		}

		return null;
	}
}
