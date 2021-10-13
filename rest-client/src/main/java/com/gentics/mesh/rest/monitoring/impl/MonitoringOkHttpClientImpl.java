package com.gentics.mesh.rest.monitoring.impl;

import static com.gentics.mesh.rest.client.impl.HttpMethod.GET;

import java.time.Duration;
import java.util.Collections;

import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.admin.status.MeshStatusResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import com.gentics.mesh.rest.client.impl.HttpMethod;
import com.gentics.mesh.rest.client.impl.MeshOkHttpRequestImpl;
import com.gentics.mesh.rest.monitoring.MonitoringClientConfig;
import com.gentics.mesh.rest.monitoring.MonitoringRestClient;

import okhttp3.OkHttpClient;

/**
 * Monitoring client implementation for {@link OkHttpClient}.
 * 
 * @see MonitoringRestClient
 */
public class MonitoringOkHttpClientImpl implements MonitoringRestClient {

	public static final int DEFAULT_PORT = 8081;

	private final OkHttpClient client;
	private final MonitoringClientConfig config;
	private static OkHttpClient defaultClient;

	public MonitoringOkHttpClientImpl(MonitoringClientConfig config) {
		this(config, defaultClient());
	}

	public MonitoringOkHttpClientImpl(MonitoringClientConfig config, OkHttpClient client) {
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

	private String getUrl(String path) {
		return config.getBaseUrl() + path;
	}

	private <T> MeshRequest<T> prepareRequest(HttpMethod method, String path, Class<? extends T> classOfT) {
		return MeshOkHttpRequestImpl.EmptyRequest(client, method.name(), getUrl(path), Collections.emptyMap(), classOfT);
	}

	@Override
	public MeshRequest<String> metrics() {
		return prepareRequest(GET, "/metrics", String.class);
	}

	@Override
	public MeshRequest<MeshServerInfoModel> versions() {
		return prepareRequest(GET, "/versions", MeshServerInfoModel.class);
	}

	@Override
	public MeshRequest<MeshStatusResponse> status() {
		return prepareRequest(GET, "/status", MeshStatusResponse.class);
	}

	@Override
	public MeshRequest<ClusterStatusResponse> clusterStatus() {
		return prepareRequest(GET, "/cluster/status", ClusterStatusResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> ready() {
		return prepareRequest(GET, "/health/ready", EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> live() {
		return prepareRequest(GET, "/health/live", EmptyResponse.class);
	}

	@Override
	public MeshRequest<EmptyResponse> writable() {
		return prepareRequest(GET, "/cluster/writable", EmptyResponse.class);
	}

	@Override
	public void close() {
		// We don't close the client because it is either
		// * The default client. This cannot be closed because other instances might use it.
		// * A user provided client. The user could use the client somewhere else, so we should not close it here.
	}

}
