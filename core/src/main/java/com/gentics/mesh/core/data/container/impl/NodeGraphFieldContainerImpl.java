package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_EDITOR;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.error.Errors.nodeConflict;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.node.field.DisplayField;
import org.apache.commons.collections.CollectionUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
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
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.graphdb.spi.FieldType;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.Tuple;
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

	public static void init(Database database) {
		database.addVertexType(NodeGraphFieldContainerImpl.class, MeshVertexImpl.class);
		// Webroot index:
		database.addVertexIndex(WEBROOT_INDEX_NAME, NodeGraphFieldContainerImpl.class, true, WEBROOT_PROPERTY_KEY, FieldType.STRING);
		database.addVertexIndex(PUBLISHED_WEBROOT_INDEX_NAME, NodeGraphFieldContainerImpl.class, true, PUBLISHED_WEBROOT_PROPERTY_KEY,
				FieldType.STRING);
		// Webroot url field index:
		database.addVertexIndex(WEBROOT_URLFIELD_INDEX_NAME, NodeGraphFieldContainerImpl.class, true, WEBROOT_URLFIELD_PROPERTY_KEY,
				FieldType.STRING_SET);
		database.addVertexIndex(PUBLISHED_WEBROOT_URLFIELD_INDEX_NAME, NodeGraphFieldContainerImpl.class, true,
				PUBLISHED_WEBROOT_URLFIELD_PROPERTY_KEY, FieldType.STRING_SET);
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
		return getProperty(DISPLAY_FIELD_PROPERTY_KEY);
	}

	@Override
	public void updateDisplayFieldValue() {
		// TODO use schema storage instead
		Schema schema = getSchemaContainerVersion().getSchema();
		String displayFieldName = schema.getDisplayField();
		FieldSchema fieldSchema = schema.getField(displayFieldName);
		GraphField field = getField(fieldSchema);
		if (field != null && field instanceof DisplayField) {
			DisplayField displayField = (DisplayField) field;
			setProperty(DISPLAY_FIELD_PROPERTY_KEY, displayField.getDisplayName());
		}
	}

	@Override
	public void delete(SearchQueueBatch batch) {
		// TODO delete linked aggregation nodes for node lists etc
		for (BinaryGraphField binaryField : outE(HAS_FIELD).frame(BinaryGraphFieldImpl.class)) {
			binaryField.removeField(this);
		}

		// Lists
		// for (NumberGraphFieldList list : out(HAS_FIELD).frame(NumberGraphFieldListImpl.class)) {
		// list.removeField(this);
		// }
		// for (DateGraphFieldList list : out(HAS_FIELD).frame(DateGraphFieldListImpl.class)) {
		// list.removeField(this);
		// }
		// for (BooleanGraphFieldList list : out(HAS_FIELD).frame(BooleanGraphFieldListImpl.class)) {
		// list.removeField(this);
		// }
		// for (HtmlGraphFieldList list : out(HAS_FIELD).frame(HtmlGraphFieldListImpl.class)) {
		// list.removeField(this);
		// }
		// for (StringGraphFieldList list : out(HAS_FIELD).frame(StringGraphFieldListImpl.class)) {
		// list.removeField(this);
		// }
		// for (NodeGraphFieldList list : out(HAS_FIELD).frame(NodeGraphFieldListImpl.class)) {
		// list.removeField(this);
		// }

		// We don't need to handle node fields since those are only edges and will automatically be removed

		// Recursively delete all versions of the container
		NodeGraphFieldContainer next = getNextVersion();
		if (next != null) {
			next.delete(batch);
		}

		// Delete the container from all releases and types
		getReleaseTypes().forEach(tuple -> {
			String releaseUuid = tuple.v1();
			ContainerType type = tuple.v2();
			if (type != ContainerType.INITIAL) {
				batch.delete(this, releaseUuid, type, false);
			}
		});

		getElement().remove();
	}

	@Override
	@SuppressWarnings("unchecked")
	public void deleteFromRelease(Release release, SearchQueueBatch batch) {
		String releaseUuid = release.getUuid();

		batch.delete(this, releaseUuid, DRAFT, false);
		if (isPublished(releaseUuid)) {
			batch.delete(this, releaseUuid, PUBLISHED, false);
			setProperty(PUBLISHED_WEBROOT_PROPERTY_KEY, null);
		}
		// Remove the edge between the node and the container that matches the release
		inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).or(e -> e.traversal().has(
				GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode()), e -> e.traversal().has(
						GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.PUBLISHED.getCode())).removeAll();
		// remove webroot property
		setProperty(WEBROOT_PROPERTY_KEY, null);
	}

	@Override
	public void updateFieldsFromRest(InternalActionContext ac, FieldMap restFields) {
		super.updateFieldsFromRest(ac, restFields);
		String releaseUuid = ac.getRelease().getUuid();

		updateWebrootPathInfo(releaseUuid, "node_conflicting_segmentfield_update");
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
	 * @param releaseUuid
	 * @param urlFieldValues
	 * @param propertyName
	 * @param indexName
	 */
	private void updateWebrootUrlFieldsInfo(String releaseUuid, Set<String> urlFieldValues, String propertyName, String indexName) {
		if (urlFieldValues != null && !urlFieldValues.isEmpty()) {
			// Prefix each path with the releaseuuid in order to scope the paths by release
			Set<String> prefixedUrlFieldValues = urlFieldValues.stream().map(e -> releaseUuid + e).collect(Collectors.toSet());

			// Individually check each url
			for (String urlFieldValue : prefixedUrlFieldValues) {
				NodeGraphFieldContainer conflictingContainer = MeshInternal.get().database().checkIndexUniqueness(indexName, this, urlFieldValue);
				if (conflictingContainer != null) {
					if (log.isDebugEnabled()) {
						log.debug("Found conflicting container with uuid {" + conflictingContainer.getUuid() + "}");
					}
					// We know that the found container already occupies the index with one of the given paths. Lets compare both sets of paths in order to
					// determine
					// which path caused the conflict.
					Set<String> fromConflictingContainer = conflictingContainer.getUrlFieldValues();
					Node conflictingNode = conflictingContainer.getParentNode();

					@SuppressWarnings("unchecked")
					Collection<String> conflictingValues = CollectionUtils.intersection(fromConflictingContainer, urlFieldValues);
					String paths = conflictingValues.stream().map(n -> n.toString()).collect(Collectors.joining(","));

					throw nodeConflict(conflictingNode.getUuid(), conflictingContainer.getDisplayFieldValue(), conflictingContainer.getLanguage()
							.getLanguageTag(), "node_conflicting_urlfield_update", paths, conflictingContainer.getParentNode().getUuid(),
							conflictingContainer.getLanguage().getLanguageTag());
				}
			}
			setProperty(propertyName, prefixedUrlFieldValues);
		} else {
			setProperty(propertyName, null);
		}

	}

	@Override
	public void updateWebrootPathInfo(String releaseUuid, String conflictI18n) {
		Set<String> urlFieldValues = getUrlFieldValues();
		if (isDraft(releaseUuid)) {
			updateWebrootPathInfo(releaseUuid, conflictI18n, ContainerType.DRAFT, WEBROOT_PROPERTY_KEY, WEBROOT_INDEX_NAME);
			updateWebrootUrlFieldsInfo(releaseUuid, urlFieldValues, WEBROOT_URLFIELD_PROPERTY_KEY, WEBROOT_URLFIELD_INDEX_NAME);
		} else {
			setProperty(WEBROOT_PROPERTY_KEY, null);
			setProperty(WEBROOT_URLFIELD_PROPERTY_KEY, null);
		}
		if (isPublished(releaseUuid)) {
			updateWebrootPathInfo(releaseUuid, conflictI18n, ContainerType.PUBLISHED, PUBLISHED_WEBROOT_PROPERTY_KEY, PUBLISHED_WEBROOT_INDEX_NAME);
			updateWebrootUrlFieldsInfo(releaseUuid, urlFieldValues, PUBLISHED_WEBROOT_URLFIELD_PROPERTY_KEY, PUBLISHED_WEBROOT_URLFIELD_INDEX_NAME);
		} else {
			setProperty(PUBLISHED_WEBROOT_PROPERTY_KEY, null);
			setProperty(PUBLISHED_WEBROOT_URLFIELD_PROPERTY_KEY, null);
		}
	}

	/**
	 * Update the webroot path info (checking for uniqueness before)
	 *
	 * @param releaseUuid
	 *            release Uuid
	 * @param conflictI18n
	 *            i18n for the message in case of conflict
	 * @param type
	 *            edge type
	 * @param propertyName
	 *            name of the property
	 * @param indexName
	 *            name of the index to check for uniqueness
	 */
	protected void updateWebrootPathInfo(String releaseUuid, String conflictI18n, ContainerType type, String propertyName, String indexName) {
		Node node = getParentNode();
		String segmentFieldName = getSchemaContainerVersion().getSchema().getSegmentField();
		// Determine the webroot path of the container parent node
		String segment = node.getPathSegment(releaseUuid, type, getLanguage().getLanguageTag());

		// The webroot uniqueness will be checked by validating that the string [segmentValue-releaseUuid-parentNodeUuid] is only listed once within the given
		// specific index for (drafts or published nodes)
		if (segment != null) {
			StringBuilder webRootInfo = new StringBuilder(segment);
			webRootInfo.append("-").append(releaseUuid);
			Node parent = node.getParentNode(releaseUuid);
			if (parent != null) {
				webRootInfo.append("-").append(parent.getUuid());
			}

			// check for uniqueness of webroot path
			NodeGraphFieldContainerImpl conflictingContainer = MeshInternal.get().database().checkIndexUniqueness(indexName, this, webRootInfo
					.toString());
			if (conflictingContainer != null) {
				if (log.isDebugEnabled()) {
					log.debug("Found conflicting container with uuid {" + conflictingContainer.getUuid() + "} using index {" + indexName + "}");
				}
				Node conflictingNode = conflictingContainer.getParentNode();
				throw nodeConflict(conflictingNode.getUuid(), conflictingContainer.getDisplayFieldValue(), conflictingContainer.getLanguage()
						.getLanguageTag(), conflictI18n, segmentFieldName, segment);
			} else {
				setProperty(propertyName, webRootInfo.toString());
			}
		} else {
			setProperty(propertyName, null);
		}
	}

	@Override
	public Node getParentNode(String uuid) {
		return inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode()).has(
				GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, uuid).nextOrDefaultExplicit(NodeImpl.class, null);
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
		setSingleLinkOutTo(container, HAS_VERSION);
	}

	@Override
	public NodeGraphFieldContainer getPreviousVersion() {
		return in(HAS_VERSION).has(NodeGraphFieldContainerImpl.class).nextOrDefaultExplicit(NodeGraphFieldContainerImpl.class, null);
	}

	@Override
	public NodeGraphFieldContainer findVersion(String version) {
		if (getVersion().toString().equals(version)) {
			return this;
		}
		NodeGraphFieldContainer container = this;
		while (container != null) {
			container = container.getNextVersion();
			if (container != null && container.getVersion().toString().equals(version)) {
				return container;
			}
		}

		container = this;
		while (container != null) {
			container = container.getPreviousVersion();
			if (container != null && container.getVersion().toString().equals(version)) {
				return container;
			}
		}
		return null;
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
		EdgeTraversal<?, ?, ?> traversal = inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).has(
				GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.DRAFT.getCode());
		return traversal.hasNext();
	}

	@Override
	public boolean isPublished(String releaseUuid) {
		EdgeTraversal<?, ?, ?> traversal = inE(HAS_FIELD_CONTAINER).has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid).has(
				GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, ContainerType.PUBLISHED.getCode());
		return traversal.hasNext();
	}

	@Override
	public Set<Tuple<String, ContainerType>> getReleaseTypes() {
		Set<Tuple<String, ContainerType>> typeSet = new HashSet<>();
		inE(HAS_FIELD_CONTAINER).frameExplicit(GraphFieldContainerEdgeImpl.class).forEach(edge -> typeSet.add(Tuple.tuple(edge.getReleaseUuid(), edge
				.getType())));
		return typeSet;
	}

	@Override
	public Set<String> getReleases(ContainerType type) {
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
		return ETag.hash(getUuid());
	}

	@Override
	public User getEditor() {
		return out(HAS_EDITOR).nextOrDefaultExplicit(UserImpl.class, null);
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

	public com.gentics.mesh.path.Path getPath(InternalActionContext ac) {
		Path nodePath = new Path();
		nodePath.addSegment(new PathSegment(this, null, getLanguage().getLanguageTag()));
		return nodePath;
	}

}
