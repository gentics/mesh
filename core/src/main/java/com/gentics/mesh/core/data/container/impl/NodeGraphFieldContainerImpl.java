package com.gentics.mesh.core.data.container.impl;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.DisplayField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UniquenessUtil;
import com.gentics.mesh.util.VersionNumber;
import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.syncleus.ferma.traversals.EdgeTraversal;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.gentics.mesh.core.data.GraphFieldContainerEdge.BRANCH_UUID_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.EDGE_TYPE_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_INDEX_NAME;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_URLFIELD_INDEX_NAME;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.error.Errors.nodeConflict;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

/**
 * @see NodeGraphFieldContainer
 */
public class NodeGraphFieldContainerImpl extends AbstractGraphFieldContainerImpl implements NodeGraphFieldContainer {

	private static final Logger log = LoggerFactory.getLogger(NodeGraphFieldContainerImpl.class);

	public static final String DISPLAY_FIELD_PROPERTY_KEY = "displayFieldValue";

	public static final String VERSION_PROPERTY_KEY = "version";

	public static final String EDITOR_UUID_PROPERTY_KEY = "editor";

	public static void init(Database database) {
		database.addVertexType(NodeGraphFieldContainerImpl.class, MeshVertexImpl.class);
	}

	@Override
	public void setSchemaContainerVersion(GraphFieldSchemaContainerVersion<?, ?, ?, ?, ?> version) {
		setSingleLinkOutTo(version, HAS_SCHEMA_CONTAINER_VERSION);
	}

