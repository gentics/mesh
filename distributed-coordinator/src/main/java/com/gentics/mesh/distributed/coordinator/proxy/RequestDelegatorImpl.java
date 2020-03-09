package com.gentics.mesh.distributed.coordinator.proxy;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.distributed.coordinator.Coordinator;
import com.gentics.mesh.distributed.coordinator.MasterServer;
import com.gentics.mesh.etc.config.cluster.CoordinatorMode;

import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
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

	private final Coordinator coordinator;
	private final HttpClient httpClient;
	private static final Set<Pattern> readOnlyPathPatternSet = createReadOnlyPatternSet();
	private static final Set<Pattern> whiteListPathPatternSet = createWhitelistPatternSet();

	@Inject
	public RequestDelegatorImpl(Coordinator coordinator, Vertx vertx) {
		this.coordinator = coordinator;
		this.httpClient = vertx.createHttpClient();
	}

	@Override
	public void handle(RoutingContext rc) {
		HttpServerRequest request = rc.request();
		HttpServerResponse response = rc.response();
		String requestURI = request.uri();
		String path = request.path();
		HttpMethod method = request.method();
		CoordinatorMode mode = coordinator.getCoordinatorMode();
		if (mode == CoordinatorMode.DISABLED) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping delegation since coordination is disabled.");
			}
			rc.next();
			return;
		}

		if (isWhitelisted(path)) {
			if (log.isDebugEnabled()) {
				log.debug("URI {" + requestURI + "} with method {" + method.name() + "} is whitelisted. Skipping delegation");
			}
			rc.next();
			return;
		}

		String headerValue = request.getHeader(MESH_DIRECT_HEADER);
		if (headerValue != null && headerValue.equalsIgnoreCase("true")) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping delegator due to direct header");
			}
			rc.next();
			return;
		}

		// In CUD mode we only delegate mutating requests to the master
		if (mode == CoordinatorMode.CUD) {
			if (isReadRequest(method, path)) {
				rc.next();
				return;
			}
		}

		MasterServer master = coordinator.getMasterMember();
		if (master == null) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping delegator since no master was elected.");
			}
			rc.next();
			return;
		}
		// We don't need to delegate the request if we are the master
		if (master.isSelf()) {
			if (log.isDebugEnabled()) {
				log.debug("Skipping delegator since we are the master");
			}
			rc.next();
			return;
		}
		String host = master.getHost();
		int port = master.getPort();

		if (log.isDebugEnabled()) {
			log.debug("Forwarding request to master {" + master.toString() + "}");
		}

		@SuppressWarnings("deprecation")
		HttpClientRequest forwardRequest = httpClient.request(method, port, host, requestURI, forwardResponse -> {
			response.setChunked(true);
			response.setStatusCode(forwardResponse.statusCode());
			forwardHeaders(response, forwardResponse);
			printHeaders("Forward response headers", response.headers());
			Pump.pump(forwardResponse, response)
				.setWriteQueueMaxSize(8192)
				.start();
			forwardResponse.endHandler(v -> response.end());
		});

		forwardHeaders(request, forwardRequest);
		forwardRequest.putHeader(MESH_DIRECT_HEADER, "true");
		forwardRequest.setChunked(true);

		if (request.isEnded()) {
			log.warn("Request to be proxied is already read");
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
	 * Log the given messages with loglevel <code>TRACE</code>.
	 *
	 * @param label
	 *            A message that will be logged before the headers
	 * @param headers
	 *            The HTTP headers to log
	 */
	private void printHeaders(String label, MultiMap headers) {
		if (log.isTraceEnabled()) {
			log.trace(label + " ({})", headers.size());
			headers.forEach(header -> log.trace("  {}: {}", header.getKey(), header.getValue()));
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
		if (body == null) {
			forwardRequest.end();
		} else {
			forwardRequest.end(body);
		}
	}

	private void forwardHeaders(HttpServerResponse response, HttpClientResponse forwardResponse) {
		MultiMap headers = forwardResponse.headers();

		for (String headerName : headers.names()) {
			response.putHeader(headerName, headers.getAll(headerName));
		}
	}

	private void forwardHeaders(HttpServerRequest request, HttpClientRequest forwardRequest) {
		MultiMap headers = request.headers();

		for (String headerName : headers.names()) {
			forwardRequest.putHeader(headerName, headers.getAll(headerName));
		}

	}

	/**
	 * Check whether the request is a read request.
	 * 
	 * @param method
	 * @param path
	 * @return
	 */
	private boolean isReadRequest(HttpMethod method, String path) {
		switch (method) {
		case CONNECT:
		case OPTIONS:
		case GET:
			return true;
		case DELETE:
		case PATCH:
		case PUT:
			return false;
		case POST:
			// Lets check whether the request is actually a read request.
			// In this case we don't need to delegate it.
			return isReadOnly(path);
		default:
			log.debug("Unhandled methd {" + method + "} in path {" + path + "}");
			return false;
		}
	}

	public static boolean isWhitelisted(String path) {
		for (Pattern pattern : whiteListPathPatternSet) {
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}

	public static boolean isReadOnly(String path) {
		for (Pattern pattern : readOnlyPathPatternSet) {
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}

	private static Set<Pattern> createWhitelistPatternSet() {
		Set<Pattern> patterns = new HashSet<>();
		patterns.add(Pattern.compile("/api/v[0-9]*/?"));
		patterns.add(Pattern.compile("/api/v[0-9]*/admin.*"));
		return patterns;
	}

	private static Set<Pattern> createReadOnlyPatternSet() {
		Set<Pattern> patterns = new HashSet<>();
		patterns.add(Pattern.compile("/api/v.*/auth/login"));
		patterns.add(Pattern.compile("/api/v.*/graphql/?"));
		patterns.add(Pattern.compile("/api/v.*/search/?"));
		patterns.add(Pattern.compile("/api/v.*/rawSearch/?"));
		return patterns;
	}

}
