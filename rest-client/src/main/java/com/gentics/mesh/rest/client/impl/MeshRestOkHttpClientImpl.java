package com.gentics.mesh.rest.client.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.http.MeshHeaders;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClientConfig;
import com.gentics.mesh.rest.client.MeshWebsocket;

import okhttp3.OkHttpClient;

/**
 * Uses OkHttp client whenever possible.
 */
public class MeshRestOkHttpClientImpl extends MeshRestHttpClientImpl {

	private final OkHttpClient client;
	private final MeshRestClientConfig config;

	public MeshRestOkHttpClientImpl(MeshRestClientConfig config) {
		this(config, OkHttpClientUtil.createClient(config));
	}

	public MeshRestOkHttpClientImpl(MeshRestClientConfig config, OkHttpClient client) {
		this.client = client;
		this.config = config;
	}

	@Override
	protected boolean isMinifyJson() {
		return config.isMinifyJson();
	}

	@Override
	public <T> MeshRequest<T> prepareFileuploadRequest(HttpMethod method, String path, Class<? extends T> classOfT,
			String fileName, String contentType, InputStream fileData, long fileSize, Map<String, String> fields) {
		return MeshOkHttpRequestImpl.FileUploadRequest(this, client, config, method.name(), getUrl(path),
				createHeaders(), classOfT, fileName, contentType, fileData, fileSize, fields);
	}

	@Override
	public <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT, RestModel restModel) {
		return handleRequest(method, path, classOfT, restModel == null ? null :restModel.toJson(config.isMinifyJson()));
	}

	@Override
	public <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT) {
		return MeshOkHttpRequestImpl.EmptyRequest(this, client, config, method.name(), getUrl(path), createHeaders(), classOfT);
	}

	@Override
	public <T> MeshRequest<T> handleRequest(HttpMethod method, String path, Class<? extends T> classOfT, String jsonBodyData) {
		return MeshOkHttpRequestImpl.JsonRequest(this, client, config, method.name(), getUrl(path), createHeaders(), classOfT, jsonBodyData);
	}

	@Override
	public <T> MeshRequest<T> handleTextRequest(HttpMethod method, String path, Class<? extends T> classOfT, String data) {
		return MeshOkHttpRequestImpl.TextRequest(this, client, config, method.name(), getUrl(path), createHeaders(), classOfT, data);
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
		// We don't close the client because it is either
		// * The default client. This cannot be closed because other instances might use it.
		// * A user provided client. The user could use the client somewhere else, so we should not close it here.
	}

	@Override
	public MeshRestClientConfig getConfig() {
		return config;
	}
}