	@Override
	public SchemaContainerVersion getSchemaContainerVersion() {
		return out(HAS_SCHEMA_CONTAINER_VERSION).has(SchemaContainerVersionImpl.class).nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public String getDisplayFieldValue() {
		// Normally the display field value would be loaded by
		// 1. Loading the displayField name from the used schema
		// 2. Loading the string field with the found name
		// 3. Loading the value from that field
		// This is very costly and thus we store the precomputed display field
		// within a local property.
		return property(DISPLAY_FIELD_PROPERTY_KEY);
	}

	@Override
	public void updateDisplayFieldValue() {
		// TODO use schema storage instead
		Schema schema = getSchemaContainerVersion().getSchema();
		String displayFieldName = schema.getDisplayField();
		FieldSchema fieldSchema = schema.getField(displayFieldName);
		// Only update the display field value if the field can be located
		if (fieldSchema != null) {
			GraphField field = getField(fieldSchema);
			if (field != null && field instanceof DisplayField) {
				DisplayField displayField = (DisplayField) field;
				property(DISPLAY_FIELD_PROPERTY_KEY, displayField.getDisplayName());
				return;
			}
		}
		// Otherwise reset the value to null
		property(DISPLAY_FIELD_PROPERTY_KEY, null);
	}

	@Override
	public void delete(BulkActionContext bac) {
		delete(bac, true);
	}

	@Override
	public void delete(BulkActionContext bac, boolean deleteNext) {

		if (deleteNext) {
			// Recursively delete all versions of the container
			for (NodeGraphFieldContainer next : getNextVersions()) {
				next.delete(bac);
			}
		}

		// Invoke common field removal operations
		super.delete(bac);

		for (BinaryGraphField binaryField : outE(HAS_FIELD).has(BinaryGraphFieldImpl.class).frameExplicit(BinaryGraphFieldImpl.class)) {
			binaryField.removeField(bac, this);
		}

		for (MicronodeGraphField micronodeField : outE(HAS_FIELD).has(MicronodeGraphFieldImpl.class).frameExplicit(MicronodeGraphFieldImpl.class)) {
			micronodeField.removeField(bac, this);
		}

		// Delete the container from all branches and types
		getBranchTypes().forEach(tuple -> {
			String branchUuid = tuple.v1();
			ContainerType type = tuple.v2();
			if (type != ContainerType.INITIAL) {
				bac.add(onDeleted(branchUuid, type));
			}
		});

		// We don't need to handle node fields since those are only edges and will automatically be removed
		getElement().remove();
		bac.inc();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void deleteFromBranch(Branch branch, BulkActionContext bac) {
		String branchUuid = branch.getUuid();

		bac.batch().add(onDeleted(branchUuid, DRAFT));
		if (isPublished(branchUuid)) {
			bac.batch().add(onDeleted(branchUuid, PUBLISHED));
		}
		// Remove the edge between the node and the container that matches the branch
		inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branchUuid).or(e -> e.traversal().has(
			GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode()),
			e -> e.traversal().has(
				GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.PUBLISHED.getCode()))
			.removeAll();
	}

	@Override
	public void updateFieldsFromRest(InternalActionContext ac, FieldMap restFields) {
		super.updateFieldsFromRest(ac, restFields);
		String branchUuid = ac.getBranch().getUuid();

		updateWebrootPathInfo(ac, branchUuid, "node_conflicting_segmentfield_update");
		updateDisplayFieldValue();
	}

	@Override
	public Set<String> getUrlFieldValues() {
		SchemaModel schema = getSchemaContainerVersion().getSchema();

		Set<String> urlFieldValues = new HashSet<>();
		if (schema.getUrlFields() != null) {
			for (String urlField : schema.getUrlFields()) {
				FieldSchema fieldSchema = schema.getField(urlField);
				GraphField field = getField(fieldSchema);
				if (field instanceof StringGraphFieldImpl) {
					StringGraphFieldImpl stringField = (StringGraphFieldImpl) field;
					String value = stringField.getString();
					if (value != null) {
						urlFieldValues.add(value);
					}
				}
				if (field instanceof StringGraphFieldListImpl) {
					StringGraphFieldListImpl stringListField = (StringGraphFieldListImpl) field;
					for (StringGraphField listField : stringListField.getList()) {
						if (listField != null) {
							String value = listField.getString();
							if (value != null) {
								urlFieldValues.add(value);
							}
						}
					}
				}
			}
		}
		return urlFieldValues;
	}

	/**
	 * Update the webroot url field index and also assert that the new values would not cause a conflict with the existing data.
	 * 
	 * @param edge
	 * @param branchUuid
	 * @param urlFieldValues
	 * @param type
	 */
	private void updateWebrootUrlFieldsInfo(GraphFieldContainerEdge edge, String branchUuid, Set<String> urlFieldValues, ContainerType type) {
		if (urlFieldValues != null && !urlFieldValues.isEmpty()) {
			// Individually check each url
			for (String urlFieldValue : urlFieldValues) {
				Object key = GraphFieldContainerEdgeImpl.composeWebrootUrlFieldIndexKey(urlFieldValue, branchUuid, type);
				GraphFieldContainerEdge conflictingEdge = MeshInternal.get().database().checkIndexUniqueness(WEBROOT_URLFIELD_INDEX_NAME, edge, key);
				if (conflictingEdge != null) {
					NodeGraphFieldContainer conflictingContainer = conflictingEdge.getNodeContainer();
					Node conflictingNode = conflictingEdge.getNode();
					if (log.isDebugEnabled()) {
						log.debug(
							"Found conflicting container with uuid {" + conflictingContainer.getUuid() + "} of node {" + conflictingNode.getUuid());
					}
					// We know that the found container already occupies the index with one of the given paths. Lets compare both sets of paths in order to
					// determine
					// which path caused the conflict.
					Set<String> fromConflictingContainer = conflictingContainer.getUrlFieldValues();
					@SuppressWarnings("unchecked")
					Collection<String> conflictingValues = CollectionUtils.intersection(fromConflictingContainer, urlFieldValues);
					String paths = conflictingValues.stream().map(n -> n.toString()).collect(Collectors.joining(","));

					throw nodeConflict(conflictingNode.getUuid(), conflictingContainer.getDisplayFieldValue(), conflictingContainer.getLanguageTag(),
						"node_conflicting_urlfield_update", paths, conflictingContainer.getParentNode().getUuid(),
						conflictingContainer.getLanguageTag());
				}
			}
			edge.setUrlFieldInfo(urlFieldValues);
		} else {
			edge.setUrlFieldInfo(null);
		}

	}

	@Override
	public void updateWebrootPathInfo(InternalActionContext ac, String branchUuid, String conflictI18n) {
		Set<String> urlFieldValues = getUrlFieldValues();
		Iterator<? extends GraphFieldContainerEdge> it = getContainerEdge(DRAFT, branchUuid);
		if (it.hasNext()) {
			GraphFieldContainerEdge draftEdge = it.next();
			updateWebrootPathInfo(ac, draftEdge, branchUuid, conflictI18n, DRAFT);
			updateWebrootUrlFieldsInfo(draftEdge, branchUuid, urlFieldValues, DRAFT);
		}
		it = getContainerEdge(PUBLISHED, branchUuid);
		if (it.hasNext()) {
			GraphFieldContainerEdge publishEdge = it.next();
			updateWebrootPathInfo(ac, publishEdge, branchUuid, conflictI18n, PUBLISHED);
			updateWebrootUrlFieldsInfo(publishEdge, branchUuid, urlFieldValues, PUBLISHED);
		}
	}

	/**
	 * Update the webroot path info (checking for uniqueness before)
	 *
	 * @param ac
	 * @param edge
	 * @param branchUuid
	 *            branch Uuid
	 * @param conflictI18n
	 *            i18n for the message in case of conflict
	 * @param type
	 *            edge type
	 */
	protected void updateWebrootPathInfo(InternalActionContext ac, GraphFieldContainerEdge edge, String branchUuid, String conflictI18n,
		ContainerType type) {
		final int MAX_NUMBER = 255;
		Node node = getParentNode();
		String segmentFieldName = getSchemaContainerVersion().getSchema().getSegmentField();
		String languageTag = getLanguageTag();

		// Handle node migration conflicts automagically
		if (ac instanceof NodeMigrationActionContextImpl) {
			NodeMigrationActionContextImpl nmac = (NodeMigrationActionContextImpl) ac;
			ConflictWarning info = null;
			for (int i = 0; i < MAX_NUMBER; i++) {
				try {
					if (updateWebrootPathInfo(node, edge, languageTag, branchUuid, segmentFieldName, conflictI18n, type)) {
						break;
					}
				} catch (NameConflictException e) {
					// Only throw the exception if we tried multiple renames
					if (i >= MAX_NUMBER - 1) {
						throw e;
					} else {
						// Generate some information about the found conflict
						info = new ConflictWarning();
						info.setNodeUuid(node.getUuid());
						info.setBranchUuid(branchUuid);
						info.setType(type.name());
						info.setLanguageTag(languageTag);
						info.setFieldName(segmentFieldName);
						node.postfixPathSegment(branchUuid, type, languageTag);
					}
				}
			}
			// We encountered a conflict which was resolved. Lets add that info to the context
			if (info != null) {
				nmac.addConflictInfo(info);
			}
		} else {
			updateWebrootPathInfo(node, edge, languageTag, branchUuid, segmentFieldName, conflictI18n, type);
		}

	}

	private boolean updateWebrootPathInfo(Node node, GraphFieldContainerEdge edge, String languageTag, String branchUuid, String segmentFieldName,
		String conflictI18n,
		ContainerType type) {
		// Determine the webroot path of the container parent node
		String segment = node.getPathSegment(branchUuid, type, getLanguageTag());

		// The webroot uniqueness will be checked by validating that the string [segmentValue-branchUuid-parentNodeUuid] is only listed once within the given
		// specific index for (drafts or published nodes)
		if (segment != null) {
			Node parentNode = node.getParentNode(branchUuid);
			String segmentInfo = GraphFieldContainerEdgeImpl.composeSegmentInfo(parentNode, segment);
			Object webRootIndexKey = GraphFieldContainerEdgeImpl.composeWebrootIndexKey(segmentInfo, branchUuid, type);
			// check for uniqueness of webroot path
			GraphFieldContainerEdge conflictingEdge = MeshInternal.get().database().checkIndexUniqueness(WEBROOT_INDEX_NAME, edge, webRootIndexKey);
			if (conflictingEdge != null) {
				Node conflictingNode = conflictingEdge.getNode();
				NodeGraphFieldContainer conflictingContainer = conflictingEdge.getNodeContainer();
				if (log.isDebugEnabled()) {
					log.debug("Found conflicting container with uuid {" + conflictingContainer.getUuid() + "} of node {" + conflictingNode.getUuid()
						+ "}");
				}
				throw nodeConflict(conflictingNode.getUuid(), conflictingContainer.getDisplayFieldValue(), conflictingContainer.getLanguageTag(),
					conflictI18n, segmentFieldName, segment);
			} else {
				edge.setSegmentInfo(segmentInfo);
				return true;
			}
		} else {
			edge.setSegmentInfo(null);
			return true;
		}
	}

	@Override
	public Node getParentNode(String branchUuid) {
		return inE(HAS_FIELD_CONTAINER).has(
			GraphFieldContainerEdgeImpl.BRANCH_UUID_KEY, branchUuid).outV().nextOrDefaultExplicit(NodeImpl.class, null);
	}

	/**
	 * Get the parent node
	 * 
	 * @return parent node
	 */
	public Node getParentNode() {
		Node parentNode = in(HAS_FIELD_CONTAINER).nextOrDefaultExplicit(NodeImpl.class, null);
		if (parentNode == null) {
			// the field container is not directly linked to its Node, get the
			// initial field container
			NodeGraphFieldContainer initial = null;
			NodeGraphFieldContainer previous = getPreviousVersion();
			while (previous != null) {
				initial = previous;
				previous = previous.getPreviousVersion();
			}

			if (initial != null) {
				return initial.getParentNode();
			}
			throw error(INTERNAL_SERVER_ERROR, "error_field_container_without_node");
		}
		return parentNode;
	}

	@Override
	public void setVersion(VersionNumber version) {
		property(VERSION_PROPERTY_KEY, version.toString());
	}

	@Override
	public VersionNumber getVersion() {
		String version = property(VERSION_PROPERTY_KEY);
		return version == null ? null : new VersionNumber(version);
	}

	@Override
	public boolean hasNextVersion() {
		return outE(HAS_VERSION).hasNext();
	}

	@Override
	public TraversalResult<? extends NodeGraphFieldContainer> getNextVersions() {
		return new TraversalResult<>(out(HAS_VERSION).frameExplicit(NodeGraphFieldContainerImpl.class));
	}

	@Override
	public void setNextVersion(NodeGraphFieldContainer container) {
		linkOut(container, HAS_VERSION);
	}

	@Override
	public boolean hasPreviousVersion() {
		return inE(HAS_VERSION).hasNext();
	}

	@Override
	public NodeGraphFieldContainer getPreviousVersion() {
		return in(HAS_VERSION).nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
	}

	@Override
	public void clone(NodeGraphFieldContainer container) {
		List<GraphField> otherFields = container.getFields();

		for (GraphField graphField : otherFields) {
			graphField.cloneTo(this);
		}
	}

	@Override
	public boolean isType(ContainerType type) {
		EdgeTraversal<?, ?, ?> traversal = inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode());
		return traversal.hasNext();
	}

