package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.handler.TypeConverter;
import com.gentics.mesh.core.data.schema.FieldTypeChange;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

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
			throw error(BAD_REQUEST, "schema_error_change_field_not_found", getFieldName(), container.getName(), getUuid());
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
	public Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String newType = getType();

		switch (newType) {
			case "boolean":
				return Collections.singletonMap(getFieldName(), changeToBoolean(oldSchema, oldContent));
			case "number":
				return Collections.singletonMap(getFieldName(), changeToNumber(oldSchema, oldContent));
			case "date":
				return Collections.singletonMap(getFieldName(), changeToDate(oldSchema, oldContent));
			case "html":
				return Collections.singletonMap(getFieldName(), changeToHtml(oldSchema, oldContent));
			case "string":
				return Collections.singletonMap(getFieldName(), changeToString(oldSchema, oldContent));
			case "binary":
				return Collections.singletonMap(getFieldName(), changeToBinary(oldSchema, oldContent));
			case "list":
				return Collections.singletonMap(getFieldName(), changeToList(oldSchema, oldContent));
			case "micronode":
				return Collections.singletonMap(getFieldName(), changeToMicronode(oldSchema, oldContent));
			case "node":
				return Collections.singletonMap(getFieldName(), changeToNode(oldSchema, oldContent));
			default:
				throw error(BAD_REQUEST, "Unknown type {" + newType + "} for change " + getUuid());
		}
	}

	private BooleanField changeToBoolean(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		TypeConverter typeConverter = new TypeConverter();

		return new BooleanFieldImpl().setValue(typeConverter.toBoolean(oldContent.getFields().getField(fieldName, fieldSchema).toJson()));
	}

	private NumberField changeToNumber(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		TypeConverter typeConverter = new TypeConverter();

		if (fieldSchema.getType().equals("number")) {
			return oldContent.getFields().getNumberField(fieldName);
		} else {
			return new NumberFieldImpl().setNumber(typeConverter.toNumber(oldContent.getFields().getField(fieldName, fieldSchema).toJson()));
		}
	}

	private DateField changeToDate(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		TypeConverter typeConverter = new TypeConverter();

		return new DateFieldImpl().setDate(typeConverter.toDate(oldContent.getFields().getField(fieldName, fieldSchema).toJson()));
	}

	private HtmlField changeToHtml(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		TypeConverter typeConverter = new TypeConverter();

		return new HtmlFieldImpl().setHTML(typeConverter.toString(oldContent.getFields().getField(fieldName, fieldSchema).toJson()));
	}

	private StringField changeToString(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		TypeConverter typeConverter = new TypeConverter();

		return new StringFieldImpl().setString(typeConverter.toString(oldContent.getFields().getField(fieldName, fieldSchema).toJson()));
	}

	private BinaryField changeToBinary(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		TypeConverter typeConverter = new TypeConverter();

		return null;
	}

	private FieldList changeToList(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		TypeConverter typeConverter = new TypeConverter();
		String listType = getListType();
		String oldValue = oldContent.getFields().getField(fieldName, fieldSchema).toJson();

		switch (listType) {
			case "boolean":
				Boolean[] booleanList = typeConverter.toBooleanList(oldValue);
				return booleanList == null
					? null
					: new BooleanFieldListImpl().setItems(Arrays.asList(booleanList));
			case "number":
				if (fieldSchema.getType().equals("number")) {
					return new NumberFieldListImpl().setItems(Collections.singletonList(oldContent.getFields().getNumberField(fieldName).getNumber()));
				} else if (fieldSchema instanceof ListFieldSchema && ((ListFieldSchema) fieldSchema).getListType().equals("number")) {
					return oldContent.getFields().getNumberFieldList(fieldName);
				}
				return new NumberFieldListImpl().setItems(Arrays.asList(typeConverter.toNumberList(oldValue)));
			case "date":
				return new DateFieldListImpl().setItems(Arrays.asList(typeConverter.toDateList(oldValue)));
			case "html":
				return new HtmlFieldListImpl().setItems(Arrays.asList(typeConverter.toStringList(oldValue)));
			case "string":
				return new StringFieldListImpl().setItems(Arrays.asList(typeConverter.toStringList(oldValue)));
			case "micronode":
//				return new MicronodeFieldListImpl().setItems(Arrays.asList(typeConverter.toMicronodeList(oldValue)));
				return null;
			case "node":
//				return new NodeFieldListImpl().setItems(Arrays.asList(typeConverter.toNodeList(oldValue)));
				return null;
			default:
				throw error(BAD_REQUEST, "Unknown list type {" + listType + "} for change " + getUuid());
		}
	}

	private MicronodeField changeToMicronode(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		TypeConverter typeConverter = new TypeConverter();

		return null;
	}

	private NodeField changeToNode(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		TypeConverter typeConverter = new TypeConverter();

		return null;
	}

	@Override
	public void delete(BulkActionContext bac) {
		getElement().remove();
	}

}
