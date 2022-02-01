package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.list.HibBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.HibDateFieldList;
import com.gentics.mesh.core.data.node.field.list.HibHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.HibNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.HibStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.data.s3binary.S3HibBinaryField;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;

public interface HibFieldContainer extends HibBasicFieldContainer {

	/**
	 * Locate the field with the given fieldkey in this container and return the rest model for this field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param fieldSchema
	 * @param languageTags
	 *            language tags
	 * @param level
	 *            Current level of transformation
	 */
	Field getRestField(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, List<String> languageTags, int level);

	/**
	 * Return the field for the given field schema.
	 * 
	 * @param fieldSchema
	 * @return
	 */
	HibField getField(FieldSchema fieldSchema);

	/**
	 * Get the type of this field container, that is used in the referencing content.
	 * 
	 * @return
	 */
	ReferenceType getReferenceType();
	/**
	 * List the fields of this container
	 * 
	 * @return
	 */
	default List<HibField> getFields() {
		FieldSchemaContainer schema = getSchemaContainerVersion().getSchema();
		List<HibField> fields = new ArrayList<>();
		for (FieldSchema fieldSchema : schema.getFields()) {
			HibField field = getField(fieldSchema);
			if (field != null) {
				fields.add(field);
			}
		}
		return fields;
	}

	/**
	 * Get a schema of the given field key. Returns null, if no key exists.
	 * 
	 * @param fieldKey
	 * @return
	 */
	default FieldSchema getFieldSchema(String fieldKey) {
		return getSchemaContainerVersion().getSchema().getField(fieldKey);
	}
	/**
	 * Get the field of the given field key. Returns null, if no key exists.
	 * 
	 * @param fieldKey
	 * @return
	 */
	default HibField getField(String fieldKey) {
		FieldSchema schema = getFieldSchema(fieldKey);
		if (schema != null) {
			return getField(schema);
		} else {
			return null;
		}
	}

	/**
	 * Remove the field from container.
	 * 
	 * @param fieldKey
	 */
	void removeField(String fieldKey, BulkActionContext bac);

	/**
	 * Remove the field with the given key and use a dummy bulk action context.
	 * 
	 * @param fieldKey
	 */
	default void removeField(String fieldKey) {
		removeField(fieldKey, new DummyBulkActionContext());
	}

	/**
	 * Remove the field.
	 * 
	 * @param fieldKey
	 */
	default void removeField(HibField field) {
		if (field != null) {
			removeField(field.getFieldKey());
		}
	}

	/**
	 * Get the schema container version used by this container
	 * 
	 * @return schema container version
	 */
	HibFieldSchemaVersionElement<?, ?, ?, ?, ?> getSchemaContainerVersion();

	/**
	 * Set the schema container version used by this container
	 * 
	 * @param version
	 *            schema container version
	 */
	void setSchemaContainerVersion(HibFieldSchemaVersionElement<?, ?, ?, ?, ?> version);

	/**
	 * Get all nodes that are in any way referenced by this node. This includes the following cases:
	 * <ul>
	 * <li>Node fields</li>
	 * <li>Node list fields</li>
	 * <li>Micronode fields with node fields or node list fields</li>
	 * <li>Micronode list fields with node fields or node list fields</li>
	 * </ul>
	 */
	default Iterable<? extends HibNode> getReferencedNodes() {
		// Get all fields and group them by type
		Map<String, List<FieldSchema>> affectedFields = getSchemaContainerVersion().getSchema().getFields().stream()
			.filter(this::isNodeReferenceType)
			.collect(Collectors.groupingBy(FieldSchema::getType));

		Function<FieldTypes, List<FieldSchema>> getFields = type -> Optional.ofNullable(affectedFields.get(type.toString()))
			.orElse(Collections.emptyList());

		Stream<Stream<HibNode>> nodeStream = Stream.of(
			getFields.apply(FieldTypes.NODE).stream().flatMap(this::getNodeFromNodeField),
			getFields.apply(FieldTypes.MICRONODE).stream().flatMap(this::getNodesFromMicronode),
			getFields.apply(FieldTypes.LIST).stream().flatMap(this::getNodesFromList)
		);
		return nodeStream.flatMap(Function.identity())::iterator;
	}

