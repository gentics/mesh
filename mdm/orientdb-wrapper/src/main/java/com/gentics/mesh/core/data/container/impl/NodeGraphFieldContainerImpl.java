package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.GraphFieldContainerEdge.BRANCH_UUID_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.EDGE_TYPE_KEY;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_INDEX_NAME;
import static com.gentics.mesh.core.data.GraphFieldContainerEdge.WEBROOT_URLFIELD_INDEX_NAME;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.MICROSCHEMA_VERSION_KEY_PROPERTY;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.SCHEMA_CONTAINER_VERSION_KEY_PROPERTY;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.error.Errors.nodeConflict;
import static com.gentics.mesh.madl.field.FieldType.LONG;
import static com.gentics.mesh.madl.field.FieldType.STRING;
import static com.gentics.mesh.madl.index.IndexType.NOTUNIQUE;
import static com.gentics.mesh.madl.index.VertexIndexDefinition.vertexIndex;
import static com.gentics.mesh.madl.type.VertexTypeDefinition.vertexType;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.GraphFieldContainerEdge;
import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.DisplayField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.S3BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.BucketableElementHelper;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.impl.PathImpl;
import com.gentics.mesh.path.impl.PathSegmentImpl;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UniquenessUtil;
import com.gentics.mesh.util.VersionNumber;
import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.syncleus.ferma.traversals.EdgeTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see NodeGraphFieldContainer
 */
public class NodeGraphFieldContainerImpl extends AbstractGraphFieldContainerImpl implements NodeGraphFieldContainer {

	private static final Logger log = LoggerFactory.getLogger(NodeGraphFieldContainerImpl.class);

	public static final String DISPLAY_FIELD_PROPERTY_KEY = "displayFieldValue";

	public static final String VERSION_PROPERTY_KEY = "version";

	// Cached instance of the parent node.
	private NodeImpl parentNodeRef;

	/**
	 * Initialize the vertex type and indices.
	 * 
	 * @param type
	 * @param index
	 */
	public static void init(TypeHandler type, IndexHandler index) {
		type.createType(vertexType(NodeGraphFieldContainerImpl.class, MeshVertexImpl.class)
			.withField(SCHEMA_CONTAINER_VERSION_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeGraphFieldContainerImpl.class)
			.withField(SCHEMA_CONTAINER_VERSION_KEY_PROPERTY, STRING));

		index.createIndex(vertexIndex(NodeGraphFieldContainerImpl.class)
			.withField(SCHEMA_CONTAINER_VERSION_KEY_PROPERTY, STRING)
			.withField(BucketableElementHelper.BUCKET_ID_KEY, LONG)
			.withType(NOTUNIQUE)
			.withPostfix("bucket"));
	}

	@Override
	public void setSchemaContainerVersion(HibFieldSchemaVersionElement version) {
		property(SCHEMA_CONTAINER_VERSION_KEY_PROPERTY, version.getUuid());
	}

