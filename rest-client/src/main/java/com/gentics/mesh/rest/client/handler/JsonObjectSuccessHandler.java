package com.gentics.mesh.rest.client.handler;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.rest.client.handler.impl.ModelResponseHandler;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface JsonObjectSuccessHandler extends ResponseHandler<JsonObject> {

	static final Logger log = LoggerFactory.getLogger(ModelResponseHandler.class);

	default void handleSuccess(HttpClientResponse response) {
		String contentType = response.getHeader("Content-Type");
		// FIXME TODO in theory it would also be possible that a customer uploads JSON into mesh. In those cases we would also need to return it directly
		// (without parsing)
		if (contentType == null && response.statusCode() == NO_CONTENT.code()) {
			if (log.isDebugEnabled()) {
				log.debug("Got {" + NO_CONTENT.code() + "} response.");
			}
			getFuture().complete();
		} else if (contentType.startsWith(HttpConstants.APPLICATION_JSON)) {
			response.bodyHandler(bh -> {
				String json = bh.toString();
				if (log.isDebugEnabled()) {
					log.debug(json);
				}
				try {
					getFuture().setBodyJson(json);
					getFuture().complete(new JsonObject(json));
				} catch (Exception e) {
					log.error("Failed to deserialize json", e);
					getFuture().fail(e);
				}
			});
		} else {
			response.bodyHandler(buffer -> getFuture().fail(new RuntimeException("Request can't be handled by this handler since the content type was {" + contentType + "}")));
		}
	}

}
