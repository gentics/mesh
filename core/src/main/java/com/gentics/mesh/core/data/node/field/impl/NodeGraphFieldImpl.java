package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.rest.common.ContainerType.forVersion;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.util.StreamUtil.toStream;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformer;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.util.CompareUtils;
import com.syncleus.ferma.VertexFrame;

public class NodeGraphFieldImpl extends MeshEdgeImpl implements NodeGraphField {

	public static FieldTransformer<NodeField> NODE_TRANSFORMER = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		NodeGraphField graphNodeField = container.getNode(fieldKey);
		if (graphNodeField == null) {
			return null;
		} else {
			// TODO check permissions
			return graphNodeField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	public static FieldUpdater NODE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NodeGraphField graphNodeField = container.getNode(fieldKey);
		NodeField nodeField = fieldMap.getNodeField(fieldKey);
		boolean isNodeFieldSetToNull = fieldMap.hasField(fieldKey) && (nodeField == null);
		GraphField.failOnDeletionOfRequiredField(graphNodeField, isNodeFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = nodeField == null;

		// Skip this check for no migrations
		if (!ac.isMigrationContext()) {
			GraphField.failOnMissingRequiredField(graphNodeField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);
		}

		// Handle Deletion - Remove the field if the field has been explicitly set to null
		if (graphNodeField != null && isNodeFieldSetToNull) {
			graphNodeField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Check whether the request contains all required information to execute it
		if (StringUtils.isEmpty(nodeField.getUuid())) {
			throw error(BAD_REQUEST, "node_error_field_property_missing", "uuid", fieldKey);
		}

		// Handle Update / Create
		BootstrapInitializer boot = MeshInternal.get().boot();
		Node node = boot.nodeRoot().findByUuid(nodeField.getUuid());
		if (node == null) {
			// TODO We want to delete the field when the field has been explicitly set to null
			if (log.isDebugEnabled()) {
				log.debug("Node field {" + fieldKey + "} could not be populated since node {" + nodeField.getUuid() + "} could not be found.");
			}
			// TODO we need to fail here - the node could not be found.
			// throw error(NOT_FOUND, "The field {, parameters)
		} else {
			// Check whether the container already contains a node field
			// TODO check node permissions
			// TODO check whether we want to allow cross project node references
			if (graphNodeField == null) {
				container.createNode(fieldKey, node);
			} else {
				// We can't update the graphNodeField since it is in fact an edge. 
				// We need to delete it and create a new one.
				container.deleteFieldEdge(fieldKey);
				container.createNode(fieldKey, node);
			}
		}
	};

	public static FieldGetter NODE_GETTER = (container, fieldSchema) -> {
		return container.getNode(fieldSchema.getName());
	};

	@Override
	public String getFieldKey() {
		return property(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public void setFieldKey(String key) {
		property(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public Stream<? extends NodeGraphFieldContainer> getReferencingContents() {
		return getReferencingFieldContainers()
			.flatMap(GraphFieldContainer::getContents);
	}

	private Stream<GraphFieldContainer> getReferencingFieldContainers() {
		if (label().equals(HAS_FIELD)) {
			return toStream(outV().frame(GraphFieldContainer.class));
		} else { // if HAS_ITEM
			return toStream(outV().in(HAS_LIST).frame(GraphFieldContainer.class));
		}
	}

	@Override
	public Node getNode() {
		return inV().nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public NodeField transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		// TODO handle null across all types
		// if (getNode() != null) {
		NodeParameters parameters = ac.getNodeParameters();
		boolean expandField = ac.getNodeParameters().getExpandedFieldnameList().contains(fieldKey) || parameters.getExpandAll();
		Node node = getNode();

		// Check whether the user is allowed to read the node reference
		boolean canReadNode = ac.getUser().canReadNode(ac, node);
		if (!canReadNode) {
			return null;
		}

		if (expandField && level < Node.MAX_TRANSFORMATION_LEVEL) {
			return node.transformToRestSync(ac, level, languageTags.toArray(new String[languageTags.size()]));
		} else {
			NodeFieldImpl nodeField = new NodeFieldImpl();
			nodeField.setUuid(node.getUuid());
			LinkType type = ac.getNodeParameters().getResolveLinks();
			if (type != LinkType.OFF) {

				WebRootLinkReplacer linkReplacer = MeshInternal.get().webRootLinkReplacer();
				Branch branch = ac.getBranch();
				ContainerType containerType = forVersion(ac.getVersioningParameters().getVersion());

				// Set the webroot path for the currently active language
				nodeField.setPath(linkReplacer.resolve(ac, branch.getUuid(), containerType, node, type, languageTags.toArray(new String[languageTags
						.size()])));

				// Set the languagePaths for all field containers
				Map<String, String> languagePaths = new HashMap<>();
				for (GraphFieldContainer currentFieldContainer : node.getGraphFieldContainers(branch, containerType)) {
					String currLanguage = currentFieldContainer.getLanguageTag();
					String languagePath = linkReplacer.resolve(ac, branch.getUuid(), containerType, node, type, currLanguage);
					languagePaths.put(currLanguage, languagePath);
				}
				nodeField.setLanguagePaths(languagePaths);

			}
			return nodeField;
		}
	}

	@Override
	public void removeField(BulkActionContext bac, GraphFieldContainer container) {
		// TODO BUG We must only remove one edge to the given container!
		remove();
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		NodeGraphFieldImpl field = getGraph().addFramedEdge(container, getNode(), HAS_FIELD, NodeGraphFieldImpl.class);
		field.setFieldKey(getFieldKey());
		return field;
	}

	@Override
	public void validate() {
	}

	@Override
	public String getFieldName() {
		ListSkipper skipper = new ListSkipper();

		if (skipper.nextVertex instanceof NodeGraphFieldContainer) {
			return skipper.getName();
		} else {
			return skipper.nextVertex.inE(HAS_FIELD, HAS_ITEM)
				.nextExplicit(NodeGraphFieldImpl.class)
				.getFieldName();
		}
	}

	@Override
	public Optional<String> getMicronodeFieldName() {
		ListSkipper skipper = new ListSkipper();

		if (skipper.nextVertex instanceof MicronodeImpl) {
			return Optional.of(skipper.getName());
		} else {
			return Optional.empty();
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeGraphField) {
			Node nodeA = getNode();
			Node nodeB = ((NodeGraphField) obj).getNode();
			return CompareUtils.equals(nodeA, nodeB);
		}
		if (obj instanceof NodeFieldListItem) {
			NodeFieldListItem restItem = (NodeFieldListItem) obj;
			// TODO assert key as well?
			// getNode can't be null since this is in fact an graph edge
			return CompareUtils.equals(restItem.getUuid(), getNode().getUuid());
		}
		if (obj instanceof NodeField) {
			NodeField nodeRestField = ((NodeField) obj);
			Node nodeA = getNode();
			String nodeUuid = nodeRestField.getUuid();
			// The node graph field is a edge so getNode should never be null. Lets check it anyways.
			if (nodeA != null) {
				return nodeA.getUuid().equals(nodeUuid);
			}
			// If both are null - both are equal
			if (nodeA == null && nodeRestField.getUuid() == null) {
				return true;
			}
		}
		return false;
	}

	private class ListSkipper {
		VertexFrame nextVertex;
		Supplier<String> nameSupplier;
		private ListSkipper() {
			VertexFrame framedVertex = outV().next();

			if (framedVertex instanceof ListGraphField) {
				nameSupplier = ((ListGraphField) framedVertex)::getFieldKey;
				nextVertex = framedVertex.in(HAS_LIST).next();
			} else {
				nextVertex = framedVertex;
				nameSupplier = NodeGraphFieldImpl.this::getFieldKey;
			}
		}

		public String getName() {
			return nameSupplier.get();
		}
	}
}
