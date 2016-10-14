package com.gentics.mesh.rest.client.handler;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClientHttpException;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract implementation for various mesh response handler.
 * 
 * @param <T>
 */
public abstract class AbstractMeshResponseHandler<T> implements MeshResponseHandler<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractMeshResponseHandler.class);

	protected String uri;
	protected HttpMethod method;
	protected MeshResponse<T> future;

	public AbstractMeshResponseHandler(HttpMethod method, String uri) {
		this.method = method;
		this.uri = uri;
		this.future = new MeshResponse<T>(Future.future());
	}

	@Override
	public String getUri() {
		return uri;
	}

	@Override
	public HttpMethod getMethod() {
		return method;
	}

	@Override
	public MeshResponse<T> getFuture() {
		return future;
	}

	@Override
	public void handle(HttpClientResponse response) {
		getFuture().setResponse(response);
		int code = response.statusCode();
		if (code >= 200 && code < 300) {
			handleSuccess(response);
		} else if (code == 304) {
			handleNotModified(response);
		} else {
			handleError(response);
		}
	}

	/**
	 * Success method which will be invoked for responses with codes &gt;=200 &amp;&amp; &gt; 300.
	 * 
	 * @param response
	 */
	public abstract void handleSuccess(HttpClientResponse response);

	/**
	 * Handles 304 responses.
	 * 
	 * @param response
	 */
	protected void handleNotModified(HttpClientResponse response) {
		future.complete(null);
	}

	/**
	 * Handler method which should be invoked for HTTP responses other than 2xx, 3xx status code.
	 * 
	 * @param response
	 */
	protected void handleError(HttpClientResponse response) {
		response.bodyHandler(bh -> {
			String body = bh.toString();
			if (log.isDebugEnabled()) {
				log.debug(body);
			}

			log.error("Request failed with statusCode {" + response.statusCode() + "} statusMessage {" + response.statusMessage() + "} {" + body
					+ "} for method {" + getMethod() + "} and uri {" + getUri() + "}");

			// Try to parse the body data and fail using the extracted exception.
			try {
				GenericMessageResponse responseMessage = JsonUtil.readValue(body, GenericMessageResponse.class);
				future.fail(new MeshRestClientHttpException(response.statusCode(), response.statusMessage(), responseMessage));
				return;
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("Could not deserialize response {" + body + "}.", e);
				}
			}

			future.fail(new MeshRestClientHttpException(response.statusCode(), response.statusMessage()));
			return;
		});

	}
}
