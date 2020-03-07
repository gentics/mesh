package com.gentics.mesh.distributed.coordinator.proxy;

import javax.inject.Inject;

import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.coordinator.MasterElector;
import com.gentics.mesh.distributed.coordinator.MasterServer;

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

public class RequestDelegatorImpl implements RequestDelegator {

	private static final Logger log = LoggerFactory.getLogger(RequestDelegatorImpl.class);

	public static final String MESH_DIRECT_HEADER = "X-Mesh-Direct";

	private MasterElector elector;
	private HttpClient httpClient;

	@Inject
	public RequestDelegatorImpl(MasterElector elector, Vertx vertx) {
		this.elector = elector;
		this.httpClient = vertx.createHttpClient();
	}

	@Override
	public void handle(RoutingContext rc) {
		HttpServerRequest request = rc.request();
		HttpServerResponse response = rc.response();
		// client.request(method, requestURI);
		String requestURI = request.uri();

		if (request.getHeader(MESH_DIRECT_HEADER) != null) {
			log.info("Skipping delegator due to direct header");
			rc.next();
			return;
		}
		MasterServer master = elector.getMasterMember();
		if (master == null) {
			log.info("Skipping delegator since no master was elected.");
			rc.next();
			return;
		}
		// We don't need to delegate the request if we are the master
		if (master.isSelf()) {
			log.info("Skipping delegator since we are the master");
			rc.next();
			return;
		}
		String host = master.getHost();
		int port = master.getPort();

		log.info("Forwarding request to master {" + master.toString() + "}");
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

		forwardRequest.putHeader(MESH_DIRECT_HEADER, "true");

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
