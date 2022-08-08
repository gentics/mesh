package com.gentics.mesh.rest.dbadmin;

import com.gentics.mesh.rest.dbadmin.impl.DatabaseAdminOkHttpClientImpl;

public interface DatabaseAdminRestClient extends DatabaseAdminMethods {

	/**
	 * Create a new mesh rest client.
	 *
	 * @param host
	 *            Server host
	 * @param port
	 *            Server port
	 * @return
	 */
	static DatabaseAdminRestClient create(String host, int port) {
		return create(DatabaseAdminClientConfig.builder()
			.withHost(host)
			.withPort(port)
			.build());
	}

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param host
	 *            Server host
	 * @return
	 */
	static DatabaseAdminRestClient create(String host) {
		return create(DatabaseAdminClientConfig.builder().withHost(host).build());
	}

	/**
	 * Create a new mesh rest client.
	 * 
	 * @param config
	 *            Client configuration
	 * @return
	 */
	static DatabaseAdminRestClient create(DatabaseAdminClientConfig config) {
		return new DatabaseAdminOkHttpClientImpl(config);
	}

	/**
	 * Close the client.
	 */
	void close();
}
