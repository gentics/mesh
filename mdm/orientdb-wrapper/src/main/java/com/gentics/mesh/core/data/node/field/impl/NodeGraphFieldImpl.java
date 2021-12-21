package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ITEM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LIST;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.common.ContainerType.forVersion;
import static com.gentics.mesh.util.StreamUtil.toStream;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.HibFieldContainer;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.list.ListGraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.util.CompareUtils;
import com.syncleus.ferma.VertexFrame;

/**
 * @see NodeGraphField
 */
public class NodeGraphFieldImpl extends MeshEdgeImpl implements NodeGraphField {

	@Override
	public String getFieldKey() {
		return property(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public void setFieldKey(String key) {
		property(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public Stream<? extends HibNodeFieldContainer> getReferencingContents() {
		return getReferencingFieldContainers()
			.flatMap(GraphFieldContainer::getContents);
	}

	/**
	 * Creates a stream of all field containers that are connected to this edge.
	 * 
	 * @return
	 */
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
		UserDao userDao = mesh().boot().userDao();
		boolean expandField = ac.getNodeParameters().getExpandedFieldnameList().contains(fieldKey) || parameters.getExpandAll();
		Node node = getNode();

		// Check whether the user is allowed to read the node reference
		boolean canReadNode = userDao.canReadNode(ac.getUser(), ac, node);
		if (!canReadNode) {
			return null;
		}

		if (expandField && level < Node.MAX_TRANSFORMATION_LEVEL) {
			return mesh().boot().nodeDao().transformToRestSync(node, ac, level, languageTags.toArray(new String[languageTags.size()]));
		} else {
			NodeFieldImpl nodeField = new NodeFieldImpl();
			nodeField.setUuid(node.getUuid());
			LinkType type = ac.getNodeParameters().getResolveLinks();
			if (type != LinkType.OFF) {
				Tx tx = Tx.get();
				ContentDao contentDao = tx.contentDao();

				WebRootLinkReplacer linkReplacer = mesh().webRootLinkReplacer();
				HibBranch branch = tx.getBranch(ac);
				ContainerType containerType = forVersion(ac.getVersioningParameters().getVersion());

				// Set the webroot path for the currently active language
				nodeField.setPath(linkReplacer.resolve(ac, branch.getUuid(), containerType, node, type, languageTags.toArray(new String[languageTags
					.size()])));

				// Set the languagePaths for all field containers
				Map<String, String> languagePaths = new HashMap<>();
				for (HibFieldContainer currentFieldContainer : contentDao.getFieldContainers(node, branch, containerType)) {
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
	public void removeField(BulkActionContext bac, HibFieldContainer container) {
		// TODO BUG We must only remove one edge to the given container!
		remove();
	}

	@Override
	public GraphField cloneTo(HibFieldContainer container) {
		NodeGraphFieldImpl field = getGraph().addFramedEdge(toGraph(container), getNode(), HAS_FIELD, NodeGraphFieldImpl.class);
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
			HibNode nodeA = getNode();
			HibNode nodeB = ((NodeGraphField) obj).getNode();
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
			HibNode nodeA = getNode();
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

	/**
	 * Used in {@link #getFieldName()} and {@link #getMicronodeFieldName()} to skip lists and abstract the retrieval of the field name.
	 */
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
