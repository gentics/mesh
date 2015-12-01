package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.impl.nesting.NodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.RxUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;

public class NodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<NodeGraphField, NodeFieldList>implements NodeGraphFieldList {

	public static void checkIndices(Database database) {
		database.addVertexType(NodeGraphFieldListImpl.class);
	}

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
	public void transformToRest(InternalActionContext ac, String fieldKey, Handler<AsyncResult<NodeFieldList>> handler) {

		// Check whether the list should be returned in a collapsed or expanded format
		boolean expandField = ac.getExpandedFieldnames().contains(fieldKey) || ac.getExpandAllFlag();
		if (expandField) {
			NodeFieldList restModel = new NodeFieldListImpl();

			List<ObservableFuture<NodeResponse>> futures = new ArrayList<>();
			for (com.gentics.mesh.core.data.node.field.nesting.NodeGraphField item : getList()) {
				ObservableFuture<NodeResponse> obsItemTransformed = RxHelper.observableFuture();
				futures.add(obsItemTransformed);
				item.getNode().transformToRest(ac, rh -> {
					if (rh.failed()) {
						obsItemTransformed.toHandler().handle(Future.failedFuture(rh.cause()));
					} else {
						obsItemTransformed.toHandler().handle(Future.succeededFuture(rh.result()));
					}
				});
			}

			RxUtil.concatList(futures).collect(() -> {
				return restModel.getItems();
			} , (x, y) -> {
				x.add(y);
			}).subscribe(list -> {
				handler.handle(Future.succeededFuture(restModel));
			} , error -> {
				handler.handle(Future.failedFuture(error));
			});

		} else {
			NodeFieldList restModel = new NodeFieldListImpl();
			for (com.gentics.mesh.core.data.node.field.nesting.NodeGraphField item : getList()) {
				// Create the rest field and populate the fields
				NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(item.getNode().getUuid());
				restModel.add(listItem);
			}
			handler.handle(Future.succeededFuture(restModel));

		}

	}

}
