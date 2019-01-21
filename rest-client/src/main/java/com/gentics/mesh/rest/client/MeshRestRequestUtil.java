package com.gentics.mesh.rest.client;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.rest.MeshRestClientAuthenticationProvider;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility methods to be used in combination with the vertx http client.
 */
public final class MeshRestRequestUtil {

	private static final Logger log = LoggerFactory.getLogger(MeshRestRequestUtil.class);

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
	 *            Content type of the posted data
	 * @param meshRestClient
	 *            Client to use
	 * @param authentication
	 *            Authentication provider to use
	 * @param disableAnonymousAccess
	 * @param accepts
	 *            Accept header
	 * @return
	 */
	public static <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, Buffer bodyData, String contentType,
			MeshRestClient meshRestClient, MeshRestClientAuthenticationProvider authentication, boolean disableAnonymousAccess, String accepts) {
//		String uri = meshRestClient.getBaseUri() + path;
//		ResponseHandler<T> handler = new ModelResponseHandler<T>(classOfT, method, uri);
//
//		HttpClientRequest request = meshRestClient.getClient().request(method, uri, handler);
//
//		// Instruct the mesh auth handler to disable anonymous access handling even if it is enabled on the server
//		if (disableAnonymousAccess) {
//			request.putHeader(MeshHeaders.ANONYMOUS_AUTHENTICATION, "disable");
//		}
//		// Let the response handler fail when an error ocures
//		request.exceptionHandler(e -> {
//			handler.getFuture().fail(e);
//		});
//		if (log.isDebugEnabled()) {
//			log.debug("Invoking {" + method.name() + "} request to {" + uri + "}");
//		}
//
//		return new MeshHttpRequestImpl<T>(request, handler, bodyData, contentType, authentication, accepts);
		// TODO implement
		throw new RuntimeException("Not implemeneted");
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
	 *            Model to be converted to JSON and send to the path
	 * @param meshRestClient
	 *            Http meshRestClient to be used
	 * @param authentication
	 *            Authentication provider to use
	 * @param disableAnonymousAccess
	 * @return
	 */
	public static <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, RestModel restModel,
			MeshRestClient meshRestClient, MeshRestClientAuthenticationProvider authentication, boolean disableAnonymousAccess) {
		Buffer buffer = Buffer.buffer();
		String json = restModel.toJson();
		if (log.isDebugEnabled()) {
			log.debug(json);
		}
		buffer.appendString(json);
		return prepareRequest(method, path, classOfT, buffer, "application/json", meshRestClient, authentication, disableAnonymousAccess, "application/json");
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
	 * @param meshRestClient
	 *            Http meshRestClient to be used
	 * @param authentication
	 *            Authentication provider to use
	 * @param disableAnonymousAccess
	 * @return
	 */
	public static <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, String jsonBodyData,
			MeshRestClient meshRestClient, MeshRestClientAuthenticationProvider authentication, boolean disableAnonymousAccess) {

		if (log.isDebugEnabled()) {
			log.debug("Posting json {" + jsonBodyData + "}");
		}
		Buffer buffer = Buffer.buffer();
		if (!StringUtils.isEmpty(jsonBodyData)) {
			buffer.appendString(jsonBodyData);
		}

		return prepareRequest(method, path, classOfT, buffer, "application/json", meshRestClient, authentication, disableAnonymousAccess, "application/json");
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
	 * @param meshRestClient
	 *            Http meshRestClient to be used
	 * @param authentication
	 *            Authentication provider to use
	 * @param disableAnonymousAccess
	 *            Flag which is used to disable the anonymous access handling
	 * @return
	 */
	public static <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, MeshRestClient meshRestClient,
			MeshRestClientAuthenticationProvider authentication, boolean disableAnonymousAccess) {
		return prepareRequest(method, path, classOfT, Buffer.buffer(), null, meshRestClient, authentication, disableAnonymousAccess, "application/json");
	}
}
