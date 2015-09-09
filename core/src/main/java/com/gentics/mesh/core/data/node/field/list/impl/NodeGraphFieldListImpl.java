package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.nesting.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.handler.ActionContext;

public class NodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<NodeGraphField, NodeFieldList>implements NodeGraphFieldList {

	@Override
	public NodeGraphField createNode(String key, Node node) {
		return addItem(key, node);
	}

	@Override
	public Class<? extends NodeGraphField> getListType() {
		return NodeGraphFieldImpl.class;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub

	}

	@Override
	public NodeFieldList transformToRest(ActionContext ac, String fieldKey) {
		NodeFieldList restModel = new NodeFieldListImpl();
		boolean expandField = ac.getExpandedFieldnames().contains(fieldKey);
		for (com.gentics.mesh.core.data.node.field.nesting.NodeGraphField item : getList()) {
			if (expandField) {
				// TODO, FIXME get rid of the countdown latch
				CountDownLatch latch = new CountDownLatch(1);
				AtomicReference<NodeResponse> reference = new AtomicReference<>();
				item.getNode().transformToRest(ac, rh -> {
					reference.set(rh.result());
					latch.countDown();
				});
				try {
					latch.await(2, TimeUnit.SECONDS);
				} catch (Exception e) {
					e.printStackTrace();
				}
				restModel.add(reference.get());
			} else {
				// Create the rest field and populate the fields
				NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(item.getNode().getUuid());
				restModel.add(listItem);
			}
		}
		return restModel;
	}

}
