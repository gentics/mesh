package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.gentics.mesh.core.data.node.handler.TypeConverter;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.BinaryField;
import com.gentics.mesh.core.rest.node.field.BooleanField;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.JsonField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NumberField;
import com.gentics.mesh.core.rest.node.field.S3BinaryField;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.JsonFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.JsonFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.google.common.collect.ImmutableSet;

/**
 * Change entry which contains information for a field type change.
 */
public interface HibFieldTypeChange extends HibSchemaFieldChange {

	public static final Set<String> UUID_TYPES = ImmutableSet.of("binary", "node", "micronode");

	String REST_PROPERTY_PREFIX_KEY = "fieldProperty_";

	SchemaChangeOperation OPERATION = SchemaChangeOperation.CHANGEFIELDTYPE;

	@Override
	default SchemaChangeOperation getOperation() {
		return OPERATION;
	}


	/**
	 * Return the new field type value.
	 * 
	 * @return
	 */
	default String getType() {
		return getRestProperty(SchemaChangeModel.TYPE_KEY);
	}

	/**
	 * Set the new field type value.
	 * 
	 * @param type
	 */
	default void setType(String type) {
		setRestProperty(SchemaChangeModel.TYPE_KEY, type);
	}


	/**
	 * Return the new list type value.
	 * 
	 * @return
	 */
	default String getListType() {
		return getRestProperty(SchemaChangeModel.LIST_TYPE_KEY);
	}

	/**
	 * Set the new list type value.
	 * 
	 * @param listType
	 */
	default void setListType(String listType) {
		setRestProperty(SchemaChangeModel.LIST_TYPE_KEY, listType);
	}

	static TypeConverter typeConverter = new TypeConverter();

