package com.gentics.mesh.core.data.schema.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ADD_FIELD_AFTER_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ALLOW_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LIST_TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.TYPE_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.schema.AddFieldChange;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
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
import com.gentics.mesh.graphdb.spi.IndexHandler;
import com.gentics.mesh.graphdb.spi.TypeHandler;

/**
 * @see AddFieldChange
 */
public class AddFieldChangeImpl extends AbstractSchemaFieldChange implements AddFieldChange {

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(AddFieldChangeImpl.class, MeshVertexImpl.class);
	}

	@Override
	public SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	@Override
	public AddFieldChange setType(String type) {
		setRestProperty(TYPE_KEY, type);
		return this;
	}

	@Override
	public String getType() {
		return getRestProperty(TYPE_KEY);
	}

	@Override
	public String getListType() {
		return getRestProperty(LIST_TYPE_KEY);
	}

	@Override
	public String[] getAllowProp() {
		Object[] prop = getRestProperty(ALLOW_KEY);
		if (prop == null) {
			return null;
		}
		return Stream.of(prop)
			.map(item -> (String) item)
			.toArray(String[]::new);
	}

	@Override
	public void setListType(String type) {
		setRestProperty(LIST_TYPE_KEY, type);
	}

	@Override
	public void setInsertAfterPosition(String fieldName) {
		setRestProperty(ADD_FIELD_AFTER_KEY, fieldName);
	}

	@Override
	public String getInsertAfterPosition() {
		return getRestProperty(ADD_FIELD_AFTER_KEY);
	}

	@Override
	public String getLabel() {
		return getRestProperty(SchemaChangeModel.LABEL_KEY);
	}

	@Override
	public void setLabel(String label) {
		setRestProperty(SchemaChangeModel.LABEL_KEY, label);
	}

	@Override
	public Boolean getRequired() {
		return getRestProperty(SchemaChangeModel.REQUIRED_KEY);
	}

	@Override
	public FieldSchemaContainer apply(FieldSchemaContainer container) {

		String position = getInsertAfterPosition();
		FieldSchema field = null;
		// TODO avoid case switches like this. We need a central delegator implementation which will be used in multiple places
		switch (getType()) {
		case "html":
			field = new HtmlFieldSchemaImpl();
			break;
		case "string":
			StringFieldSchema stringField = new StringFieldSchemaImpl();
			stringField.setAllowedValues(getAllowProp());
			field = stringField;
			break;
		case "number":
			field = new NumberFieldSchemaImpl();
			break;
		case "binary":
			field = new BinaryFieldSchemaImpl();
			break;
		case "node":
			NodeFieldSchema nodeField = new NodeFieldSchemaImpl();
			nodeField.setAllowedSchemas(getAllowProp());
			field = nodeField;
			break;
		case "micronode":
			MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
			micronodeFieldSchema.setAllowedMicroSchemas(getAllowProp());
			field = micronodeFieldSchema;
			break;
		case "date":
			field = new DateFieldSchemaImpl();
			break;
		case "boolean":
			field = new BooleanFieldSchemaImpl();
			break;
		case "list":
			ListFieldSchema listField = new ListFieldSchemaImpl();
			listField.setListType(getListType());
			field = listField;
			switch (getListType()) {
			case "node":
			case "micronode":
				listField.setAllowedSchemas(getAllowProp());
				break;
			}
			break;
		default:
			throw error(BAD_REQUEST, "Unknown type");
		}
		field.setName(getFieldName());
		field.setLabel(getLabel());
		Boolean required = getRequired();
		if (required != null) {
			field.setRequired(required);
		}
		container.addField(field, position);
		return container;
	}

	@Override
	public Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		return Collections.singletonMap(getFieldName(), null);
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

}
