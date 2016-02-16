package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.data.schema.AddFieldChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * @see AddFieldChange
 */
public class AddFieldChangeImpl extends AbstractSchemaFieldChange implements AddFieldChange {

	public static void checkIndices(Database database) {
		database.addVertexType(AddFieldChangeImpl.class);
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	public AddFieldChange setType(String type) {
		setRestProperty(SchemaChangeModel.TYPE_KEY, type);
		return this;
	}

	@Override
	public String getType() {
		return getRestProperty(SchemaChangeModel.TYPE_KEY);
	}

	@Override
	public String getListType() {
		return getRestProperty(SchemaChangeModel.LIST_TYPE_KEY);
	}

	@Override
	public void setListType(String type) {
		setRestProperty(SchemaChangeModel.LIST_TYPE_KEY, type);
	}

	@Override
	public FieldSchemaContainer apply(FieldSchemaContainer container) {

		//TODO avoid case switches like this. We need a central delegator implementation which will be used in multiple places
		switch (getType()) {
		case "html":
			container.addField(new HtmlFieldSchemaImpl().setName(getFieldName()));
			break;
		case "string":
			container.addField(new StringFieldSchemaImpl().setName(getFieldName()));
			break;
		case "number":
			container.addField(new NumberFieldSchemaImpl().setName(getFieldName()));
			break;
		case "binary":
			container.addField(new BinaryFieldSchemaImpl().setName(getFieldName()));
			break;
		case "node":
			container.addField(new NodeFieldSchemaImpl().setName(getFieldName()));
			break;
		case "micronode":
			container.addField(new MicronodeFieldSchemaImpl().setName(getFieldName()));
			break;
		case "date":
			container.addField(new DateFieldSchemaImpl().setName(getFieldName()));
			break;
		case "boolean":
			container.addField(new BooleanFieldSchemaImpl().setName(getFieldName()));
			break;
		case "list":
			ListFieldSchema field = new ListFieldSchemaImpl();
			field.setName(getFieldName());
			field.setListType(getListType());
			container.addField(field);
			break;
		default:
			throw error(BAD_REQUEST, "Unknown type");
		}
		return container;
	}

}
