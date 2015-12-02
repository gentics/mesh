package com.gentics.mesh.core.data.node.field.impl.nesting;

import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class MicronodeGraphFieldImpl extends MeshEdgeImpl implements MicronodeGraphField {

	@Override
	public void setFieldKey(String key) {
		setProperty(GraphField.FIELD_KEY_PROPERTY_KEY, key);
	}

	@Override
	public String getFieldKey() {
		return getProperty(GraphField.FIELD_KEY_PROPERTY_KEY);
	}

	@Override
	public Micronode getMicronode() {
		return inV().has(MicronodeImpl.class).nextOrDefaultExplicit(MicronodeImpl.class, null);
	}

	@Override
	public void transformToRest(InternalActionContext ac, String fieldKey, Handler<AsyncResult<Field>> handler) {
		Micronode micronode = getMicronode();
		if (micronode == null) {
			// TODO is this correct?
			handler.handle(failedFuture(BAD_REQUEST, "error_name_must_be_set"));
		}
		if (micronode != null) {
			micronode.transformToRest(ac, rh -> {
				handler.handle(Future.succeededFuture(rh.result()));
			});
		}
	}

}
