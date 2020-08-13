package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

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
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.binary.Binary;
import com.gentics.mesh.core.data.impl.GraphFieldTypes;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.HtmlGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.syncleus.ferma.traversals.EdgeTraversal;

/**
 * Abstract implementation for a field container. A {@link GraphFieldContainer} is used to store {@link GraphField} instances.
 */
public abstract class AbstractGraphFieldContainerImpl extends AbstractBasicGraphFieldContainerImpl implements GraphFieldContainer {

	/**
	 * Return the parent node of the field container.
	 * 
	 * @return
	 */
	abstract protected Node getParentNode();

	@Override
	public StringGraphField createString(String key) {
		// TODO check whether the key is already occupied
		StringGraphFieldImpl field = new StringGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public StringGraphField getString(String key) {
		if (fieldExists(key, "string")) {
			return new StringGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NodeGraphField createNode(String key, Node node) {
		NodeGraphFieldImpl field = getGraph().addFramedEdge(this, node, HAS_FIELD, NodeGraphFieldImpl.class);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public NodeGraphField getNode(String key) {
		return outE(HAS_FIELD).has(GraphField.FIELD_KEY_PROPERTY_KEY, key).nextOrDefaultExplicit(NodeGraphFieldImpl.class, null);
	}

	@Override
	public DateGraphField createDate(String key) {
		DateGraphFieldImpl field = new DateGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public DateGraphField getDate(String key) {
		if (fieldExists(key, "date")) {
			return new DateGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public NumberGraphField createNumber(String key) {
		NumberGraphFieldImpl field = new NumberGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public NumberGraphField getNumber(String key) {
		if (fieldExists(key, "number")) {
			return new NumberGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public HtmlGraphField createHTML(String key) {
		HtmlGraphFieldImpl field = new HtmlGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public HtmlGraphField getHtml(String key) {
		if (fieldExists(key, "html")) {
			return new HtmlGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public BooleanGraphField createBoolean(String key) {
		BooleanGraphFieldImpl field = new BooleanGraphFieldImpl(key, this);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public BooleanGraphField getBoolean(String key) {
		if (fieldExists(key, "boolean")) {
			return new BooleanGraphFieldImpl(key, this);
		}
		return null;
	}

	@Override
	public MicronodeGraphField createMicronode(String key, MicroschemaVersion microschema) {
		// 1. Copy existing micronode
		MicronodeGraphField existing = getMicronode(key);
		Micronode existingMicronode = null;
		if (existing != null) {
			existingMicronode = existing.getMicronode();
			// existing.getMicronode().delete();
		}

		// 2. Create a new micronode and assign the given schema to it
		MicronodeImpl micronode = getGraph().addFramedVertex(MicronodeImpl.class);
		micronode.setSchemaContainerVersion(microschema);
		if (existingMicronode != null) {
			micronode.clone(existingMicronode);

			// Remove the old field (edge)
			existing.remove();

			// If the existing micronode was only used by this container, remove it
			if (!existingMicronode.in(HAS_FIELD).hasNext()) {
				existingMicronode.remove();
			}
		}
		// 3. Create a new edge from the container to the created micronode field
		MicronodeGraphField field = getGraph().addFramedEdge(this, micronode, HAS_FIELD, MicronodeGraphFieldImpl.class);
		field.setFieldKey(key);
		return field;
	}

	@Override
	public MicronodeGraphField getMicronode(String key) {
		return outE(HAS_FIELD).has(GraphField.FIELD_KEY_PROPERTY_KEY, key).nextOrDefaultExplicit(MicronodeGraphFieldImpl.class, null);
	}

	@Override
	public BinaryGraphField createBinary(String fieldKey, Binary binary) {
		BinaryGraphField edge = addFramedEdge(HAS_FIELD, binary, BinaryGraphFieldImpl.class);
		edge.setFieldKey(fieldKey);
		return edge;
	}

	@Override
	public BinaryGraphField getBinary(String key) {
		return outE(HAS_FIELD).has(GraphField.FIELD_KEY_PROPERTY_KEY, key).nextOrDefaultExplicit(BinaryGraphFieldImpl.class, null);
	}

	@Override
	public NumberGraphFieldList createNumberList(String fieldKey) {
		return createList(NumberGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public NumberGraphFieldList getNumberList(String fieldKey) {
		return getList(NumberGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public NodeGraphFieldList createNodeList(String fieldKey) {
		return createList(NodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public NodeGraphFieldList getNodeList(String fieldKey) {
		return getList(NodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public StringGraphFieldList createStringList(String fieldKey) {
		return createList(StringGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public StringGraphFieldList getStringList(String fieldKey) {
		return getList(StringGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public BooleanGraphFieldList createBooleanList(String fieldKey) {
		return createList(BooleanGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public BooleanGraphFieldList getBooleanList(String fieldKey) {
		return getList(BooleanGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public MicronodeGraphFieldList createMicronodeFieldList(String fieldKey) {
		return createList(MicronodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public MicronodeGraphFieldList getMicronodeList(String fieldKey) {
		return getList(MicronodeGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public HtmlGraphFieldList createHTMLList(String fieldKey) {
		return createList(HtmlGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public HtmlGraphFieldList getHTMLList(String fieldKey) {
		return getList(HtmlGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public DateGraphFieldList createDateList(String fieldKey) {
		return createList(DateGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public DateGraphFieldList getDateList(String fieldKey) {
		return getList(DateGraphFieldListImpl.class, fieldKey);
	}

	@Override
	public <T extends ListGraphField<?, ?, ?>> T getList(Class<T> classOfT, String fieldKey) {
		return out(HAS_LIST).has(classOfT).has(GraphField.FIELD_KEY_PROPERTY_KEY, fieldKey).nextOrDefaultExplicit(classOfT, null);
	}

	/**
	 * Create new list of the given type. If the container already has a list of given type, it will be "unattached" and removed, if this container was the only
	 * parent
	 * 
	 * @param classOfT
	 *            Implementation/Type of list
	 * @param fieldKey
	 *            Field key for the list
	 * @return
	 */
	private <T extends ListGraphField<?, ?, ?>> T createList(Class<T> classOfT, String fieldKey) {
		T existing = getList(classOfT, fieldKey);
		T list = getGraph().addFramedVertex(classOfT);
		list.setFieldKey(fieldKey);
		linkOut(list, HAS_LIST);

		if (existing != null) {
			unlinkOut(existing, HAS_LIST);
			if (existing.in(HAS_LIST).count() == 0) {
				existing.remove();
			}
		}

		return list;
	}

	@Override
	public Field getRestFieldFromGraph(InternalActionContext ac, String fieldKey, FieldSchema fieldSchema, List<String> languageTags, int level) {
		GraphFieldTypes type = GraphFieldTypes.valueByFieldSchema(fieldSchema);
		if (type != null) {
			return type.getRestFieldFromGraph(this, ac, fieldKey, fieldSchema, languageTags, level, () -> getParentNode());
		} else {
			throw error(BAD_REQUEST, "type unknown");
		}
	}

	/**
	 * Update or create the field using the given restField. The {@link FieldSchema} is used to determine the type of the field.
	 * 
	 * @param ac
	 *            Action context
	 * @param fieldMap
	 * @param fieldKey
	 *            Key of the field
	 * @param fieldSchema
	 *            Field schema of the field
	 * @param schema
	 *            Schema of the field
	 */
	protected void updateField(InternalActionContext ac, FieldMap fieldMap, String fieldKey, FieldSchema fieldSchema, FieldSchemaContainer schema) {
		GraphFieldTypes type = GraphFieldTypes.valueByFieldSchema(fieldSchema);
		if (type != null) {
			type.updateField(this, ac, fieldMap, fieldKey, fieldSchema, schema);
		} else {
			throw error(BAD_REQUEST, "type unknown");
		}
	}

	@Override
	public void updateFieldsFromRest(InternalActionContext ac, FieldMap fieldMap) {
		FieldSchemaContainer schema = getSchemaContainerVersion().getSchema();
		schema.assertForUnhandledFields(fieldMap);

		// TODO: This should return an observable
		// Iterate over all known field that are listed in the schema for the node
		for (FieldSchema entry : schema.getFields()) {
			String key = entry.getName();
			updateField(ac, fieldMap, key, entry, schema);
		}
	}

	@Override
	public GraphField getField(FieldSchema fieldSchema) {
		GraphFieldTypes type = GraphFieldTypes.valueByFieldSchema(fieldSchema);
		if (type != null) {
			return type.getField(this, fieldSchema);
		} else {
			throw new GenericRestException(INTERNAL_SERVER_ERROR, "Unknown list type {" + fieldSchema.getType() + "}");
		}
	}

	@Override
	public List<GraphField> getFields() {
		FieldSchemaContainer schema = getSchemaContainerVersion().getSchema();
		List<GraphField> fields = new ArrayList<>();
		for (FieldSchema fieldSchema : schema.getFields()) {
			GraphField field = getField(fieldSchema);
			if (field != null) {
				fields.add(field);
			}
		}
		return fields;
	}

	@Override
	public void deleteFieldEdge(String key) {
		EdgeTraversal<?, ?, ?> traversal = outE(HAS_FIELD).has(GraphField.FIELD_KEY_PROPERTY_KEY, key);
		if (traversal.hasNext()) {
			traversal.next().remove();
		}
	}

	@Override
	public Iterable<? extends Node> getReferencedNodes() {
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
			.map(NodeGraphField::getNode)
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
				.map(NodeGraphField::getNode);
		} else if (type.equals(FieldTypes.MICRONODE.toString())) {
			return Optional.ofNullable(getMicronodeList(list.getName()))
				.map(listField -> listField.getList().stream())
				.orElseGet(Stream::empty)
				.flatMap(micronode -> StreamSupport.stream(micronode.getMicronode().getReferencedNodes().spliterator(), false));
		} else {
			return Stream.empty();
		}
	}

	@Override
	public void delete(BulkActionContext bac) {

		// Lists
		for (GraphField field : out(HAS_LIST).frame(GraphField.class)) {
			field.removeField(bac, this);
		}

	}

}
