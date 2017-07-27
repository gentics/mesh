package com.gentics.mesh.rest.client.handler;

import com.gentics.mesh.rest.client.MeshRestClientJsonObjectException;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface JsonObjectErrorHandler<T> extends ResponseHandler<T> {

	static final Logger log = LoggerFactory.getLogger(JsonObjectErrorHandler.class);

	default void handleError(HttpClientResponse response) {
		response.bodyHandler(bh -> {
			String body = bh.toString();
			if (log.isDebugEnabled()) {
				log.debug(body);
			}

			log.error("Request failed with statusCode {" + response.statusCode() + "} statusMessage {" + response.statusMessage() + "} {" + body
					+ "} for method {" + getMethod() + "} and uri {" + getUri() + "}");

			// Try to parse the body data and fail using the extracted exception.
			try {
				JsonObject responseObj = new JsonObject(body);
				getFuture().fail(new MeshRestClientJsonObjectException(response.statusCode(), response.statusMessage(), responseObj));
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
