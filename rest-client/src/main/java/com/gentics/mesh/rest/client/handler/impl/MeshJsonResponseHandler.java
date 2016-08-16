package com.gentics.mesh.rest.client.handler.impl;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.client.MeshRestClientHttpException;
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

	@Override
	public void handle(HttpClientResponse response) {
		int code = response.statusCode();
		if (code >= 200 && code < 300) {

			String contentType = response.getHeader("Content-Type");
			//FIXME TODO in theory it would also be possible that a customer uploads JSON into mesh. In those cases we would also need to return it directly (without parsing)
			if (contentType.startsWith(HttpConstants.APPLICATION_JSON)) {
				response.bodyHandler(bh -> {
					String json = bh.toString();
					if (log.isDebugEnabled()) {
						log.debug(json);
					}
					try {
						// Hack to fallback to node responses when dealing with object classes
						if (classOfT.equals(Object.class)) {
							NodeResponse restObj = JsonUtil.readValue(json, NodeResponse.class);
							future.complete((T) restObj);
						} else {
							T restObj = JsonUtil.readValue(json, classOfT);
							future.complete(restObj);
						}
					} catch (Exception e) {
						log.error("Failed to deserialize json to class {" + classOfT + "}", e);
						future.fail(e);
					}
				});
			} else {
				response.bodyHandler(buffer -> {
					future.fail(new RuntimeException("Request can't be handled by this handler since the content type was {" + contentType + "}"));
				});
			}
		} else {
			response.bodyHandler(bh -> {
				String json = bh.toString();
				if (log.isDebugEnabled()) {
					log.debug(json);
				}

				log.error("Request failed with statusCode {" + response.statusCode() + "} statusMessage {" + response.statusMessage() + "} {" + json
						+ "} for method {" + getMethod() + "} and uri {" + getUri() + "}");

				try {
					GenericMessageResponse responseMessage = JsonUtil.readValue(json, GenericMessageResponse.class);
					future.fail(new MeshRestClientHttpException(response.statusCode(), response.statusMessage(), responseMessage));
					return;
				} catch (Exception e) {
					if (log.isDebugEnabled()) {
						log.debug("Could not deserialize response {" + json + "}.", e);
					}
				}
				future.fail(new MeshRestClientHttpException(response.statusCode(), response.statusMessage()));
			});
		}

	}

}