	/**
	 * Apply the field type change to the specified schema.
	 */
	@Override
	default <R extends FieldSchemaContainer> R apply(R container) {
		FieldSchema fieldSchema = container.getField(getFieldName());

		if (fieldSchema == null) {
			throw error(BAD_REQUEST, "schema_error_change_field_not_found", getFieldName(), container.getName(), getUuid());
		}

		FieldSchema field = null;
		String newType = getType();
		if (newType != null) {

			switch (FieldTypes.valueByName(newType)) {
				case BOOLEAN:
					field = new BooleanFieldSchemaImpl();
					break;
				case NUMBER:
					field = new NumberFieldSchemaImpl();
					break;
				case DATE:
					field = new DateFieldSchemaImpl();
					break;
				case HTML:
					field = new HtmlFieldSchemaImpl();
					break;
				case STRING:
					field = new StringFieldSchemaImpl();
					break;
				case BINARY:
					field = new BinaryFieldSchemaImpl();
					break;
				case S3BINARY:
					field = new S3BinaryFieldSchemaImpl();
					break;
				case LIST:
					ListFieldSchema listField = new ListFieldSchemaImpl();
					listField.setListType(getListType());
					field = listField;
					break;
				case MICRONODE:
					field = new MicronodeFieldSchemaImpl();
					break;
				case NODE:
					field = new NodeFieldSchemaImpl();
					break;
				case JSON:
					field = new JsonFieldSchemaImpl();
					break;
				default:
					throw error(BAD_REQUEST, "Unknown type {" + newType + "} for change " + getUuid());
			}
			field.setRequired(fieldSchema.isRequired());
			field.setNoIndex(fieldSchema.isNoIndex());
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
	default Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String newType = getType();

		switch (FieldTypes.valueByName(newType)) {
			case BOOLEAN:
				return Collections.singletonMap(getFieldName(), changeToBoolean(oldSchema, oldFields));
			case NUMBER:
				return Collections.singletonMap(getFieldName(), changeToNumber(oldSchema, oldFields));
			case DATE:
				return Collections.singletonMap(getFieldName(), changeToDate(oldSchema, oldFields));
			case HTML:
				return Collections.singletonMap(getFieldName(), changeToHtml(oldSchema, oldFields));
			case STRING:
				return Collections.singletonMap(getFieldName(), changeToString(oldSchema, oldFields));
			case BINARY:
				return Collections.singletonMap(getFieldName(), changeToBinary(oldSchema, oldFields));
			case S3BINARY:
				return Collections.singletonMap(getFieldName(), changeToS3Binary(oldSchema, oldFields));
			case LIST:
				return Collections.singletonMap(getFieldName(), changeToList(oldSchema, oldFields));
			case MICRONODE:
				return Collections.singletonMap(getFieldName(), changeToMicronode(oldSchema, oldFields));
			case NODE:
				return Collections.singletonMap(getFieldName(), changeToNode(oldSchema, oldFields));
			case JSON:
				return Collections.singletonMap(getFieldName(), changeToJson(oldSchema, oldFields));
			default:
				throw error(BAD_REQUEST, "Unknown type {" + newType + "} for change " + getUuid());
		}
	}

	private BooleanField changeToBoolean(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		Field oldField = oldFields.getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		return new BooleanFieldImpl().setValue(typeConverter.toBoolean(oldField.getValue()));
	}

	private NumberField changeToNumber(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		String oldType = fieldSchema.getType();
		Field oldField = oldFields.getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		Object oldValue = oldField.getValue();

		if (isUuidType(fieldSchema)) {
			return null;
		}

		switch (oldType) {
			case "number":
				return oldFields.getNumberField(fieldName);
			default:
				return new NumberFieldImpl().setNumber(typeConverter.toNumber(oldValue));
		}
	}

	private DateField changeToDate(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		Field oldField = oldFields.getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		return new DateFieldImpl().setDate(typeConverter.toDate(oldField.getValue()));
	}

	private JsonField changeToJson(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		Field oldField = oldFields.getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		return new JsonFieldImpl().setJson(typeConverter.toJsonObject(oldField.getValue()));
	}

	private HtmlField changeToHtml(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		if (isNonNodeUuidType(fieldSchema)) {
			return null;
		}
		Field field = oldFields.getField(fieldName, fieldSchema);
		if (field == null) {
			return null;
		}
		return new HtmlFieldImpl().setHTML(typeConverter.toString(field.getValue()));
	}

	private StringField changeToString(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		if (isNonNodeUuidType(fieldSchema)) {
			return null;
		}
		Field oldField = oldFields.getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		return new StringFieldImpl().setString(typeConverter.toString(oldField.getValue()));
	}

	private BinaryField changeToBinary(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		String oldType = fieldSchema.getType();
		switch (oldType) {
			case "binary":
				return oldFields.getBinaryField(fieldName);
			default:
				return null;
		}
	}

	private S3BinaryField changeToS3Binary(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		String oldType = fieldSchema.getType();
		switch (oldType) {
			case "s3binary":
				return oldFields.getS3BinaryField(fieldName);
			default:
				return null;
		}
	}

	@SuppressWarnings("rawtypes")
	private FieldList changeToList(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		String listType = getListType();
		Field oldField = oldFields.getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		Object oldValue = oldField.getValue();
		String oldType = fieldSchema.getType();

		switch (FieldTypes.valueByName(listType)) {
		case BOOLEAN:
			return typeConverter.toBooleanList(oldValue);
		case NUMBER:
			if (isUuidType(fieldSchema)) {
				return null;
			}
			switch (FieldTypes.valueByName(oldType)) {
			case NUMBER:
				return new NumberFieldListImpl().setItems(Collections.singletonList(oldFields.getNumberField(fieldName).getNumber()));
			default:
				return typeConverter.toNumberList(oldValue);
			}
		case DATE:
			return typeConverter.toDateList(oldValue);
		case HTML:
			if (isNonNodeUuidType(fieldSchema)) {
				return null;
			} else {
				return typeConverter.toHtmlList(oldValue);
			}
		case STRING:
			if (isNonNodeUuidType(fieldSchema)) {
				return null;
			} else {
				return typeConverter.toStringList(oldValue);
			}
		case JSON:
			if (isNonNodeUuidType(fieldSchema)) {
				return null;
			} else {
				return typeConverter.toJsonList(oldValue);
			}
		case MICRONODE:
			return typeConverter.toMicronodeList(oldField);
		case NODE:
			return typeConverter.toNodeList(oldField);
		default:
			throw error(BAD_REQUEST, "Unknown list type {" + listType + "} for change " + getUuid());
		}
	}

	private boolean isNonNodeUuidType(FieldSchema fieldSchema) {
		return isUuidType(fieldSchema) && !fieldSchema.getType().equals("node");
	}

	private boolean isUuidType(FieldSchema fieldSchema) {
		if (fieldSchema instanceof ListFieldSchema) {
			return UUID_TYPES.contains(fieldSchema.getType()) ||
				UUID_TYPES.contains(((ListFieldSchema) fieldSchema).getListType());
		} else {
			return UUID_TYPES.contains(fieldSchema.getType());
		}
	}

	@SuppressWarnings("unused")
	private <T> FieldList<T> nullableList(T[] input, Supplier<FieldList<T>> output) {
		if (input == null) {
			return null;
		} else {
			FieldList<T> list = output.get();
			list.setItems(Arrays.asList(input));
			return list;
		}
	}

	private MicronodeField changeToMicronode(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		return typeConverter.toMicronode(oldFields.getField(fieldName, fieldSchema));
	}

	private NodeField changeToNode(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		return typeConverter.toNode(oldFields.getField(fieldName, fieldSchema));
	}
}
