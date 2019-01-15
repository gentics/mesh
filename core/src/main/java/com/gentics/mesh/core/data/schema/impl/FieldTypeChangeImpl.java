package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.FieldTypeChange;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
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
 * @see FieldTypeChange
 */
public class FieldTypeChangeImpl extends AbstractSchemaFieldChange implements FieldTypeChange {

	public static void init(Database database) {
		database.addVertexType(FieldTypeChangeImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	public String getType() {
		return getRestProperty(SchemaChangeModel.TYPE_KEY);
	}

	@Override
	public void setType(String type) {
		setRestProperty(SchemaChangeModel.TYPE_KEY, type);
	}

	@Override
	public String getListType() {
		return getRestProperty(SchemaChangeModel.LIST_TYPE_KEY);
	}

	@Override
	public void setListType(String listType) {
		setRestProperty(SchemaChangeModel.LIST_TYPE_KEY, listType);
	}

	/**
	 * Apply the field type change to the specified schema.
	 */
	@Override
	public FieldSchemaContainer apply(FieldSchemaContainer container) {
		FieldSchema fieldSchema = container.getField(getFieldName());

		if (fieldSchema == null) {
			throw error(BAD_REQUEST, "Could not find schema field {" + getFieldName() + "} within schema {" + container.getName() + "} for change {"
				+ getUuid() + "}");
		}

		FieldSchema field = null;
		String newType = getType();
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
				listField.setListType(getListType());
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
			field.setRequired(fieldSchema.isRequired());
			field.setLabel(fieldSchema.getLabel());
			field.setName(fieldSchema.getName());

			// Remove prefix from map keys
			Map<String, Object> properties = new HashMap<>();
			for (String key : getRestProperties().keySet()) {
				Object value = getRestProperties().get(key);
				key = key.replace(REST_PROPERTY_PREFIX_KEY, "");
				properties.put(key, value);
			}
			field.apply(properties);

			// Remove the old field
			container.removeField(fieldSchema.getName());
			// Add the new field
			container.addField(field);
		} else {
			throw error(BAD_REQUEST, "New type was not specified for change {" + getUuid() + "}");
		}
		return container;
	}

	@Override
	public String getAutoMigrationScript() throws IOException {
		return OPERATION.getAutoMigrationScript(Arrays.asList(SchemaChangeModel.TYPE_KEY, SchemaChangeModel.LIST_TYPE_KEY).stream()
			.filter(key -> getRestProperty(key) != null).collect(Collectors.toMap(key -> key, key -> getRestProperty(key))));
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

}
