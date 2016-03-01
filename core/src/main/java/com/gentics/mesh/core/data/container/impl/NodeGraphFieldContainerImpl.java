package com.gentics.mesh.core.data.container.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeGraphFieldContainerImpl extends AbstractGraphFieldContainerImpl implements NodeGraphFieldContainer {
	public static final String WEBROOT_PROPERTY_KEY = "webrootPathInfo";

	public static final String WEBROOT_INDEX_NAME = "webrootPathInfoIndex";

	private static final Logger log = LoggerFactory.getLogger(NodeGraphFieldContainerImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(NodeGraphFieldContainerImpl.class);
		database.addVertexIndex(WEBROOT_INDEX_NAME, NodeGraphFieldContainerImpl.class, true, WEBROOT_PROPERTY_KEY);
	}

	@Override
	public String getDisplayFieldValue(Schema schema) {
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
	public void updateFieldsFromRest(InternalActionContext ac, Map<String, Field> restFields,
			FieldSchemaContainer schema) {
		super.updateFieldsFromRest(ac, restFields, schema);

		Node node = getParentNode();
		String segmentFieldName = node.getSchemaContainer().getSchema().getSegmentField();
		if (restFields.containsKey(segmentFieldName)) {
			updateWebrootPathInfo("node_conflicting_segmentfield_update");
		}
	}

	@Override
	public void updateWebrootPathInfo(String conflictI18n) {
		Node node = getParentNode();
		String segmentFieldName = node.getSchemaContainer().getSchema().getSegmentField();
		String segment = node.getPathSegment(getLanguage().getLanguageTag()).toBlocking().last();
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
				throw conflict(conflictingContainer.getParentNode().getUuid(),
						conflictingContainer.getDisplayFieldValue(conflictingNode.getSchemaContainer().getSchema()), conflictI18n,
						segmentFieldName, segment);
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
}
