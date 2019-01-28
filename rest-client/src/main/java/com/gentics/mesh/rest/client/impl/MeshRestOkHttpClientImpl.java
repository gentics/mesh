package com.gentics.mesh.rest.client.impl;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshWebsocket;
import okhttp3.OkHttpClient;

import java.io.InputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses OkHttp client whenever possible.
 */
public class MeshRestOkHttpClientImpl extends MeshRestHttpClientImpl {

	private final OkHttpClient client;
	private final MeshRestClientConfig config;
	private static OkHttpClient defaultClient;

	public MeshRestOkHttpClientImpl(MeshRestClientConfig config) {
		this(config, defaultClient());
	}

	public MeshRestOkHttpClientImpl(MeshRestClientConfig config, OkHttpClient client) {
		this.client = client;
		this.config = config;
	}

	private static OkHttpClient defaultClient() {
		if (defaultClient == null) {
			defaultClient = new OkHttpClient.Builder()
				.callTimeout(Duration.ofMinutes(1))
				.connectTimeout(Duration.ofMinutes(1))
				.writeTimeout(Duration.ofMinutes(1))
				.readTimeout(Duration.ofMinutes(1))
				.pingInterval(Duration.ofSeconds(1))
				.build();
		}
		return defaultClient;
	}

	@Override
	public <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, InputStream bodyData, long fileSize, String contentType) {
		return MeshOkHttpReqeuestImpl.BinaryRequest(client, method.name(), getUrl(path), createHeaders(), classOfT, bodyData, fileSize, contentType);
	}

	@Override
	public <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, RestModel restModel) {
		return handleRequest(method, path, classOfT, restModel.toJson());
	}

	@Override
	public <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT) {
		return MeshOkHttpReqeuestImpl.EmptyRequest(client, method.name(), getUrl(path), createHeaders(), classOfT);
	}

	@Override
	public <T> MeshRequest<T> handleRequest(HttpMethod method, String path, Class<? extends T> classOfT, String jsonBodyData) {
		return MeshOkHttpReqeuestImpl.JsonRequest(client, method.name(), getUrl(path), createHeaders(), classOfT, jsonBodyData);
	}

	@Override
	public MeshWebsocket eventbus() {
		return new OkHttpWebsocket(client, config);
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
		return config.getBaseUrl() + path;
	}

	@Override
	public void close() {
		// TODO close default client?
	}
}
