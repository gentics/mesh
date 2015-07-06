package com.gentics.mesh.rest;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.gentics.mesh.core.rest.common.AbstractRestModel;
import com.gentics.mesh.json.JsonUtil;

public class MeshResponseHandler<T extends AbstractRestModel> implements Handler<HttpClientResponse> {

	private static final Logger log = LoggerFactory.getLogger(MeshResponseHandler.class);

	private Future<T> future;
	private Class<T> classOfT;
	private Handler<HttpClientResponse> handler;

	public MeshResponseHandler(Class<T> classOfT) {
		this.classOfT = classOfT;
		this.future = Future.future();
	}

	@Override
	public void handle(HttpClientResponse response) {

		if (response.statusCode() == 200) {
			response.bodyHandler(bh -> {
				String json = bh.toString();
				try {
					T restObj = JsonUtil.readValue(json, classOfT);
					future.complete(restObj);
				} catch (Exception e) {
					log.error("Failed to deserialize json to class {" + classOfT + "}", e);
					future.fail(e);
				}
			});
		} else {
			response.bodyHandler(bh -> {
				log.error("Request failed {" + bh.toString() + "}");
				// TODO try to unserialize using GenericMessageReponse class and return nested message?
				future.fail(bh.toString());
			});
		}
		if (handler != null) {
			handler.handle(response);
		}

	}

	public Future<T> getFuture() {
		return future;
	}

	public void handle(Handler<HttpClientResponse> handler) {
		this.handler = handler;
	}

}
