package com.gentics.mesh.rest.monitoring;

import java.util.Objects;

public class MonitoringClientConfig {
	private final String host;
	private final String basePath;
	private final int port;

	public MonitoringClientConfig(Builder builder) {
		this.host = Objects.requireNonNull(builder.host);
		this.port = builder.port;
		this.basePath = builder.basePath;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getBasePath() {
		return basePath;
	}

	public String getBaseUrl() {
		return "http" + "://" + getHost() + ":" + getPort() +
			getBasePath();
	}

	public static class Builder {
		private String host;
		private String basePath = "/api/v1";
		private int port = 8080;

		public MonitoringClientConfig build() {
			return new MonitoringClientConfig(this);
		}

		/**
		 * Sets the host to connect to. Example: "demo.getmesh.io"
		 *
		 * This configuration is required.
		 *
		 * @param host
		 * @return
		 */
		public Builder setHost(String host) {
			this.host = Objects.requireNonNull(host);
			return this;
		}

		/**
		 * Sets the port to connect to.
		 *
		 * <p>
		 * Default: 8080
		 * </p>
		 *
		 * @param port
		 * @return
		 */
		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		/**
		 * Sets the base uri that is prepended to all paths when making requests to mesh.
		 *
		 * <p>
		 * Default: "/api/v1"
		 * </p>
		 *
		 * @param basePath
		 * @return
		 */
		public Builder setBasePath(String basePath) {
			this.basePath = Objects.requireNonNull(basePath);
			return this;
		}
	}
}
