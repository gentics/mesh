package com.gentics.mesh.rest.client;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The mesh response handler will take care of delegating the returned JSON to the correct JSON deserializer.
 * 
 * @param <T>
 */
public class MeshResponseHandler<T> implements Handler<HttpClientResponse> {

	private static final Logger log = LoggerFactory.getLogger(MeshResponseHandler.class);

	private MeshResponse<T> future;
	private Class<? extends T> classOfT;
	private Handler<HttpClientResponse> handler;
	private HttpMethod method;
	private String uri;

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
	public MeshResponseHandler(Class<? extends T> classOfT, HttpMethod method, String uri) {
		this.classOfT = classOfT;
		this.future = new MeshResponse<>(Future.future());
		this.method = method;
		this.uri = uri;
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
			} else if (classOfT.equals(String.class)) {
				// if client requested a String, we just return the buffer as string
				response.bodyHandler(buffer -> {
					future.complete((T) buffer.toString());
				});
			} else {
				NodeDownloadResponse downloadResponse = new NodeDownloadResponse();
				downloadResponse.setContentType(contentType);
				String disposition = response.getHeader("content-disposition");
				String filename = disposition.substring(disposition.indexOf("=") + 1);
				downloadResponse.setFilename(filename);

				response.bodyHandler(buffer -> {
					downloadResponse.setBuffer(buffer);
					future.complete((T) downloadResponse);
				});
			}
		} else {
			response.bodyHandler(bh -> {
				String json = bh.toString();
				if (log.isDebugEnabled()) {
					log.debug(json);
				}

				log.error("Request failed with statusCode {" + response.statusCode() + "} statusMessage {" + response.statusMessage() + "} {" + json
						+ "} for method {" + this.method + "} and uri {" + this.uri + "}");

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
		if (handler != null) {
			handler.handle(response);
		}

	}

	public MeshResponse<T> getFuture() {
		return future;
	}

	/**
	 * Handle the client response.
	 * 
	 * @param handler
	 */
	public void handle(Handler<HttpClientResponse> handler) {
		this.handler = handler;
	}

}
