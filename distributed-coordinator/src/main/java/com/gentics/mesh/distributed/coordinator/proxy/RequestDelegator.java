package com.gentics.mesh.distributed.coordinator.proxy;

import javax.inject.Inject;

import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.distributed.coordinator.MasterServer;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.RoutingContext;

public class RequestDelegator implements Handler<RoutingContext> {

	private static final Logger log = LoggerFactory.getLogger(RequestDelegator.class);

	private Coordinator coordinator;
	private Vertx vertx;
	private HttpClient httpClient;

	@Inject
	public RequestDelegator(Coordinator coordinator, Vertx vertx) {
		this.coordinator = coordinator;
		this.vertx = vertx;
		this.httpClient = vertx.createHttpClient();
	}

	@Override
	public void handle(RoutingContext rc) {
		HttpServerRequest request = rc.request();
		HttpServerResponse response = rc.response();
		// client.request(method, requestURI);
		String requestURI = request.uri();

		MasterServer master = coordinator.getElectedMaster();
		String host = master.getHost();
		int port = master.getPort();

		@SuppressWarnings("deprecation")
		HttpClientRequest forwardRequest = httpClient.request(request.method(), port, host, requestURI, forwardResponse -> {

			response.setChunked(true);
			response.setStatusCode(forwardResponse.statusCode());
			// forwardHeaders(response, forwardResponse);
			// printHeaders("Forward response headers", response.headers());
			Pump.pump(forwardResponse, response)
				.setWriteQueueMaxSize(8192)
				.start();
			forwardResponse.endHandler(v -> response.end());
		});

		if (request.isEnded()) {
			log.warn("Request to be proxied is already read");
			log.warn("This suggests that the proxy handler does not use the BEFORE_BODY_HANDLER route order");

			proxyEndHandler(forwardRequest, rc.getBody());
		} else {
			request.exceptionHandler(e -> log.error("Could not forward request to Mesh: {}", e, e.getMessage()))
				.endHandler(v -> proxyEndHandler(forwardRequest, null));
			Pump.pump(request, forwardRequest)
				.setWriteQueueMaxSize(8192)
				.start();
		}

	}

	/**
	 * End the given <code>forwardRequest</code> optionally with the provided body if it is not <code>null</code>.
	 *
	 * @param forwardRequest
	 *            The proxy request
	 * @param body
	 *            The optional body for the request (may by <code>null</code>)
	 */
	private void proxyEndHandler(HttpClientRequest forwardRequest, Buffer body) {
		// printHeaders("Forward request headers", forwardRequest.headers());

		if (body == null) {
			forwardRequest.end();
		} else {
			forwardRequest.end(body);
		}
	}

}
