package com.gentics.mesh.rest.client.handler;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface GenericMessageErrorHandler<T> extends ResponseHandler<T> {

	static final Logger log = LoggerFactory.getLogger(GenericMessageErrorHandler.class);

	/**
	 * Handler method which should be invoked for HTTP responses other than 2xx, 3xx status code.
	 * 
	 * @param response
	 */
	default void handleError(HttpClientResponse response) {
		response.bodyHandler(bh -> {
			String body = bh.toString();
			if (log.isDebugEnabled()) {
				log.debug(body);
			}

			if (log.isDebugEnabled()) {
				log.debug("Request failed with statusCode {" + response.statusCode() + "} statusMessage {" + response.statusMessage() + "} {" + body
						+ "} for method {" + getMethod() + "} and uri {" + getUri() + "}");
			}

			// Try to parse the body data and fail using the extracted exception.
			try {
				GenericMessageResponse responseMessage = JsonUtil.readValue(body, GenericMessageResponse.class);
				getFuture().fail(new MeshRestClientMessageException(response.statusCode(), response.statusMessage(), responseMessage));
				return;
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("Could not deserialize response {" + body + "}.", e);
				}
			}

			getFuture().fail(new MeshRestClientMessageException(response.statusCode(), response.statusMessage()));
			return;
		});
	}

}
