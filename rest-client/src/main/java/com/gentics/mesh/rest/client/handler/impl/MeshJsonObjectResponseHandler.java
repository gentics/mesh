package com.gentics.mesh.rest.client.handler.impl;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.rest.client.handler.AbstractMeshResponseHandler;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The mesh response handler which will return JSON using the {@link JsonObject} class.
 */
public class MeshJsonObjectResponseHandler extends AbstractMeshResponseHandler<JsonObject> {

	private static final Logger log = LoggerFactory.getLogger(MeshJsonResponseHandler.class);

	/**
	 * Create a new response handler.
	 * 
	 * @param method
	 *            Method that was used for the request
	 * @param uri
	 *            Uri that was queried
	 */
	public MeshJsonObjectResponseHandler(HttpMethod method, String uri) {
		super(method, uri);
	}

	public void handleSuccess(HttpClientResponse response) {
		String contentType = response.getHeader("Content-Type");
		// FIXME TODO in theory it would also be possible that a customer uploads JSON into mesh. In those cases we would also need to return it directly
		// (without parsing)
		if (contentType == null && response.statusCode() == NO_CONTENT.code()) {
			if (log.isDebugEnabled()) {
				log.debug("Got {" + NO_CONTENT.code() + "} response.");
			}
			future.complete();
		} else if (contentType.startsWith(HttpConstants.APPLICATION_JSON)) {
			response.bodyHandler(bh -> {
				String json = bh.toString();
				// System.out.println(json);
				if (log.isDebugEnabled()) {
					log.debug(json);
				}
				try {
					future.setBodyJson(json);
					future.complete(new JsonObject(json));
				} catch (Exception e) {
					log.error("Failed to deserialize json", e);
					future.fail(e);
				}
			});
		} else {
			response.bodyHandler(buffer -> {
				future.fail(new RuntimeException("Request can't be handled by this handler since the content type was {" + contentType + "}"));
			});
		}
	}
}