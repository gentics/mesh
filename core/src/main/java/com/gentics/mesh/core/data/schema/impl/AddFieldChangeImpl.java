package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.data.schema.AddFieldChange;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

/**
 * Change entry which contains information for a field to be added to the schema.
 */
public class AddFieldChangeImpl extends AbstractSchemaFieldChange implements AddFieldChange {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.ADDFIELD;
	private static final String FIELD_TYPE_KEY = "fieldType";

	@Override
	public AddFieldChange setType(String type) {
		setProperty(FIELD_TYPE_KEY, type);
		return this;
	}

	@Override
	public String getType() {
		return getProperty(FIELD_TYPE_KEY);
	}

	@Override
	public Schema apply(Schema schema) {

		//TODO avoid case switches like this. We need a central delegator implementation which will be used in multiple places
		switch (getType()) {
		case "html":
			schema.addField(new HtmlFieldSchemaImpl().setName(getFieldName()));
			break;
		case "string":
			schema.addField(new StringFieldSchemaImpl().setName(getFieldName()));
			break;
		case "number":
			schema.addField(new NumberFieldSchemaImpl().setName(getFieldName()));
			break;

		default:
			throw error(BAD_REQUEST, "Unknown type");
		}
		return schema;
	}

}
