package com.gentics.mesh.core.data.node.field.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.GraphFieldContainerEdge.Type;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.util.CompareUtils;

import rx.Observable;

public class NodeGraphFieldImpl extends MeshEdgeImpl implements NodeGraphField {

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public Node getNode() {
		return inV().has(NodeImpl.class).nextOrDefaultExplicit(NodeImpl.class, null);
	}

	@Override
	public Observable<? extends Field> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level) {
		// TODO handle null across all types
		//if (getNode() != null) {
		boolean expandField = ac.getExpandedFieldnames().contains(fieldKey) || ac.getExpandAllFlag();
		if (expandField && level < Node.MAX_TRANSFORMATION_LEVEL) {
			return getNode().transformToRestSync(ac, level, languageTags.toArray(new String[languageTags.size()]));
		} else {
			NodeFieldImpl nodeField = new NodeFieldImpl();
			Node node = getNode();
			nodeField.setUuid(node.getUuid());
			if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {
				nodeField.setPath(WebRootLinkReplacer.getInstance()
						.resolve(ac.getRelease(null).getUuid(), Type.forVersion(ac.getVersion()), node,
								ac.getResolveLinksType(), languageTags.toArray(new String[languageTags.size()]))
						.toBlocking().first());
			}
			return Observable.just(nodeField);
		}
	}

	@Override
	public void removeField(GraphFieldContainer container) {
		remove();
	}

	@Override
	public GraphField cloneTo(GraphFieldContainer container) {
		NodeGraphFieldImpl field = getGraph().addFramedEdge(container.getImpl(), getNode().getImpl(), HAS_FIELD, NodeGraphFieldImpl.class);
		field.setFieldKey(getFieldKey());
		return field;
	}

	@Override
	public void validate() {
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeGraphField) {
			Node nodeA = getNode();
			Node nodeB = ((NodeGraphField) obj).getNode();
			return CompareUtils.equals(nodeA, nodeB);
		}
		if (obj instanceof NodeFieldListItem) {
			NodeFieldListItem restItem = (NodeFieldListItem)obj;
			//TODO assert key as well?
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
}
