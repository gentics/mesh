package com.gentics.mesh.distributed.coordinator.proxy;

import javax.inject.Inject;

import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.coordinator.MasterElector;
import com.gentics.mesh.distributed.coordinator.MasterServer;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;

import io.vertx.codegen.annotations.Nullable;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import io.vertx.ext.web.RoutingContext;

public class RequestDelegatorImpl implements RequestDelegator {

	private static final Logger log = LoggerFactory.getLogger(RequestDelegatorImpl.class);

	public static final String MESH_DIRECT_HEADER = "X-Mesh-Direct";

	private final MasterElector elector;
	private final HttpClient httpClient;
	private final ClusterOptions options;

	@Inject
	public RequestDelegatorImpl(MasterElector elector, Vertx vertx, MeshOptions options) {
		this.elector = elector;
		this.httpClient = vertx.createHttpClient();
		this.options = options.getClusterOptions();
	}

	@Override
	public void handle(RoutingContext rc) {
		HttpServerRequest request = rc.request();
		HttpServerResponse response = rc.response();
		String requestURI = request.uri();
		HttpMethod method = request.method();
		CoordinatorMode mode = options.getCoordinatorMode();

		String headerValue = request.getHeader(MESH_DIRECT_HEADER);
		if (headerValue != null && headerValue.equalsIgnoreCase("true")) {
			log.info("Skipping delegator due to direct header");
			rc.next();
			return;
		}

		// In Mode A we only delegate mutating requests to the master
		if (mode == CoordinatorMode.MODE_A) {
			switch (method) {
			case DELETE:
			case PATCH:
			case PUT:
				break;
			case POST:
				if (requestURI.contains("/graphql")) {
					break;
				} else if (requestURI.contains("/search")) {
					break;
				} else {
					rc.next();
					return;
				}
			default:
				rc.next();
				return;
			}
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
		HttpClientRequest forwardRequest = httpClient.request(method, port, host, requestURI, forwardResponse -> {

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
