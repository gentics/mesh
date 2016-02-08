package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.Optional;

import com.gentics.mesh.core.data.schema.FieldTypeChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

/**
 * @see FieldTypeChange
 */
public class FieldTypeChangeImpl extends AbstractSchemaFieldChange implements FieldTypeChange {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.CHANGEFIELDTYPE;

	/**
	 * Apply the field type change to the specified schema.
	 */
	@Override
	public Schema apply(Schema schema) {
		Optional<FieldSchema> fieldSchema = schema.getFieldSchema(getFieldName());

		if (!fieldSchema.isPresent()) {
			throw error(BAD_REQUEST,
					"Could not find schema field {" + getFieldName() + "} within schema {" + schema.getName() + "} for change {" + getUuid() + "}");
		}

		FieldSchema field = null;
		String newType = getFieldProperty("newType");
		if (newType != null) {

			switch (newType) {
			case "boolean":
				field = new BooleanFieldSchemaImpl();
				break;
			case "number":
				field = new NumberFieldSchemaImpl();
				break;
			case "date":
				field = new DateFieldSchemaImpl();
				break;
			case "html":
				field = new HtmlFieldSchemaImpl();
				break;
			case "string":
				field = new StringFieldSchemaImpl();
				break;
			case "list":
				ListFieldSchema listField = new ListFieldSchemaImpl();
				listField.setListType(getFieldProperty("listType"));
				field = listField;
				break;
			case "micronode":
				field = new MicronodeFieldSchemaImpl();
				break;
			case "node":
				field = new NodeFieldSchemaImpl();
				break;
			default:
				throw error(BAD_REQUEST, "Unknown type {" + newType + "} for change " + getUuid());
			}
			field.setRequired(fieldSchema.get().isRequired());
			field.setLabel(fieldSchema.get().getLabel());
			field.setName(fieldSchema.get().getName());
			// Remove the old field
			schema.removeField(fieldSchema.get().getName());
			// Add the new field
			schema.addField(field);
		} else {
			throw error(BAD_REQUEST, "New type was not specified for change {" + getUuid() + "}");
		}
		return schema;
	}

	@Override
	public String getAutoMigrationScript() throws IOException {
		String newType = getFieldProperty("type");
		if (newType != null) {
			switch (newType) {
			case "binary":
				return loadAutoMigrationScript("typechange_binary.js");
			case "boolean":
				return loadAutoMigrationScript("typechange_boolean.js");
			case "date":
				return loadAutoMigrationScript("typechange_date.js");
			case "micronode":
				return loadAutoMigrationScript("typechange_micronode.js");
			case "node":
				return loadAutoMigrationScript("typechange_node.js");
			case "number":
				return loadAutoMigrationScript("typechange_number.js");
			case "html":
			case "string":
				return loadAutoMigrationScript("typechange_string.js");
			case "list":
				String newListType = getFieldProperty("listType");
				if (newListType != null) {
					switch (newListType) {
					case "boolean":
						return loadAutoMigrationScript("typechange_booleanlist.js");
					case "date":
						return loadAutoMigrationScript("typechange_datelist.js");
					case "micronode":
						return loadAutoMigrationScript("typechange_micronodelist.js");
					case "node":
						return loadAutoMigrationScript("typechange_nodelist.js");
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
