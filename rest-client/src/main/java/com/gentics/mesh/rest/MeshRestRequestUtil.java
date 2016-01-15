package com.gentics.mesh.rest;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.schema.SchemaStorage;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class MeshRestRequestUtil {
	private static final Logger log = LoggerFactory.getLogger(MeshRestRequestUtil.class);
	public static final String BASEURI = "/api/v1";
	
//	protected <T> Future<T> invokeRequest(HttpMethod method, String path, Class<T> classOfT, Buffer bodyData, String contentType) {
//		String uri = BASEURI + path;
//		MeshResponseHandler<T> handler = new MeshResponseHandler<>(classOfT, this, method, uri);
//
//		HttpClientRequest request = client.request(method, uri, handler);
//		// Let the response handler fail when an error ocures
//		request.exceptionHandler(e -> {
//			handler.getFuture().fail(e);
//		});
//		if (log.isDebugEnabled()) {
//			log.debug("Invoking get request to {" + uri + "}");
//		}
//
//		if (getCookie() != null) {
//			request.headers().add("Cookie", getCookie());
//		} else {
//			request.headers().add("Authorization", "Basic " + authEnc);
//		}
//		request.headers().add("Accept", "application/json");
//
//		if (bodyData.length() != 0) {
//			request.headers().add("content-length", String.valueOf(bodyData.length()));
//			if (!StringUtils.isEmpty(contentType)) {
//				request.headers().add("content-type", contentType);
//			}
//			request.write(bodyData);
//		}
//		request.end();
//		return handler.getFuture();
//	}
	
	public static <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> classOfT, Buffer bodyData, String contentType, HttpClient client, MeshRestClientAuthenticationProvider authentication, SchemaStorage schemaStorage) {
		String uri = BASEURI + path;
		MeshResponseHandler<T> handler = new MeshResponseHandler<>(classOfT, method, uri, schemaStorage);

		HttpClientRequest request = client.request(method, uri, handler);
		// Let the response handler fail when an error ocures
		request.exceptionHandler(e -> {
			handler.getFuture().fail(e);
		});
		if (log.isDebugEnabled()) {
			log.debug("Invoking get request to {" + uri + "}");
		}

		if (authentication != null) {
			authentication.addAuthenticationInformation(request).subscribe(x -> {
				request.headers().add("Accept", "application/json");

				if (bodyData.length() != 0) {
					request.headers().add("content-length", String.valueOf(bodyData.length()));
					if (!StringUtils.isEmpty(contentType)) {
						request.headers().add("content-type", contentType);
					}
					request.write(bodyData);
				}
				request.end();
			});
		} else {
			request.headers().add("Accept", "application/json");

			if (bodyData.length() != 0) {
				request.headers().add("content-length", String.valueOf(bodyData.length()));
				if (!StringUtils.isEmpty(contentType)) {
					request.headers().add("content-type", contentType);
				}
				request.write(bodyData);
			}
			request.end();
		}
		
		return handler.getFuture();
	}

	public static <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> classOfT, RestModel restModel, HttpClient client, MeshRestClientAuthenticationProvider authentication, SchemaStorage schemaStorage) {
		Buffer buffer = Buffer.buffer();
		String json = JsonUtil.toJson(restModel);
		if (log.isDebugEnabled()) {
			log.debug(json);
		}
		buffer.appendString(json);
		return handleRequest(method, path, classOfT, buffer, "application/json", client, authentication, schemaStorage);
	}

	public static <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> classOfT, String jsonBodyData, HttpClient client, MeshRestClientAuthenticationProvider authentication, SchemaStorage schemaStorage) {

		Buffer buffer = Buffer.buffer();
		if (!StringUtils.isEmpty(jsonBodyData)) {
			buffer.appendString(jsonBodyData);
		}

		return handleRequest(method, path, classOfT, buffer, "application/json", client, authentication, schemaStorage);
	}

	public static <T> Future<T> handleRequest(HttpMethod method, String path, Class<T> classOfT, HttpClient client, MeshRestClientAuthenticationProvider authentication, SchemaStorage schemaStorage) {
		return handleRequest(method, path, classOfT, Buffer.buffer(), null, client, authentication, schemaStorage);
	}
}
