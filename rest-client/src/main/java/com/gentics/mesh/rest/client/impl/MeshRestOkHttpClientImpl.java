package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.rest.client.MeshRequest;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import okhttp3.OkHttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses OkHttp client whenever possible.
 */
public class MeshRestOkHttpClientImpl extends MeshRestHttpClientImpl {

	private final String origin;
	private final OkHttpClient client;

	public MeshRestOkHttpClientImpl(String host, Vertx vertx) {
		super(host, vertx);
		origin = "http://" + host;
		this.client = createClient();
	}

	public MeshRestOkHttpClientImpl(String host, int port, boolean ssl, Vertx vertx) {
		this(host, port, ssl, vertx, createClient());
	}

	public MeshRestOkHttpClientImpl(String host, int port, boolean ssl, Vertx vertx, OkHttpClient client) {
		super(host, port, ssl, vertx);
		String scheme = ssl ? "https" : "http";
		origin = scheme + "://" + host + ":" + port;
		this.client = client;
	}

	private static OkHttpClient createClient() {
		return new OkHttpClient.Builder()
			.callTimeout(Duration.ofMinutes(1))
			.connectTimeout(Duration.ofMinutes(1))
			.writeTimeout(Duration.ofMinutes(1))
			.readTimeout(Duration.ofMinutes(1))
			.build();
	}

	@Override
	protected <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, Buffer bodyData, String contentType) {
		return MeshOkHttpReqeuestImpl.BinaryRequest(client, method.name(), getUrl(path), createHeaders(), classOfT, bodyData.getBytes(), contentType);
	}

	@Override
	protected <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, RestModel restModel) {
		return handleRequest(method, path, classOfT, restModel.toJson());
	}

	@Override
	protected <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT) {
		return MeshOkHttpReqeuestImpl.EmptyRequest(client, method.name(), getUrl(path), createHeaders(), classOfT);
	}

	@Override
	protected <T> MeshRequest<T> handleRequest(HttpMethod method, String path, Class<? extends T> classOfT, String jsonBodyData) {
		return MeshOkHttpReqeuestImpl.JsonRequest(client, method.name(), getUrl(path), createHeaders(), classOfT, jsonBodyData);
	}

	private Map<String, String> createHeaders() {
		Map<String, String> headers = new HashMap<>();
		if (disableAnonymousAccess) {
			headers.put(MeshHeaders.ANONYMOUS_AUTHENTICATION, "disable");
		}
		headers.put("Accept", "application/json");
		headers.putAll(authentication.getHeaders());
		return headers;
	}

	private String getUrl(String path) {
		return origin + getBaseUri() + path;
	}
}