	/**
	 * Validate consistency of this container. This will check whether all mandatory fields have been filled
	 */
	default void validate() {
		FieldSchemaContainerVersion schema = getSchemaContainerVersion().getSchema();
		Map<String, HibField> fieldsMap = getFields().stream().collect(Collectors.toMap(HibField::getFieldKey, Function.identity()));

		schema.getFields().stream().forEach(fieldSchema -> {
			HibField field = fieldsMap.get(fieldSchema.getName());
			if (fieldSchema.isRequired() && field == null) {
				throw error(CONFLICT, "node_error_missing_mandatory_field_value", fieldSchema.getName(), schema.getName());
			}
			if (field != null) {
				field.validate();
			}
		});
	}

	/**
	 * Use the given map of rest fields to set the data from the map to this container.
	 * 
	 * @param ac
	 * @param restFields
	 */
	void updateFieldsFromRest(InternalActionContext ac, FieldMap restFields);

	/**
	 * Gets the HibNodeFieldContainers connected to this FieldContainer. 
	 * For HibNodeFieldContainers this is simply the same object. 
	 * For Micronodes this will return all contents that use this micronode.
	 * 
	 * @return
	 */
	Stream<? extends HibNodeFieldContainer> getContents();

	/**
	 * 
	 * Return the string field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	HibStringField getString(String key);

	/**
	 * Return the binary  field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	HibBinaryField getBinary(String key);

	/**
	 * Return the s3 binary  field for the given key.
	 *
	 * @param key
	 * @return
	 */
	S3HibBinaryField getS3Binary(String key);

	/**
	 * Return the node field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	HibNodeField getNode(String key);

	/**
	 * Return the date field.
	 * 
	 * @param key
	 * @return
	 */
	HibDateField getDate(String key);

	/**
	 * Return the number field.
	 * 
	 * @param key
	 * @return
	 */
	HibNumberField getNumber(String key);

	/**
	 * Return the html field.
	 * 
	 * @param key
	 * @return
	 */
	HibHtmlField getHtml(String key);

	/**
	 * Return the boolean field.
	 * 
	 * @param key
	 * @return
	 */
	HibBooleanField getBoolean(String key);

	/**
	 * Return the micronode field.
	 * 
	 * @param key
	 * @return
	 */
	HibMicronodeField getMicronode(String key);

	/**
	 * Return the date list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibDateFieldList getDateList(String fieldKey);

	/**
	 * Return html list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibHtmlFieldList getHTMLList(String fieldKey);

	/**
	 * Return number list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibNumberFieldList getNumberList(String fieldKey);

	/**
	 * Return node list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibNodeFieldList getNodeList(String fieldKey);

	/**
	 * Return string list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibStringFieldList getStringList(String fieldKey);

	/**
	 * Return boolean list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibBooleanFieldList getBooleanList(String fieldKey);

	/**
	 * Return node list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibMicronodeFieldList getMicronodeList(String fieldKey);

	/**
	 * Create a new micronode list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibMicronodeFieldList createMicronodeList(String fieldKey);

	/**
	 * Create a new boolean list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibBooleanFieldList createBooleanList(String fieldKey);

	/**
	 * Create a new string list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibStringFieldList createStringList(String fieldKey);

	/**
	 * Create a new node list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibNodeFieldList createNodeList(String fieldKey);

	/**
	 * Create a new number list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibNumberFieldList createNumberList(String fieldKey);

	/**
	 * Create a new html list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibHtmlFieldList createHTMLList(String fieldKey);

	/**
	 * Create a new date list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HibDateFieldList createDateList(String fieldKey);

	/**
	 * Create a new micronode field. This method ensures that only one micronode exists per key.
	 * 
	 * @param key
	 * @param microschemaVersion
	 * @return
	 */
	HibMicronodeField createMicronode(String key, HibMicroschemaVersion microschemaVersion);

