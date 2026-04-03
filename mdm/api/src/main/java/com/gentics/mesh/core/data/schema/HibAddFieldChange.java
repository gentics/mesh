package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ADD_FIELD_AFTER_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.ALLOW_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.LIST_TYPE_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.TYPE_KEY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.lang3.BooleanUtils;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.S3BinaryExtractOptions;
import com.gentics.mesh.core.rest.schema.S3BinaryFieldSchema;
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
import com.gentics.mesh.core.rest.schema.impl.S3BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

import io.vertx.core.json.JsonObject;

/**
 * Change entry which contains information for a field to be added to the schema.
 */
public interface HibAddFieldChange extends HibSchemaFieldChange {

	SchemaChangeOperation OPERATION = SchemaChangeOperation.ADDFIELD;

	/**
	 * Return the allow rest property value.
	 * @return
	 */
	default String[] getAllowProp() {
		Object[] prop = getRestProperty(ALLOW_KEY);
		if (prop == null) {
			return null;
		}
		return Stream.of(prop)
			.map(item -> (String) item)
			.toArray(String[]::new);
	}

	@Override
	default SchemaChangeOperation getOperation() {
		return OPERATION;
	}

	/**
	 * Set the type of the field that should be added.
	 * 
	 * @param type
	 * @return Fluent API
	 */
	default HibAddFieldChange setType(String type) {
		setRestProperty(TYPE_KEY, type);
		return this;
	}

	/**
	 * Returns the type of the field that should be added.
	 * 
	 * @return
	 */
	default String getType() {
		return getRestProperty(TYPE_KEY);
	}

	/**
	 * Return the list type field value.
	 * 
	 * @return The list type is null for non list fields
	 */
	default String getListType() {
		return getRestProperty(LIST_TYPE_KEY);
	}

	/**
	 * Set the list type field value.
	 * 
	 * @param type
	 */
	default void setListType(String type) {
		setRestProperty(LIST_TYPE_KEY, type);
	}

	/**
	 * Set the insert position. The position refers to an existing fieldname.
	 * 
	 * @param fieldName
	 */
	default void setInsertAfterPosition(String fieldName) {
		setRestProperty(ADD_FIELD_AFTER_KEY, fieldName);
	}

	/**
	 * Return the insert position.
	 * 
	 * @return
	 */
	default String getInsertAfterPosition() {
		return getRestProperty(ADD_FIELD_AFTER_KEY);
	}

	/**
	 * Return the field label.
	 * 
	 * @return
	 */
	default String getLabel() {
		return getRestProperty(SchemaChangeModel.LABEL_KEY);
	}

	/**
	 * Set the field label.
	 * 
	 * @param label
	 */
	default void setLabel(String label) {
		setRestProperty(SchemaChangeModel.LABEL_KEY, label);
	}

	/**
	 * Get the required flag
	 * 
	 * @return required flag
	 */
	default Boolean getRequired() {
		return getRestProperty(SchemaChangeModel.REQUIRED_KEY);
	}

	/**
	 * Get the 'exclude from indexing' flag
	 * 
	 * @return flag
	 */
	default Boolean getNoIndex() {
		return getRestProperty(SchemaChangeModel.NO_INDEX_KEY);
	}

	@Override
	default <R extends FieldSchemaContainer> R apply(R container) {

		String position = getInsertAfterPosition();
		FieldSchema field = null;
		// TODO avoid case switches like this. We need a central delegator implementation which will be used in multiple places
		switch (FieldTypes.valueByName(getType())) {
			case JSON:
				field = new JsonFieldSchemaImpl();
				break;
			case HTML:
				field = new HtmlFieldSchemaImpl();
				break;
			case STRING:
				StringFieldSchema stringField = new StringFieldSchemaImpl();
				stringField.setAllowedValues(getAllowProp());
				field = stringField;
				break;
			case NUMBER:
				field = new NumberFieldSchemaImpl();
				break;
			case BINARY:
				BinaryFieldSchema binaryField = new BinaryFieldSchemaImpl();
				Boolean content = getRestProperty(BinaryFieldSchemaImpl.CHANGE_EXTRACT_CONTENT_KEY);
				Boolean metadata = getRestProperty(BinaryFieldSchemaImpl.CHANGE_EXTRACT_METADATA_KEY);
				if (metadata != null || content != null) {
					BinaryExtractOptions options = new BinaryExtractOptions();
					options.setContent(BooleanUtils.toBoolean(content));
					options.setMetadata(BooleanUtils.toBoolean(metadata));
					binaryField.setBinaryExtractOptions(options);
				}
				field = binaryField;
				break;
			case S3BINARY:
				S3BinaryFieldSchema s3binaryField = new S3BinaryFieldSchemaImpl();
				Boolean s3Content = getRestProperty(S3BinaryFieldSchemaImpl.CHANGE_EXTRACT_CONTENT_KEY);
				Boolean s3Metadata = getRestProperty(S3BinaryFieldSchemaImpl.CHANGE_EXTRACT_METADATA_KEY);
				if (s3Metadata != null || s3Content != null) {
					S3BinaryExtractOptions options = new S3BinaryExtractOptions();
					options.setContent(BooleanUtils.toBoolean(s3Content));
					options.setMetadata(BooleanUtils.toBoolean(s3Metadata));
					s3binaryField.setS3BinaryExtractOptions(options);
				}
				field = s3binaryField;
				break;
			case NODE:
				NodeFieldSchema nodeField = new NodeFieldSchemaImpl();
				nodeField.setAllowedSchemas(getAllowProp());
				field = nodeField;
				break;
			case MICRONODE:
				MicronodeFieldSchema micronodeFieldSchema = new MicronodeFieldSchemaImpl();
				micronodeFieldSchema.setAllowedMicroSchemas(getAllowProp());
				field = micronodeFieldSchema;
				break;
			case DATE:
				field = new DateFieldSchemaImpl();
				break;
			case BOOLEAN:
				field = new BooleanFieldSchemaImpl();
				break;
			case LIST:
				ListFieldSchema listField = new ListFieldSchemaImpl();
				listField.setListType(getListType());
				field = listField;
				switch (FieldTypes.valueByName(getListType())) {
					case NODE:
					case MICRONODE:
					case JSON:
						listField.setAllowedSchemas(getAllowProp());
						break;
					default:
						break;
				}
				break;
			default:
				throw error(BAD_REQUEST, "Unknown type");
		}
		setCommonFieldProperties(field);
		container.addField(field, position);
		return container;
	}

	private void setCommonFieldProperties(FieldSchema field) {
		field.setName(getFieldName());
		field.setLabel(getLabel());
		Boolean required = getRequired();
		if (required != null) {
			field.setRequired(required);
		}
		Boolean noIndex = getNoIndex();
		if (noIndex != null) {
			field.setNoIndex(noIndex);
		}
		JsonObject elasticSearch = getIndexOptions();
		if (elasticSearch != null) {
			field.setElasticsearch(elasticSearch);
		}
	}

	@Override
	default Map<String, Field> createFields(FieldSchemaContainer oldSchema, FieldMap oldFields) {
		return Collections.singletonMap(getFieldName(), null);
	}
}

