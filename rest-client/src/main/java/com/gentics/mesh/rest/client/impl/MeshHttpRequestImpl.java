package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.handler.MeshResponseHandler;

import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MeshHttpRequestImpl<T> implements MeshRequest<T> {

	private static final Logger log = LoggerFactory.getLogger(MeshHttpRequestImpl.class);

	private HttpClientRequest request;

	private MeshResponseHandler<T> handler;

	public MeshHttpRequestImpl(HttpClientRequest request, MeshResponseHandler<T> handler) {
		this.request = request;
		this.handler = handler;
	}

	public HttpClientRequest getRequest() {
		return request;
	}

	public MeshResponse<T> invoke() {
		if (log.isDebugEnabled()) {
			log.debug("Invoking request to {" + handler.getUri() + "}");
		}
		request.end();
		return handler.getFuture();
	}

}
