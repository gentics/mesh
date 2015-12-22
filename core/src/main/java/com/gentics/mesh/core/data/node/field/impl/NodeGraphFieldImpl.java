package com.gentics.mesh.core.data.node.field.impl;

import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.link.WebRootLinkReplacer;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.handler.InternalActionContext;

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
	public Observable<? extends Field> transformToRest(InternalActionContext ac, String fieldKey) {
		// TODO handle null across all types
		//if (getNode() != null) {
		boolean expandField = ac.getExpandedFieldnames().contains(fieldKey) || ac.getExpandAllFlag();
		if (expandField) {
			return getNode().transformToRest(ac);
		} else {
			NodeFieldImpl nodeField = new NodeFieldImpl();
			Node node = getNode();
			nodeField.setUuid(node.getUuid());
			if (ac.getResolveLinksType() != WebRootLinkReplacer.Type.OFF) {
				// TODO what about the language?
				nodeField.setUrl(WebRootLinkReplacer.getInstance()
						.resolve(node.getUuid(), null, ac.getResolveLinksType()).toBlocking().first());
			}
			return Observable.just(nodeField);	
		}
	}

}
