package com.gentics.mesh.rest.monitoring;

import com.gentics.mesh.rest.monitoring.impl.MonitoringOkHttpClientImpl;

public interface MonitoringRestClient extends MonitoringClientMethods {

	/**
	 * Create a new mesh rest client.
	 *
	 * @param host
	 *            Server host
	 * @param port
	 *            Server port
	 * @return
	 */
	static MonitoringRestClient create(String host, int port) {
		return create(new MonitoringClientConfig.Builder()
			.setHost(host)
			.setPort(port)
			.build());
	}

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param host
	 *            Server host
	 * @return
	 */
	static MonitoringRestClient create(String host) {
		return create(new MonitoringClientConfig.Builder().setHost(host).build());
	}

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param config
	 *            Client configuration
	 * @return
	 */
	static MonitoringRestClient create(MonitoringClientConfig config) {
		return new MonitoringOkHttpClientImpl(config);
	}

	/**
	 * Close the client.
	 */
	void close();

}