	@Override
	public HibSchemaVersion getSchemaContainerVersion() {
		return db().index().findByUuid(SchemaContainerVersionImpl.class, property(SCHEMA_CONTAINER_VERSION_KEY_PROPERTY));
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
		SchemaModel schema = getSchemaContainerVersion().getSchema();
		String displayFieldName = schema.getDisplayField();
		FieldSchema fieldSchema = schema.getField(displayFieldName);
		// Only update the display field value if the field can be located
		if (fieldSchema != null) {
			HibField field = getField(fieldSchema);
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
			for (HibNodeFieldContainer next : getNextVersions()) {
				next.delete(bac);
			}
		}

		// Invoke common field removal operations
		super.delete(bac);

		for (BinaryGraphField binaryField : outE(HAS_FIELD).has(BinaryGraphFieldImpl.class).frameExplicit(BinaryGraphFieldImpl.class)) {
			binaryField.removeField(bac, this);
		}

		for (S3BinaryGraphField s3binaryField : outE(HAS_FIELD).has(S3BinaryGraphFieldImpl.class).frameExplicit(S3BinaryGraphFieldImpl.class)) {
			s3binaryField.removeField(bac, this);
		}

		for (MicronodeGraphField micronodeField : outE(HAS_FIELD).has(MicronodeGraphFieldImpl.class).frameExplicit(MicronodeGraphFieldImpl.class)) {
			micronodeField.removeField(bac, this);
		}
		ContentDao contentDao = Tx.get().contentDao();

		// Delete the container from all branches and types
		getBranchTypes().forEach(tuple -> {
			String branchUuid = tuple.v1();
			ContainerType type = tuple.v2();
			if (type != ContainerType.INITIAL) {
				bac.add(contentDao.onDeleted(this, branchUuid, type));
			}
		});

		// We don't need to handle node fields since those are only edges and will automatically be removed
		getElement().remove();
		bac.inc();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void deleteFromBranch(HibBranch branch, BulkActionContext bac) {
		ContentDao contentDao = Tx.get().contentDao();
		String branchUuid = branch.getUuid();

		bac.batch().add(contentDao.onDeleted(this, branchUuid, DRAFT));
		if (isPublished(branchUuid)) {
			bac.batch().add(contentDao.onDeleted(this, branchUuid, PUBLISHED));
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
		String branchUuid = Tx.get().getBranch(ac).getUuid();

		updateWebrootPathInfo(ac, branchUuid, "node_conflicting_segmentfield_update");
		updateDisplayFieldValue();
	}

	@Override
	public Stream<String> getUrlFieldValues() {
		SchemaVersionModel schema = getSchemaContainerVersion().getSchema();

		List<String> urlFields = schema.getUrlFields();
		if (urlFields == null) {
			return Stream.empty();
		}
		return urlFields.stream().flatMap(urlField -> {
			FieldSchema fieldSchema = schema.getField(urlField);
			HibField field = getField(fieldSchema);
			if (field instanceof StringGraphFieldImpl) {
				StringGraphFieldImpl stringField = (StringGraphFieldImpl) field;
				String value = stringField.getString();
				if (StringUtils.isBlank(value)) {
					return Stream.empty();
				} else {
					return Stream.of(value);
				}
			}
			if (field instanceof StringGraphFieldListImpl) {
				StringGraphFieldListImpl stringListField = (StringGraphFieldListImpl) field;
				return stringListField.getList().stream()
					.flatMap(listField -> Optional.ofNullable(listField)
						.map(HibStringField::getString)
						.filter(StringUtils::isNotBlank)
						.stream());
			}

			return Stream.empty();
		});
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
				Object key = GraphFieldContainerEdgeImpl.composeWebrootUrlFieldIndexKey(db(), urlFieldValue, branchUuid, type);
				GraphFieldContainerEdge conflictingEdge = mesh().database().index().checkIndexUniqueness(WEBROOT_URLFIELD_INDEX_NAME, edge, key);
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
					Set<String> fromConflictingContainer = conflictingContainer.getUrlFieldValues().collect(Collectors.toSet());
					@SuppressWarnings("unchecked")
					Collection<String> conflictingValues = CollectionUtils.intersection(fromConflictingContainer, urlFieldValues);
					String paths = conflictingValues.stream().map(n -> n.toString()).collect(Collectors.joining(","));

					throw nodeConflict(conflictingNode.getUuid(), conflictingContainer.getDisplayFieldValue(), conflictingContainer.getLanguageTag(),
						"node_conflicting_urlfield_update", paths, conflictingContainer.getNode().getUuid(),
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
		Set<String> urlFieldValues = getUrlFieldValues().collect(Collectors.toSet());
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
		NodeImpl node = getNode();
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
		NodeDao nodeDao = Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		// Determine the webroot path of the container parent node
		String segment = contentDao.getPathSegment(node, branchUuid, type, getLanguageTag());

		// The webroot uniqueness will be checked by validating that the string [segmentValue-branchUuid-parentNodeUuid] is only listed once within the given
		// specific index for (drafts or published nodes)
		if (segment != null) {
			HibNode parentNode = nodeDao.getParentNode(node, branchUuid);
			String segmentInfo = GraphFieldContainerEdgeImpl.composeSegmentInfo(parentNode, segment);
			Object webRootIndexKey = GraphFieldContainerEdgeImpl.composeWebrootIndexKey(db(), segmentInfo, branchUuid, type);
			// check for uniqueness of webroot path
			GraphFieldContainerEdge conflictingEdge = db().index().checkIndexUniqueness(WEBROOT_INDEX_NAME, edge, webRootIndexKey);
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

	/**
	 * Get the parent node
	 * 
	 * @return parent node
	 */
	public NodeImpl getNode() {
		// Return pre-loaded instance
		if (parentNodeRef != null) {
			return parentNodeRef;
		}

		NodeImpl parentNode = in(HAS_FIELD_CONTAINER, NodeImpl.class).nextOrNull();
		if (parentNode == null) {
			// the field container is not directly linked to its Node, get the
			// initial field container
			NodeGraphFieldContainerImpl initial = null;
			NodeGraphFieldContainerImpl previous = getPreviousVersion();
			while (previous != null) {
				initial = previous;
				previous = previous.getPreviousVersion();
			}

			if (initial != null) {
				return initial.getNode();
			}
			throw error(INTERNAL_SERVER_ERROR, "error_field_container_without_node");
		}
		parentNodeRef = parentNode;
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
	public Result<HibNodeFieldContainer> getNextVersions() {
		// TODO out function should not return wildcard generics.
		return (Result<HibNodeFieldContainer>) (Result<?>) out(HAS_VERSION, NodeGraphFieldContainerImpl.class);
	}

	@Override
	public void setNextVersion(HibNodeFieldContainer container) {
		linkOut(toGraph(container), HAS_VERSION);
	}

	@Override
	public boolean hasPreviousVersion() {
		return inE(HAS_VERSION).hasNext();
	}

	@Override
	public NodeGraphFieldContainerImpl getPreviousVersion() {
		return in(HAS_VERSION, NodeGraphFieldContainerImpl.class).nextOrNull();
	}

	@Override
	public void clone(HibNodeFieldContainer container) {
		List<HibField> otherFields = container.getFields();

		for (HibField graphField : otherFields) {
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
	public Iterator<GraphFieldContainerEdge> getContainerEdge(ContainerType type, String branchUuid) {
		EdgeTraversal<?, ?, ?> traversal = inE(HAS_FIELD_CONTAINER)
			.has(BRANCH_UUID_KEY, branchUuid)
			.has(EDGE_TYPE_KEY, type.getCode());
		return traversal.<GraphFieldContainerEdge>frameExplicit(GraphFieldContainerEdgeImpl.class).iterator();
	}

	/**
	 * Return a set which contains all branchUuid<->containerType variations that match the edges for the content.
	 * 
	 * @return
	 */
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
		SchemaModel schema = getSchemaContainerVersion().getSchema();
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

	@Override
	public List<HibMicronodeField> getMicronodeFields(HibMicroschemaVersion version) {
		String microschemaVersionUuid = version.getUuid();
		return new TraversalResult<>(outE(HAS_FIELD)
			.has(MicronodeGraphFieldImpl.class)
			.frameExplicit(MicronodeGraphFieldImpl.class))
				.stream()
				.filter(edge -> toGraph(edge.getMicronode()).property(MICROSCHEMA_VERSION_KEY_PROPERTY).equals(microschemaVersionUuid))
				.collect(Collectors.toList());
	}

	@Override
	public Result<HibMicronodeFieldList> getMicronodeListFields(HibMicroschemaVersion version) {
		String microschemaVersionUuid = version.getUuid();
		TraversalResult<? extends MicronodeGraphFieldList> lists = new TraversalResult<>(out(HAS_LIST)
			.has(MicronodeGraphFieldListImpl.class)
			.frameExplicit(MicronodeGraphFieldListImpl.class));
		return new TraversalResult<>(lists
			.stream()
			.filter(list -> list.getValues().stream()
				.anyMatch(micronode -> toGraph(micronode).property(MICROSCHEMA_VERSION_KEY_PROPERTY).equals(microschemaVersionUuid))));
	}

	@Override
	public String getETag(InternalActionContext ac) {
		Stream<String> referencedUuids = StreamSupport.stream(getReferencedNodes().spliterator(), false)
			.map(HibNode::getUuid);

		int hashcode = Stream.concat(Stream.of(getUuid()), referencedUuids)
			.collect(Collectors.toSet())
			.hashCode();

		return ETag.hash(hashcode);
	}

	@Override
	public HibUser getEditor() {
		return mesh().userProperties().getEditor(this);
	}

	@Override
	public void setEditor(HibUser user) {
		mesh().userProperties().setEditor(this, user);
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

		// 3. Try to load the path segment using the binary field or the s3 binary since the string field could not be found
		if (stringField == null) {
			S3BinaryGraphField s3binaryField = getS3Binary(segmentFieldKey);
			if (nonNull(s3binaryField)) {
				return s3binaryField.getS3Binary().getFileName();
			}
			BinaryGraphField binary = getBinary(segmentFieldKey);
			if (nonNull(binary)) {
				return binary.getFileName();
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

	/**
	 * Return the path of the content.
	 * 
	 * @param ac
	 * @return Created node path
	 */
	public com.gentics.mesh.path.Path getPath(InternalActionContext ac) {
		Path nodePath = new PathImpl();
		nodePath.addSegment(new PathSegmentImpl(this, null, getLanguageTag(), null));
		return nodePath;
	}

	@Override
	public boolean isPurgeable() {
		// The container is purgeable if no edge (publish, draft, initial) exists to its node.
		return !inE(HAS_FIELD_CONTAINER).hasNext();
	}

	@Override
	public boolean isAutoPurgeEnabled() {
		HibSchemaVersion schema = getSchemaContainerVersion();
		return schema.isAutoPurgeEnabled();
	}

	@Override
	public void purge(BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Purging container {" + getUuid() + "} for version {" + getVersion() + "}");
		}
		// Link the previous to the next to isolate the old container
		NodeGraphFieldContainer beforePrev = getPreviousVersion();
		for (HibNodeFieldContainer afterPrev : getNextVersions()) {
			beforePrev.setNextVersion(afterPrev);
		}
		delete(bac, false);
	}

	@Override
	public Result<HibNodeFieldContainer> versions() {
		return new TraversalResult<>(StreamUtil.untilNull(() -> this, HibNodeFieldContainer::getPreviousVersion));
	}

	@Override
	public Stream<NodeGraphFieldContainer> getContents() {
		return Stream.of(this);
	}

	@Override
	public Integer getBucketId() {
		return BucketableElementHelper.getBucketId(this);
	}

	@Override
	public void setBucketId(Integer bucketId) {
		BucketableElementHelper.setBucketId(this, bucketId);
	}
}
