package com.gentics.mesh.rest.dbadmin.impl;

import static com.gentics.mesh.rest.client.impl.HttpMethod.POST;

import java.time.Duration;
import java.util.Collections;

import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import com.gentics.mesh.rest.client.impl.HttpMethod;
import com.gentics.mesh.rest.client.impl.MeshOkHttpRequestImpl;
import com.gentics.mesh.rest.dbadmin.DatabaseAdminClientConfig;
import com.gentics.mesh.rest.dbadmin.DatabaseAdminRestClient;

import okhttp3.OkHttpClient;

public class DatabaseAdminOkHttpClientImpl implements DatabaseAdminRestClient {

	public static final int DEFAULT_PORT = 8082;

	private final OkHttpClient client;
	private final DatabaseAdminClientConfig config;
	private static OkHttpClient defaultClient;

	public DatabaseAdminOkHttpClientImpl(DatabaseAdminClientConfig config) {
		this(config, defaultClient());
	}

	public DatabaseAdminOkHttpClientImpl(DatabaseAdminClientConfig config, OkHttpClient client) {
		this.client = client;
		this.config = config;
	}

	/**
	 * We need a long timeout per default since some requests take a long time. For all tests a 1 minute timeout works fine.
	 * 
	 * @return
	 */
	private static OkHttpClient defaultClient() {
		if (defaultClient == null) {
			defaultClient = new OkHttpClient.Builder()
				.callTimeout(Duration.ofMinutes(1))
				.connectTimeout(Duration.ofMinutes(1))
				.writeTimeout(Duration.ofMinutes(1))
				.readTimeout(Duration.ofMinutes(1))
				.build();
		}
		return defaultClient;
	}

	@Override
	public MeshRequest<EmptyResponse> stopDatabase() {
		return prepareRequest(POST, "/dbstop", EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> startDatabase() {
		return prepareRequest(POST, "/dbstart", EmptyResponse.class);
	}

	private String getUrl(String path) {
		return config.getBaseUrl() + path;
	}

	private <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT) {
		return MeshOkHttpRequestImpl.EmptyRequest(client, null, method.name(), getUrl(path), Collections.emptyMap(), classOfT);
	}

	@Override
	public void close() {
		// We don't close the client because it is either
		// * The default client. This cannot be closed because other instances might use it.
		// * A user provided client. The user could use the client somewhere else, so we should not close it here.
	}
}
