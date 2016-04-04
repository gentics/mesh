package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_CONTAINER_VERSION;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_VERSION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.GraphFieldContainerEdge.Type;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.VersionNumber;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.gentics.mesh.search.index.NodeIndexHandler;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeGraphFieldContainerImpl extends AbstractGraphFieldContainerImpl implements NodeGraphFieldContainer {

	public static final String WEBROOT_PROPERTY_KEY = "webrootPathInfo";

	public static final String WEBROOT_INDEX_NAME = "webrootPathInfoIndex";

	public static final String VERSION_PROPERTY_KEY = "version";

	private static final Logger log = LoggerFactory.getLogger(NodeGraphFieldContainerImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(NodeGraphFieldContainerImpl.class);
		database.addVertexIndex(WEBROOT_INDEX_NAME, NodeGraphFieldContainerImpl.class, true, WEBROOT_PROPERTY_KEY);
	}

	@Override
	public void setSchemaContainerVersion(SchemaContainerVersion schema) {
		setSingleLinkOutTo(schema.getImpl(), HAS_SCHEMA_CONTAINER_VERSION);
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
	public void delete() {
		// TODO delete linked aggregation nodes for node lists etc
		getElement().remove();
	}

	@Override
	public void updateFieldsFromRest(InternalActionContext ac, FieldMap restFields, FieldSchemaContainer schema) {
		super.updateFieldsFromRest(ac, restFields, schema);

		String segmentFieldName = getSchemaContainerVersion().getSchema().getSegmentField();
		if (restFields.containsKey(segmentFieldName)) {
			updateWebrootPathInfo(ac.getRelease().getUuid(), "node_conflicting_segmentfield_update");
		}
	}

	@Override
	public void updateWebrootPathInfo(String releaseUuid, String conflictI18n) {
		Node node = getParentNode();
		String segmentFieldName = getSchemaContainerVersion().getSchema().getSegmentField();
		String segment = node.getPathSegment(releaseUuid, Type.DRAFT, getLanguage().getLanguageTag()).toBlocking().last();
		if (segment != null) {
			StringBuilder webRootInfo = new StringBuilder(segment);
			Node parent = node.getParentNode();
			if (parent != null) {
				webRootInfo.append("-").append(parent.getUuid());
			}

			// check for uniqueness of webroot path
			NodeGraphFieldContainerImpl conflictingContainer = MeshSpringConfiguration.getInstance().database()
					.checkIndexUniqueness(WEBROOT_INDEX_NAME, this, webRootInfo.toString());
			if (conflictingContainer != null) {
				Node conflictingNode = conflictingContainer.getParentNode();
				throw conflict(conflictingNode.getUuid(), conflictingContainer.getDisplayFieldValue(), conflictI18n, segmentFieldName, segment);
			}

			setProperty(WEBROOT_PROPERTY_KEY, webRootInfo.toString());
		} else {
			setProperty(WEBROOT_PROPERTY_KEY, null);
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
		List<GraphField> otherFields = container.getFields(container.getSchemaContainerVersion().getSchema());

		for (GraphField graphField : otherFields) {
			graphField.cloneTo(this);
		}
	}

	@Override
	public boolean isPublished(String releaseUuid) {
		EdgeTraversal<?, ?, ?> traversal = inE(HAS_FIELD_CONTAINER)
				.has(GraphFieldContainerEdgeImpl.RELEASE_UUID_KEY, releaseUuid)
				.has(GraphFieldContainerEdgeImpl.EDGE_TYPE_KEY, Type.PUBLISHED.getCode());
		return traversal.hasNext();
	}

	@Override
	public void validate() {
		Schema schema = getSchemaContainerVersion().getSchema();
		Map<String, GraphField> fieldsMap = getFields(schema).stream()
				.collect(Collectors.toMap(GraphField::getFieldKey, Function.identity()));

		schema.getFields().stream().forEach(fieldSchema -> {
			GraphField field = fieldsMap.get(fieldSchema.getName());
			if (fieldSchema.isRequired() && field == null) {
				throw error(CONFLICT, "node_error_missing_mandatory_field_value", fieldSchema.getName(),
						schema.getName());
			}
			if (field != null) {
				field.validate();
			}
		});
	}

	@Override
	public void addIndexBatchEntry(SearchQueueBatch batch, SearchQueueEntryAction action) {
		String indexType = NodeIndexHandler.getDocumentType(getSchemaContainerVersion());
		batch.addEntry(getParentNode().getUuid() + "-" + getLanguage().getLanguageTag(), getParentNode().getType(), action, indexType);
	}

}
