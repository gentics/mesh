package com.gentics.mesh.core.data.node.field.impl.nesting;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

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
	public void transformToRest(InternalActionContext ac, String fieldKey, Handler<AsyncResult<Field>> handler) {
		// TODO handle null across all types
		//if (getNode() != null) {
		boolean expandField = ac.getExpandedFieldnames().contains(fieldKey) || ac.getExpandAllFlag();
		if (expandField) {
			// TODO, FIXME don't use countdown latch here
			CountDownLatch latch = new CountDownLatch(1);
			AtomicReference<NodeResponse> reference = new AtomicReference<>();
			getNode().transformToRest(ac, rh -> {
				reference.set(rh.result());
				latch.countDown();
			});
			try {
				latch.await(2, TimeUnit.SECONDS);
			} catch (Exception e) {
				e.printStackTrace();
			}
			handler.handle(Future.succeededFuture(reference.get()));
		} else {
			NodeFieldImpl nodeField = new NodeFieldImpl();
			nodeField.setUuid(getNode().getUuid());
			handler.handle(Future.succeededFuture(nodeField));	
		}
	}

}