	@Override
	public boolean isType(ContainerType type, String branchUuid) {
		return getContainerEdge(type, branchUuid).hasNext();
	}

	@Override
	public Iterator<? extends GraphFieldContainerEdge> getContainerEdge(ContainerType type, String branchUuid) {
		EdgeTraversal<?, ?, ?> traversal = inE(HAS_FIELD_CONTAINER)
			.has(BRANCH_UUID_KEY, branchUuid)
			.has(EDGE_TYPE_KEY, type.getCode());
		return traversal.frameExplicit(GraphFieldContainerEdgeImpl.class).iterator();
	}

	@Override
	public Set<Tuple<String, ContainerType>> getBranchTypes() {
		Set<Tuple<String, ContainerType>> typeSet = new HashSet<>();
		inE(HAS_FIELD_CONTAINER).frameExplicit(GraphFieldContainerEdgeImpl.class).forEach(edge -> typeSet.add(Tuple.tuple(edge.getBranchUuid(), edge
			.getType())));
		return typeSet;
	}

	@Override
	public Set<String> getBranches(ContainerType type) {
		Set<String> branchUuids = new HashSet<>();
		inE(HAS_FIELD_CONTAINER).has(EDGE_TYPE_KEY, type.getCode()).frameExplicit(GraphFieldContainerEdgeImpl.class)
			.forEach(edge -> branchUuids.add(edge.getBranchUuid()));
		return branchUuids;
	}

