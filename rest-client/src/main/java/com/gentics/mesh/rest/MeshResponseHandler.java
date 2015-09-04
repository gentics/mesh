package com.gentics.mesh.rest;

import org.apache.commons.lang.StringUtils;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;

public class MeshResponseHandler<T> implements Handler<HttpClientResponse> {

	private static final Logger log = LoggerFactory.getLogger(MeshResponseHandler.class);

	private Future<T> future;
	private Class<T> classOfT;
	private Handler<HttpClientResponse> handler;
	private AbstractMeshRestClient client;
	private HttpMethod method;
	private String uri;

	public MeshResponseHandler(Class<T> classOfT, AbstractMeshRestClient client, HttpMethod method, String uri) {
		this.classOfT = classOfT;
		this.client = client;
		this.future = Future.future();
		this.method = method;
		this.uri = uri;
	}

	@Override
	public void handle(HttpClientResponse response) {

		if (response.statusCode() == 200) {

			if (!StringUtils.isEmpty(response.headers().get("Set-Cookie"))) {
				client.setCookie(response.headers().get("Set-Cookie"));
			}

			response.bodyHandler(bh -> {
				String json = bh.toString();
				if (log.isDebugEnabled()) {
					log.debug(json);
				}
				try {
					if (isSchemaClass(classOfT)) {
						T restObj = JsonUtil.readSchema(json, classOfT);
						future.complete(restObj);
					} else if (isNodeClass(classOfT) || isNodeListClass(classOfT)) {
						T restObj = JsonUtil.readNode(json, classOfT, client.getClientSchemaStorage());
						future.complete(restObj);
					} else {
						T restObj = JsonUtil.readValue(json, classOfT);
						future.complete(restObj);
					}
				} catch (Exception e) {
					log.error("Failed to deserialize json to class {" + classOfT + "}", e);
					future.fail(e);
				}
			});
		} else {
			response.bodyHandler(bh -> {
				log.error("Request failed with statusCode {" + response.statusCode() + "} statusMessage {" + response.statusMessage() + "} {"
						+ bh.toString() + "} for method {" + this.method + "} and uri {" + this.uri + "}");

				GenericMessageResponse responseMessage = null;
				try {
					responseMessage = JsonUtil.readValue(bh.toString(), GenericMessageResponse.class);
				} catch (Exception e) {
					if (log.isDebugEnabled()) {
						log.debug("Could not deserialize response {" + bh.toString() + "}.", e);
					}
					responseMessage = new GenericMessageResponse(bh.toString());
				}
				future.fail(new MeshRestClientHttpException(response.statusCode(), response.statusMessage(), responseMessage));
			});
		}
		if (handler != null) {
			handler.handle(response);
		}

	}

	private boolean isNodeListClass(Class<?> clazz) {
		if (clazz.isAssignableFrom(NodeListResponse.class)) {
			return true;
		}
		return false;
	}

	private boolean isSchemaClass(Class<?> clazz) {
		if (clazz.isAssignableFrom(SchemaResponse.class) || clazz.isAssignableFrom(SchemaCreateRequest.class)
				|| clazz.isAssignableFrom(SchemaUpdateRequest.class) || clazz.isAssignableFrom(SchemaListResponse.class)) {
			return true;
		}
		return false;
	}

	private boolean isNodeClass(Class<?> clazz) {
		if (clazz.isAssignableFrom(NodeUpdateRequest.class) || clazz.isAssignableFrom(NodeResponse.class)
				|| clazz.isAssignableFrom(NodeCreateRequest.class)) {
			return true;
		}
		return false;
	}

	public Future<T> getFuture() {
		return future;
	}

	public void handle(Handler<HttpClientResponse> handler) {
		this.handler = handler;
	}

}
