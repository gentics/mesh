package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.DummyBulkActionContext;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryField;
import com.gentics.mesh.core.data.node.field.BooleanField;
import com.gentics.mesh.core.data.node.field.DateField;
import com.gentics.mesh.core.data.node.field.HtmlField;
import com.gentics.mesh.core.data.node.field.NumberField;
import com.gentics.mesh.core.data.node.field.StringField;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.s3binary.S3Binary;
import com.gentics.mesh.core.data.s3binary.S3BinaryField;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.common.ReferenceType;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;

public interface FieldContainer extends BasicFieldContainer {

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
	FieldModel getRestField(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, List<String> languageTags, int level);

	/**
	 * Return the field for the given field schema.
	 * 
	 * @param fieldSchema
	 * @return
	 */
	Field getField(FieldSchema fieldSchema);

	/**
	 * Get the type of this field container, that is used in the referencing content.
	 * 
	 * @return
	 */
	ReferenceType getReferenceType();

	/**
	 * List the fields of this container for the current schema container version
	 * 
	 * @return
	 */
	default List<Field> getFields() {
		return getFields(getSchemaContainerVersion());
	}

	/**
	 * List the fields of this container for the provided schema container version
	 * During a migraiton, the container might temporary have fields for an older schema version
	 * @param schemaContainerVersion
	 * @return
	 */
	default List<Field> getFields(FieldSchemaVersionElement<?, ?, ?, ?, ?> schemaContainerVersion) {
		FieldSchemaContainer schema = schemaContainerVersion.getSchema();
		List<Field> fields = new ArrayList<>();
		for (FieldSchema fieldSchema : schema.getFields()) {
			Field field = getField(fieldSchema);
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
	default Field getField(String fieldKey) {
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
	default void removeField(Field field) {
		if (field != null) {
			removeField(field.getFieldKey());
		}
	}

	/**
	 * Get the schema container version used by this container
	 * 
	 * @return schema container version
	 */
	FieldSchemaVersionElement<?, ?, ?, ?, ?> getSchemaContainerVersion();

	/**
	 * Set the schema container version used by this container
	 * 
	 * @param version
	 *            schema container version
	 */
	void setSchemaContainerVersion(FieldSchemaVersionElement<?, ?, ?, ?, ?> version);

	/**
	 * Get all nodes that are in any way referenced by this node. This includes the following cases:
	 * <ul>
	 * <li>Node fields</li>
	 * <li>Node list fields</li>
	 * <li>Micronode fields with node fields or node list fields</li>
	 * <li>Micronode list fields with node fields or node list fields</li>
	 * </ul>
	 */
	default Iterable<? extends Node> getReferencedNodes() {
		// Get all fields and group them by type
		Map<String, List<FieldSchema>> affectedFields = getSchemaContainerVersion().getSchema().getFields().stream()
			.filter(this::isNodeReferenceType)
			.collect(Collectors.groupingBy(FieldSchema::getType));

		Function<FieldTypes, List<FieldSchema>> getFields = type -> Optional.ofNullable(affectedFields.get(type.toString()))
			.orElse(Collections.emptyList());

		Stream<Stream<Node>> nodeStream = Stream.of(
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
		Map<String, Field> fieldsMap = getFields().stream().collect(Collectors.toMap(Field::getFieldKey, Function.identity()));

		schema.getFields().stream().forEach(fieldSchema -> {
			Field field = fieldsMap.get(fieldSchema.getName());
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
	 * Use the given map of rest fields to set the data from the map to this container.
	 * @param ac
	 * @param restFields
	 */
	default void createFieldsFromRest(InternalActionContext ac, FieldMap restFields) {
		updateFieldsFromRest(ac, restFields);
	}

	/**
	 * Gets the HibNodeFieldContainers connected to this FieldContainer. 
	 * For HibNodeFieldContainers this is simply the same object. 
	 * For Micronodes this will return all contents that use this micronode.
	 * 
	 * @return
	 */
	default Stream<? extends NodeFieldContainer> getContents() {
		return getContents(true, true);
	}

	/**
	 * Gets the HibNodeFieldContainers connected to this FieldContainer. 
	 * For HibNodeFieldContainers this is simply the same object. 
	 * For Micronodes this will return all contents that use this micronode.
	 * 
	 * @return
	 */
	Stream<? extends NodeFieldContainer> getContents(boolean lookupInFields, boolean lookupInLists);

	/**
	 * 
	 * Return the string field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	StringField getString(String key);

	/**
	 * Return the binary  field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	BinaryField getBinary(String key);

	/**
	 * Return the binary file name for the given key.
	 *
	 * @param key
	 * @return
	 */
	String getBinaryFileName(String key);

	/**
	 * Return the s3 binary  field for the given key.
	 *
	 * @param key
	 * @return
	 */
	S3BinaryField getS3Binary(String key);

	/**
	 * Return the s3 binary file name for the given key.
	 *
	 * @param key
	 * @return
	 */
	String getS3BinaryFileName(String key);

	/**
	 * Return the node field for the given key.
	 * 
	 * @param key
	 * @return
	 */
	NodeField getNode(String key);

	/**
	 * Return the date field.
	 * 
	 * @param key
	 * @return
	 */
	DateField getDate(String key);

	/**
	 * Return the number field.
	 * 
	 * @param key
	 * @return
	 */
	NumberField getNumber(String key);

	/**
	 * Return the html field.
	 * 
	 * @param key
	 * @return
	 */
	HtmlField getHtml(String key);

	/**
	 * Return the boolean field.
	 * 
	 * @param key
	 * @return
	 */
	BooleanField getBoolean(String key);

	/**
	 * Return the micronode field.
	 * 
	 * @param key
	 * @return
	 */
	MicronodeField getMicronode(String key);

	/**
	 * Return the date list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	DateFieldList getDateList(String fieldKey);

	/**
	 * Return html list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HtmlFieldList getHTMLList(String fieldKey);

	/**
	 * Return number list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NumberFieldList getNumberList(String fieldKey);

	/**
	 * Return node list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NodeFieldList getNodeList(String fieldKey);

	/**
	 * Return string list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	StringFieldList getStringList(String fieldKey);

	/**
	 * Return boolean list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	BooleanFieldList getBooleanList(String fieldKey);

	/**
	 * Return node list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	MicronodeFieldList getMicronodeList(String fieldKey);

	/**
	 * Create a new micronode list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	MicronodeFieldList createMicronodeList(String fieldKey);

	/**
	 * Create a new boolean list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	BooleanFieldList createBooleanList(String fieldKey);

	/**
	 * Create a new string list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	StringFieldList createStringList(String fieldKey);

	/**
	 * Create a new node list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NodeFieldList createNodeList(String fieldKey);

	/**
	 * Create a new number list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	NumberFieldList createNumberList(String fieldKey);

	/**
	 * Create a new html list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	HtmlFieldList createHTMLList(String fieldKey);

	/**
	 * Create a new date list.
	 * 
	 * @param fieldKey
	 * @return
	 */
	DateFieldList createDateList(String fieldKey);

	/**
	 * Create a new micronode field. This method ensures that only one micronode exists per key.
	 * 
	 * @param key
	 * @param microschemaVersion
	 * @return
	 */
	MicronodeField createMicronode(String key, MicroschemaVersion microschemaVersion);

	/**
	 * Create a new micronode field for given microschemaVersion. This method assumes there are no previous
	 * micronodes to clone
	 *
	 * @param microschemaVersion
	 * @return
	 */
	MicronodeField createEmptyMicronode(String key, MicroschemaVersion microschemaVersion);

	/**
	 * Create a new boolean field.
	 * 
	 * @param key
	 * @return
	 */
	BooleanField createBoolean(String key);

	/**
	 * Create a new html field.
	 * 
	 * @param key
	 * @return
	 */
	HtmlField createHTML(String key);

	/**
	 * Create the number field.
	 * 
	 * @param key
	 * @return
	 */
	NumberField createNumber(String key);

	/**
	 * Create a new date field.
	 * 
	 * @param key
	 * @return
	 */
	DateField createDate(String key);

	/**
	 * Create a new node field.
	 * 
	 * @param key
	 *            Key of the field
	 * @param node
	 *            Node to be referenced.
	 * @return
	 */
	NodeField createNode(String key, Node node);

	/**
	 * Create an s3 binary field and use the given binary to be referenced by the field.
	 *
	 * @param fieldKey
	 * @param binary
	 * @return
	 */
	S3BinaryField createS3Binary(String fieldKey, S3Binary binary);

	/**
	 * Create a binary field and use the given binary to be referenced by the field.
	 * 
	 * @param fieldKey
	 * @param binary
	 * @return
	 */
	BinaryField createBinary(String fieldKey, Binary binary);

	/**
	 * Create a new string field.
	 * 
	 * @param key
	 * @return
	 */
	StringField createString(String key);

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
	private Stream<Node> getNodeFromNodeField(FieldSchema field) {
		return Optional.ofNullable(getNode(field.getName()))
			.map(NodeField::getNode)
			.filter(Objects::nonNull)
			.map(Stream::of)
			.orElseGet(Stream::empty);
	}

	/**
	 * Gets the nodes that are referenced by a micronode in the given field. This includes all node fields and node list fields in the micronode.
	 */
	private Stream<? extends Node> getNodesFromMicronode(FieldSchema field) {
		return Optional.ofNullable(getMicronode(field.getName()))
			.map(micronode -> StreamSupport.stream(micronode.getMicronode().getReferencedNodes().spliterator(), false))
			.orElseGet(Stream::empty);
	}

	/**
	 * Gets the nodes that are referenced by a list field. In case of a node list, all nodes in that list are returned. In case of a micronode list, all nodes
	 * referenced by all node fields and node list fields in all microschemas are returned. Otherwise an empty stream is returned.
	 */
	private Stream<? extends Node> getNodesFromList(FieldSchema field) {
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
				.map(NodeField::getNode)
				.filter(Objects::nonNull);
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