	@Override
	public void validate() {
		Schema schema = getSchemaContainerVersion().getSchema();
		Map<String, GraphField> fieldsMap = getFields().stream().collect(Collectors.toMap(GraphField::getFieldKey, Function.identity()));

		schema.getFields().stream().forEach(fieldSchema -> {
			GraphField field = fieldsMap.get(fieldSchema.getName());
			if (fieldSchema.isRequired() && field == null) {
				throw error(CONFLICT, "node_error_missing_mandatory_field_value", fieldSchema.getName(), schema.getName());
			}
			if (field != null) {
				field.validate();
			}
		});
	}

	@Override
	public List<FieldContainerChange> compareTo(FieldMap fieldMap) {
		List<FieldContainerChange> changes = new ArrayList<>();

		Schema schemaA = getSchemaContainerVersion().getSchema();
		Map<String, FieldSchema> fieldSchemaMap = schemaA.getFieldsAsMap();

		// Handle all fields
		for (String fieldName : fieldSchemaMap.keySet()) {
			FieldSchema fieldSchema = fieldSchemaMap.get(fieldName);
			// Check content
			GraphField fieldA = getField(fieldSchema);
			Field fieldB = fieldMap.getField(fieldName, fieldSchema);
			// Handle null cases. The field may not have been created yet.
			if (fieldA != null && fieldB == null && fieldMap.hasField(fieldName)) {
				// Field only exists in A
				changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
			} else if (fieldA == null && fieldB != null) {
				// Field only exists in B
				changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
			} else if (fieldA != null && fieldB != null) {
				// Field exists in A and B and the fields are not equal to each
				// other.
				changes.addAll(fieldA.compareTo(fieldB));
			} else {
				// Both fields are equal if those fields are both null
			}

		}
		return changes;
	}

