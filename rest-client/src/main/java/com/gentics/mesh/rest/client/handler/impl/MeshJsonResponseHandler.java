package com.gentics.mesh.rest.client.handler.impl;

import static io.netty.handler.codec.http.HttpResponseStatus.NO_CONTENT;

import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.handler.AbstractMeshResponseHandler;

import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The mesh response handler will take care of delegating the returned JSON to the correct JSON deserializer.
 * 
 * @param <T>
 */
public class MeshJsonResponseHandler<T> extends AbstractMeshResponseHandler<T> {

	private static final Logger log = LoggerFactory.getLogger(MeshJsonResponseHandler.class);

	private Class<? extends T> classOfT;

	/**
	 * Create a new response handler.
	 * 
	 * @param classOfT
	 *            Expected response POJO class
	 * @param method
	 *            Method that was used for the request
	 * @param uri
	 *            Uri that was queried
	 */
	public MeshJsonResponseHandler(Class<? extends T> classOfT, HttpMethod method, String uri) {
		super(method, uri);
		this.classOfT = classOfT;
	}

	/**
	 * Handle the mesh response. This method will deserialise the JSON response.
	 */
	public void handleSuccess(HttpClientResponse response) {
		String contentType = response.getHeader("Content-Type");
		//FIXME TODO in theory it would also be possible that a customer uploads JSON into mesh. In those cases we would also need to return it directly (without parsing)
		if (contentType == null && response.statusCode() == NO_CONTENT.code()) {
			if (log.isDebugEnabled()) {
				log.debug("Got {" + NO_CONTENT.code() + "} response.");
			}
			future.complete();
		} else if (contentType.startsWith(HttpConstants.APPLICATION_JSON)) {
			response.bodyHandler(bh -> {
				String json = bh.toString();
				//				System.out.println(json);
				if (log.isDebugEnabled()) {
					log.debug(json);
				}
				try {
					future.setBodyJson(json);
					T restObj = JsonUtil.readValue(json, classOfT);
					future.complete(restObj);
				} catch (Exception e) {
					log.error("Failed to deserialize json to class {" + classOfT + "}", e);
					future.fail(e);
				}
			});
		} else {
			if (classOfT.isAssignableFrom(String.class)) {
				response.bodyHandler(buffer -> {
					future.complete((T) buffer.toString());
				});
			} else {
				response.bodyHandler(buffer -> {
					future.fail(new RuntimeException("Request can't be handled by this handler since the content type was {" + contentType + "}"));
				});
			}
		}
	}

}