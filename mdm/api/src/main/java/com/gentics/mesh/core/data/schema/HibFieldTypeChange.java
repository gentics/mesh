package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.gentics.mesh.core.data.node.handler.TypeConverter;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.node.field.*;
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
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.*;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
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
				case "binary":
					field = new BinaryFieldSchemaImpl();
					break;
				case "s3binary":
					field = new S3BinaryFieldSchemaImpl();
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
	default Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
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
			case "s3binary":
				return Collections.singletonMap(getFieldName(), changeToS3Binary(oldSchema, oldContent));
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

		Field oldField = oldContent.getFields().getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		return new BooleanFieldImpl().setValue(typeConverter.toBoolean(oldField.getValue()));
	}

	private NumberField changeToNumber(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		String oldType = fieldSchema.getType();
		Field oldField = oldContent.getFields().getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		Object oldValue = oldField.getValue();

		if (isUuidType(fieldSchema)) {
			return null;
		}

		switch (oldType) {
			case "number":
				return oldContent.getFields().getNumberField(fieldName);
			default:
				return new NumberFieldImpl().setNumber(typeConverter.toNumber(oldValue));
		}
	}

	private DateField changeToDate(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		Field oldField = oldContent.getFields().getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		return new DateFieldImpl().setDate(typeConverter.toDate(oldField.getValue()));
	}

	private HtmlField changeToHtml(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		if (isNonNodeUuidType(fieldSchema)) {
			return null;
		}
		Field field = oldContent.getFields().getField(fieldName, fieldSchema);
		if (field == null) {
			return null;
		}
		return new HtmlFieldImpl().setHTML(typeConverter.toString(field.getValue()));
	}

	private StringField changeToString(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		if (isNonNodeUuidType(fieldSchema)) {
			return null;
		}
		Field oldField = oldContent.getFields().getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		return new StringFieldImpl().setString(typeConverter.toString(oldField.getValue()));
	}

	private BinaryField changeToBinary(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		String oldType = fieldSchema.getType();
		switch (oldType) {
			case "binary":
				return oldContent.getFields().getBinaryField(fieldName);
			default:
				return null;
		}
	}

	private S3BinaryField changeToS3Binary(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		String oldType = fieldSchema.getType();
		switch (oldType) {
			case "s3binary":
				return oldContent.getFields().getS3BinaryField(fieldName);
			default:
				return null;
		}
	}

	@SuppressWarnings("rawtypes")
	private FieldList changeToList(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);
		String listType = getListType();
		Field oldField = oldContent.getFields().getField(fieldName, fieldSchema);
		if (oldField == null) {
			return null;
		}
		Object oldValue = oldField.getValue();
		String oldType = fieldSchema.getType();

		switch (listType) {
		case "boolean":
			return typeConverter.toBooleanList(oldValue);
		case "number":
			if (isUuidType(fieldSchema)) {
				return null;
			}
			switch (oldType) {
			case "number":
				return new NumberFieldListImpl().setItems(Collections.singletonList(oldContent.getFields().getNumberField(fieldName).getNumber()));
			default:
				return typeConverter.toNumberList(oldValue);
			}
		case "date":
			return typeConverter.toDateList(oldValue);
		case "html":
			if (isNonNodeUuidType(fieldSchema)) {
				return null;
			} else {
				return typeConverter.toHtmlList(oldValue);
			}
		case "string":
			if (isNonNodeUuidType(fieldSchema)) {
				return null;
			} else {
				return typeConverter.toStringList(oldValue);
			}
		case "micronode":
			return typeConverter.toMicronodeList(oldField);
		case "node":
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

	private <T> FieldList<T> nullableList(T[] input, Supplier<FieldList<T>> output) {
		if (input == null) {
			return null;
		} else {
			FieldList<T> list = output.get();
			list.setItems(Arrays.asList(input));
			return list;
		}
	}

	private MicronodeField changeToMicronode(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		return typeConverter.toMicronode(oldContent.getFields().getField(fieldName, fieldSchema));
	}

	private NodeField changeToNode(FieldSchemaContainer oldSchema, FieldContainer oldContent) {
		String fieldName = getFieldName();
		FieldSchema fieldSchema = oldSchema.getField(fieldName);

		return typeConverter.toNode(oldContent.getFields().getField(fieldName, fieldSchema));
	}
}
