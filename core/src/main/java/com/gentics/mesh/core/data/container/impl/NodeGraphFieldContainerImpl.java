package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainerEdge.Type;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.VersionNumber;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.MicronodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.util.Tuple;
import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.syncleus.ferma.traversals.EdgeTraversal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeGraphFieldContainerImpl extends AbstractGraphFieldContainerImpl implements NodeGraphFieldContainer {

	public static final String WEBROOT_PROPERTY_KEY = "webrootPathInfo";

	public static final String WEBROOT_INDEX_NAME = "webrootPathInfoIndex";

	public static final String PUBLISHED_WEBROOT_PROPERTY_KEY = "publishedWebrootPathInfo";

	public static final String PUBLISHED_WEBROOT_INDEX_NAME = "publishedWebrootPathInfoIndex";

	public static final String VERSION_PROPERTY_KEY = "version";

	private static final Logger log = LoggerFactory.getLogger(NodeGraphFieldContainerImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(NodeGraphFieldContainerImpl.class);
		database.addVertexIndex(WEBROOT_INDEX_NAME, NodeGraphFieldContainerImpl.class, true, WEBROOT_PROPERTY_KEY);
		database.addVertexIndex(PUBLISHED_WEBROOT_INDEX_NAME, NodeGraphFieldContainerImpl.class, true, PUBLISHED_WEBROOT_PROPERTY_KEY);
	}

	@Override
	public void setSchemaContainerVersion(GraphFieldSchemaContainerVersion<?, ?, ?, ?> version) {
		setSingleLinkOutTo(version.getImpl(), HAS_SCHEMA_CONTAINER_VERSION);
	}

	@Override
	public SchemaContainerVersion getSchemaContainerVersion() {
		return out(HAS_SCHEMA_CONTAINER_VERSION).has(SchemaContainerVersionImpl.class).nextOrDefaultExplicit(SchemaContainerVersionImpl.class, null);
	}

	@Override
	public String getDisplayFieldValue() {
		//TODO use schema storage instead
		Schema schema = getSchemaContainerVersion().getSchema();
		String displayFieldName = schema.getDisplayField();
		StringGraphField field = getString(displayFieldName);
		if (field != null) {
			return field.getString();
		}
		return null;
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// TODO delete linked aggregation nodes for node lists etc

		NodeGraphFieldContainer next = getNextVersion();
		if (next != null) {
			next.delete(batch);
		}

		getReleaseTypes().forEach(tuple -> {
			String releaseUuid = tuple.v1();
			Type type = tuple.v2();
			if (type != Type.INITIAL) {
				addIndexBatchEntry(batch, DELETE_ACTION, releaseUuid, type);
			}
		});

		getElement().remove();
	}

	@SuppressWarnings("unchecked")
	@Override
	public void deleteFromRelease(Release release, SearchQueueBatch batch) {
		String releaseUuid = release.getUuid();

		addIndexBatchEntry(batch, DELETE_ACTION, releaseUuid, Type.DRAFT);
		if (isPublished(releaseUuid)) {
			addIndexBatchEntry(batch, DELETE_ACTION, releaseUuid, Type.PUBLISHED);
		}
		inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
				.or(e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, Type.DRAFT.getCode()),
						e -> e.traversal().has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, Type.PUBLISHED.getCode()))
				.removeAll();
		// remove webroot property
		setProperty(WEBROOT_PROPERTY_KEY, null);
	}

	@Override
	public void setProperty(String name, Object value) {
if (value == null) {
	System.out.println("Remove property " + name + " from " + getUuid());
} else {
	System.out.println("Set property " + name + " to '" + value + "' for " + getUuid());
}
		super.setProperty(name, value);
	}

	@Override
	public void updateFieldsFromRest(InternalActionContext ac, FieldMap restFields) {
		super.updateFieldsFromRest(ac, restFields);

		String segmentFieldName = getSchemaContainerVersion().getSchema().getSegmentField();
		if (restFields.hasField(segmentFieldName)) {
			updateWebrootPathInfo(ac.getRelease(null).getUuid(), "node_conflicting_segmentfield_update");
		}
	}

	@Override
	public void updateWebrootPathInfo(String releaseUuid, String conflictI18n) {
		if (isDraft(releaseUuid)) {
			updateWebrootPathInfo(releaseUuid, conflictI18n, Type.DRAFT, WEBROOT_PROPERTY_KEY, WEBROOT_INDEX_NAME,
					PUBLISHED_WEBROOT_PROPERTY_KEY);
		} else {
			setProperty(WEBROOT_PROPERTY_KEY, null);
		}
		if (isPublished(releaseUuid)) {
			updateWebrootPathInfo(releaseUuid, conflictI18n, Type.PUBLISHED, PUBLISHED_WEBROOT_PROPERTY_KEY,
					PUBLISHED_WEBROOT_INDEX_NAME);
		} else {
			setProperty(PUBLISHED_WEBROOT_PROPERTY_KEY, null);
		}
	}

	/**
	 * Udpdate the webroot path info (checking for uniqueness before)
	 *
	 * @param releaseUuid release Uuid
	 * @param conflictI18n i18n for the message in case of conflict
	 * @param type edge type
	 * @param propertyName name of the property
	 * @param indexNames names of indices to check for uniqueness
	 */
	protected void updateWebrootPathInfo(String releaseUuid, String conflictI18n, Type type, String propertyName,
			String...indexNames) {
		Node node = getParentNode();
		String segmentFieldName = getSchemaContainerVersion().getSchema().getSegmentField();
		// Determine the webroot path of the container parent node
		String segment = node.getPathSegment(releaseUuid, type, getLanguage().getLanguageTag()).toBlocking().last();
		if (segment != null) {
			StringBuilder webRootInfo = new StringBuilder(segment);
			webRootInfo.append("-").append(releaseUuid);
			Node parent = node.getParentNode(releaseUuid);
			if (parent != null) {
				webRootInfo.append("-").append(parent.getUuid());
			}

			// check for uniqueness of webroot path
			for (String indexName : indexNames) {
System.out.println("Check uniqueness of " + webRootInfo.toString() + " in index " + indexName);
				NodeGraphFieldContainerImpl conflictingContainer = MeshSpringConfiguration.getInstance().database()
						.checkIndexUniqueness(indexName, this, webRootInfo.toString());
				if (conflictingContainer != null) {
					Node conflictingNode = conflictingContainer.getParentNode();
					throw conflict(conflictingNode.getUuid(), conflictingContainer.getDisplayFieldValue(), conflictI18n, segmentFieldName, segment);
				}
			}

			setProperty(propertyName, webRootInfo.toString());
		} else {
			setProperty(propertyName, null);
		}
	}

	/**
	 * Get the parent node
	 * 
	 * @return parent node
	 */
	public Node getParentNode() {
		Node parentNode = in(HAS_FIELD_CONTAINER).has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
		if (parentNode == null) {
			// the field container is not directly linked to its Node, get the initial field container
			NodeGraphFieldContainer initial = null;
			NodeGraphFieldContainer previous = getPreviousVersion();
			while (previous != null) {
				initial = previous;
				previous = previous.getPreviousVersion();
			}

			if (initial != null) {
				return initial.getParentNode();
			}
			throw error(BAD_REQUEST, "error_field_container_without_node");
		}
		return parentNode;
	}

	@Override
	public void setVersion(VersionNumber version) {
		setProperty(VERSION_PROPERTY_KEY, version.toString());
	}

	@Override
	public VersionNumber getVersion() {
		String version = getProperty(VERSION_PROPERTY_KEY);
		return version == null ? null : new VersionNumber(version);
	}

	@Override
	public NodeGraphFieldContainer getNextVersion() {
		return out(HAS_VERSION).has(NodeGraphFieldContainerImpl.class).nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
	}

	@Override
	public void setNextVersion(NodeGraphFieldContainer container) {
		setSingleLinkOutTo(container.getImpl(), HAS_VERSION);
	}

	@Override
	public NodeGraphFieldContainer getPreviousVersion() {
		return in(HAS_VERSION).has(NodeGraphFieldContainerImpl.class).nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
	}

	@Override
	public void clone(NodeGraphFieldContainer container) {
		List<GraphField> otherFields = container.getFields();

		for (GraphField graphField : otherFields) {
			graphField.cloneTo(this);
		}
	}

	@Override
	public boolean isDraft(String releaseUuid) {
		EdgeTraversal<?, ?, ?> traversal = inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, Type.DRAFT.getCode());
		return traversal.hasNext();
	}

	@Override
	public boolean isPublished(String releaseUuid) {
		EdgeTraversal<?, ?, ?> traversal = inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, Type.PUBLISHED.getCode());
		return traversal.hasNext();
	}

	@Override
	public Set<Tuple<String, Type>> getReleaseTypes() {
		Set<Tuple<String, Type>> typeSet = new HashSet<>();
		inE(HAS_FIELD_CONTAINER).frameExplicit(GraphFieldContainerEdgeImpl.class)
				.forEach(edge -> typeSet.add(Tuple.tuple(edge.getReleaseUuid(), edge.getType())));
		return typeSet;
	}

	@Override
	public Set<String> getReleases(Type type) {
		Set<String> releaseUuids = new HashSet<>();
		inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, type.getCode()).frameExplicit(GraphFieldContainerEdgeImpl.class)
				.forEach(edge -> releaseUuids.add(edge.getReleaseUuid()));
		return releaseUuids;
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
	public void addIndexBatchEntry(SearchQueueBatch batch, SearchQueueEntryAction action, String releaseUuid, Type type) {
		String indexType = NodeIndexHandler.getDocumentType(getSchemaContainerVersion());
		Node node = getParentNode();
		batch.addEntry(node.getUuid(), node.getType(), action, indexType,
				Arrays.asList(Tuple.tuple(NodeIndexHandler.CUSTOM_LANGUAGE_TAG, getLanguage().getLanguageTag()),
						Tuple.tuple(NodeIndexHandler.CUSTOM_RELEASE_UUID, releaseUuid),
						Tuple.tuple(NodeIndexHandler.CUSTOM_VERSION, type.toString().toLowerCase()),
						Tuple.tuple(NodeIndexHandler.CUSTOM_PROJECT_UUID, node.getProject().getUuid())));
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
				// Field exists in A and B and the fields are not equal to each other. 
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
		// Generate a structural diff first. This way it is easy to determine which fields have been added or removed.
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
		return outE(HAS_FIELD).has(MicronodeGraphFieldImpl.class).mark().inV().has(MicronodeImpl.class).out(HAS_MICROSCHEMA_CONTAINER)
				.has(MicroschemaContainerVersionImpl.class).has("uuid", version.getUuid()).back().toListExplicit(MicronodeGraphFieldImpl.class);
	}

	@Override
	public List<? extends MicronodeGraphFieldList> getMicronodeListFields(MicroschemaContainerVersion version) {
		return out(HAS_LIST).has(MicronodeGraphFieldListImpl.class).mark().out(HAS_ITEM).has(MicronodeImpl.class).out(HAS_MICROSCHEMA_CONTAINER)
				.has(MicroschemaContainerVersionImpl.class).has("uuid", version.getUuid()).back().toListExplicit(MicronodeGraphFieldListImpl.class);
	}
}