	@Override
	public List<FieldContainerChange> compareTo(NodeGraphFieldContainer container) {
		List<FieldContainerChange> changes = new ArrayList<>();

		Schema schemaA = getSchemaContainerVersion().getSchema();
		Map<String, FieldSchema> fieldMapA = schemaA.getFieldsAsMap();
		Schema schemaB = container.getSchemaContainerVersion().getSchema();
		Map<String, FieldSchema> fieldMapB = schemaB.getFieldsAsMap();
		// Generate a structural diff first. This way it is easy to determine
		// which fields have been added or removed.
		MapDifference<String, FieldSchema> diff = Maps.difference(fieldMapA, fieldMapB, new Equivalence<FieldSchema>() {

			@Override
			protected boolean doEquivalent(FieldSchema a, FieldSchema b) {
				return a.getName().equals(b.getName());
			}

			@Override
			protected int doHash(FieldSchema t) {
				// TODO Auto-generated method stub
				return 0;
			}

		});

		// Handle fields which exist only in A - They have been removed in B
		for (FieldSchema field : diff.entriesOnlyOnLeft().values()) {
			changes.add(new FieldContainerChange(field.getName(), FieldChangeTypes.REMOVED));
		}

		// Handle fields which don't exist in A - They have been added in B
		for (FieldSchema field : diff.entriesOnlyOnRight().values()) {
			changes.add(new FieldContainerChange(field.getName(), FieldChangeTypes.ADDED));
		}

		// Handle fields which are common in both schemas
		for (String fieldName : diff.entriesInCommon().keySet()) {
			FieldSchema fieldSchemaA = fieldMapA.get(fieldName);
			FieldSchema fieldSchemaB = fieldMapB.get(fieldName);
			// Check whether the field type is different in between both schemas
			if (fieldSchemaA.getType().equals(fieldSchemaB.getType())) {
				// Check content
				GraphField fieldA = getField(fieldSchemaA);
				GraphField fieldB = container.getField(fieldSchemaB);
				// Handle null cases. The field may not have been created yet.
				if (fieldA != null && fieldB == null) {
					// Field only exists in A
					changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
				} else if (fieldA == null && fieldB != null) {
					// Field only exists in B
					changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
				} else if (fieldA != null && fieldB != null) {
					changes.addAll(fieldA.compareTo(fieldB));
				} else {
					// Both fields are equal if those fields are both null
				}
			} else {
				// The field type has changed
				changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
			}

		}
		return changes;
	}

	@Override
	public List<? extends MicronodeGraphField> getMicronodeFields(MicroschemaContainerVersion version) {
		return outE(HAS_FIELD).mark().inV().has(MicronodeImpl.class).out(HAS_MICROSCHEMA_CONTAINER).has(MicroschemaContainerVersionImpl.class).has(
			"uuid", version.getUuid()).back().toListExplicit(MicronodeGraphFieldImpl.class);
	}

	@Override
	public List<? extends MicronodeGraphFieldList> getMicronodeListFields(MicroschemaContainerVersion version) {
		return out(HAS_LIST).has(MicronodeGraphFieldListImpl.class).mark().out(HAS_ITEM).has(MicronodeImpl.class).out(HAS_MICROSCHEMA_CONTAINER).has(
			MicroschemaContainerVersionImpl.class).has("uuid", version.getUuid()).back().toListExplicit(MicronodeGraphFieldListImpl.class);
	}

