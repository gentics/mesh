package com.gentics.mesh.rest.client;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.rest.MeshRestClientAuthenticationProvider;
import com.gentics.mesh.rest.client.handler.MeshResponseHandler;
import com.gentics.mesh.rest.client.handler.impl.MeshJsonResponseHandler;
import com.gentics.mesh.rest.client.impl.MeshHttpRequestImpl;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility methods to be used in combination with the vertx http client.
 */
public final class MeshRestRequestUtil {

	private static final Logger log = LoggerFactory.getLogger(MeshRestRequestUtil.class);
	public static final String BASEURI = "/api/v1";

	/**
	 * Prepare the request by adding the response handlers, auth info, payload etc.
	 * 
	 * @param method
	 *            Request method
	 * @param path
	 *            Path
	 * @param classOfT
	 *            Expected response POJO class
	 * @param bodyData
	 *            Body data to post
	 * @param contentType
	 * @param client
	 *            Client to use
	 * @param authentication
	 *            Authentication provider to use
	 * @return
	 */
	public static <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, Buffer bodyData, String contentType,
			HttpClient client, MeshRestClientAuthenticationProvider authentication) {
		String uri = BASEURI + path;
		MeshResponseHandler<T> handler = new MeshJsonResponseHandler<T>(classOfT, method, uri);

		HttpClientRequest request = client.request(method, uri, handler);
		// Let the response handler fail when an error ocures
		request.exceptionHandler(e -> {
			String p = path;
			handler.getFuture().fail(e);
		});
		if (log.isDebugEnabled()) {
			log.debug("Invoking get request to {" + uri + "}");
		}

		return new MeshHttpRequestImpl<T>(request, handler, bodyData, contentType, authentication);
	}

	/**
	 * Prepare the request by adding the payload, auth info etc.
	 * 
	 * @param method
	 *            Request method
	 * @param path
	 *            Request path
	 * @param classOfT
	 *            Expected response object class
	 * @param restModel
	 *            Model to be converted to json and send to the path
	 * @param client
	 *            Http client to be used
	 * @param authentication
	 *            Authentication provider to use
	 * @return
	 */
	public static <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, RestModel restModel,
			HttpClient client, MeshRestClientAuthenticationProvider authentication) {
		Buffer buffer = Buffer.buffer();
		String json = JsonUtil.toJson(restModel);
		if (log.isDebugEnabled()) {
			log.debug(json);
		}
		buffer.appendString(json);
		return prepareRequest(method, path, classOfT, buffer, "application/json", client, authentication);
	}

	/**
	 * Prepare the request by adding the payload, auth info etc.
	 * 
	 * @param method
	 *            Request method
	 * @param path
	 *            Request path
	 * @param classOfT
	 *            Expected response object class
	 * @param jsonBodyData
	 *            JSON Data to post
	 * @param client
	 *            Http client to be used
	 * @param authentication
	 *            Authentication provider to use
	 * @return
	 */
	public static <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, String jsonBodyData,
			HttpClient client, MeshRestClientAuthenticationProvider authentication) {

		if (log.isDebugEnabled()) {
			log.debug("Posting json {" + jsonBodyData + "}");
		}
		Buffer buffer = Buffer.buffer();
		if (!StringUtils.isEmpty(jsonBodyData)) {
			buffer.appendString(jsonBodyData);
		}

		return prepareRequest(method, path, classOfT, buffer, "application/json", client, authentication);
	}

	/**
	 * Prepare the request by adding the payload, auth info etc.
	 * 
	 * @param method
	 *            Request method
	 * @param path
	 *            Request path
	 * @param classOfT
	 *            Expected response object class
	 * @param client
	 *            Http client to be used
	 * @param authentication
	 *            Authentication provider to use
	 * @return
	 */
	public static <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, HttpClient client,
			MeshRestClientAuthenticationProvider authentication) {
		return prepareRequest(method, path, classOfT, Buffer.buffer(), null, client, authentication);
	}
}