	/**
	 * Create a new boolean field.
	 * 
	 * @param key
	 * @return
	 */
	HibBooleanField createBoolean(String key);

	/**
	 * Create a new html field.
	 * 
	 * @param key
	 * @return
	 */
	HibHtmlField createHTML(String key);

	/**
	 * Create the number field.
	 * 
	 * @param key
	 * @return
	 */
	HibNumberField createNumber(String key);

	/**
	 * Create a new date field.
	 * 
	 * @param key
	 * @return
	 */
	HibDateField createDate(String key);

	/**
	 * Create a new node field.
	 * 
	 * @param key
	 *            Key of the field
	 * @param node
	 *            Node to be referenced.
	 * @return
	 */
	HibNodeField createNode(String key, HibNode node);

	/**
	 * Create an s3 binary field and use the given binary to be referenced by the field.
	 *
	 * @param fieldKey
	 * @param binary
	 * @return
	 */
	S3HibBinaryField createS3Binary(String fieldKey, S3HibBinary binary);

	/**
	 * Create a binary field and use the given binary to be referenced by the field.
	 * 
	 * @param fieldKey
	 * @param binary
	 * @return
	 */
	HibBinaryField createBinary(String fieldKey, HibBinary binary);

	/**
	 * Create a new string field.
	 * 
	 * @param key
	 * @return
	 */
	HibStringField createString(String key);

	/**
	 * Checks if a field can have a node reference.
	 */
	private boolean isNodeReferenceType(FieldSchema schema) {
		String type = schema.getType();
		return type.equals(FieldTypes.NODE.toString()) || type.equals(FieldTypes.LIST.toString()) || type.equals(FieldTypes.MICRONODE.toString());
	}

	/**
	 * Gets the node from a node field.
	 * 
	 * @param field
	 *            The node field to get the node from
	 * @return Gets the node as a stream or an empty stream if the node field is not set
	 */
	private Stream<HibNode> getNodeFromNodeField(FieldSchema field) {
		return Optional.ofNullable(getNode(field.getName()))
			.map(HibNodeField::getNode)
			.map(Stream::of)
			.orElseGet(Stream::empty);
	}

	/**
	 * Gets the nodes that are referenced by a micronode in the given field. This includes all node fields and node list fields in the micronode.
	 */
	private Stream<? extends HibNode> getNodesFromMicronode(FieldSchema field) {
		return Optional.ofNullable(getMicronode(field.getName()))
			.map(micronode -> StreamSupport.stream(micronode.getMicronode().getReferencedNodes().spliterator(), false))
			.orElseGet(Stream::empty);
	}

	/**
	 * Gets the nodes that are referenced by a list field. In case of a node list, all nodes in that list are returned. In case of a micronode list, all nodes
	 * referenced by all node fields and node list fields in all microschemas are returned. Otherwise an empty stream is returned.
	 */
	private Stream<? extends HibNode> getNodesFromList(FieldSchema field) {
		ListFieldSchema list;
		if (field instanceof ListFieldSchema) {
			list = (ListFieldSchema) field;
		} else {
			throw new InvalidParameterException("Invalid field type");
		}

		String type = list.getListType();
		if (type.equals(FieldTypes.NODE.toString())) {
			return Optional.ofNullable(getNodeList(list.getName()))
				.map(listField -> listField.getList().stream())
				.orElseGet(Stream::empty)
				.map(HibNodeField::getNode);
		} else if (type.equals(FieldTypes.MICRONODE.toString())) {
			return Optional.ofNullable(getMicronodeList(list.getName()))
				.map(listField -> listField.getList().stream())
				.orElseGet(Stream::empty)
				.flatMap(micronode -> StreamSupport.stream(micronode.getMicronode().getReferencedNodes().spliterator(), false));
		} else {
			return Stream.empty();
		}
	}
}