	@Override
	public String getETag(InternalActionContext ac) {
		Stream<String> referencedUuids = StreamSupport.stream(getReferencedNodes().spliterator(), false)
			.map(Node::getUuid);

		int hashcode = Stream.concat(Stream.of(getUuid()), referencedUuids)
			.collect(Collectors.toSet())
			.hashCode();

		return ETag.hash(hashcode);
	}

	@Override
	public User getEditor() {
		String uuid = property(EDITOR_UUID_PROPERTY_KEY);
		if (uuid == null) {
			return null;
		} else {
			UserRoot userRoot = MeshInternal.get().boot().userRoot();
			return userRoot.findByUuid(uuid);
		}
	}

	@Override
	public void setEditor(User user) {
		String uuid = user == null
			? null
			: user.getUuid();
		property(EDITOR_UUID_PROPERTY_KEY, uuid);
	}

	@Override
	public String getSegmentFieldValue() {
		String segmentFieldKey = getSchemaContainerVersion().getSchema().getSegmentField();
		// 1. The container may reference a schema which has no segment field set thus no path segment can be determined
		if (segmentFieldKey == null) {
			return null;
		}

		// 2. Try to load the path segment using the string field
		StringGraphField stringField = getString(segmentFieldKey);
		if (stringField != null) {
			return stringField.getString();
		}

		// 3. Try to load the path segment using the binary field since the string field could not be found
		if (stringField == null) {
			BinaryGraphField binaryField = getBinary(segmentFieldKey);
			if (binaryField != null) {
				return binaryField.getFileName();
			}
		}
		return null;
	}

	@Override
	public void postfixSegmentFieldValue() {
		String segmentFieldKey = getSchemaContainerVersion().getSchema().getSegmentField();
		// 1. The container may reference a schema which has no segment field set thus no path segment can be determined
		if (segmentFieldKey == null) {
			return;
		}

		// 2. Try to load the path segment using the string field
		StringGraphField stringField = getString(segmentFieldKey);
		if (stringField != null) {
			String oldValue = stringField.getString();
			if (oldValue != null) {
				stringField.setString(UniquenessUtil.suggestNewName(oldValue));
			}
		}

		// 3. Try to load the path segment using the binary field since the string field could not be found
		if (stringField == null) {
			BinaryGraphField binaryField = getBinary(segmentFieldKey);
			if (binaryField != null) {
				binaryField.postfixFileName();
			}
		}
	}

	public com.gentics.mesh.path.Path getPath(InternalActionContext ac) {
		Path nodePath = new Path();
		nodePath.addSegment(new PathSegment(this, null, getLanguageTag(), null));
		return nodePath;
	}

	@Override
	public NodeMeshEventModel onDeleted(String branchUuid, ContainerType type) {
		return createEvent(NODE_CONTENT_DELETED, branchUuid, type);
	}

	@Override
	public NodeMeshEventModel onUpdated(String branchUuid, ContainerType type) {
		return createEvent(NODE_UPDATED, branchUuid, type);
	}

	@Override
	public NodeMeshEventModel onCreated(String branchUuid, ContainerType type) {
		return createEvent(NODE_CONTENT_CREATED, branchUuid, type);
	}

	@Override
	public NodeMeshEventModel onTakenOffline(String branchUuid) {
		return createEvent(NODE_UNPUBLISHED, branchUuid, ContainerType.PUBLISHED);
	}

	@Override
	public NodeMeshEventModel onPublish(String branchUuid) {
		return createEvent(NODE_PUBLISHED, branchUuid, ContainerType.PUBLISHED);
	}

	/**
	 * Create a new node event.
	 * 
	 * @param event
	 *            Type of the event
	 * @param branchUuid
	 *            Branch Uuid if known
	 * @param type
	 *            Type of the node content if known
	 * @return Created model
	 */
	private NodeMeshEventModel createEvent(MeshEvent event, String branchUuid, ContainerType type) {
		NodeMeshEventModel model = new NodeMeshEventModel();
		model.setEvent(event);
		Node node = getParentNode(branchUuid);
		String nodeUuid = node.getUuid();
		model.setUuid(nodeUuid);
		model.setBranchUuid(branchUuid);
		model.setLanguageTag(getLanguageTag());
		model.setType(type);
		SchemaContainerVersion version = getSchemaContainerVersion();
		if (version != null) {
			model.setSchema(version.transformToReference());
		}
		Project project = node.getProject();
		model.setProject(project.transformToReference());
		return model;
	}

}
