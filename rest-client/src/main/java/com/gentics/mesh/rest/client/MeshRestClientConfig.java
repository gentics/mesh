package com.gentics.mesh.rest.client;

import java.time.Duration;
import java.util.Objects;

public class MeshRestClientConfig {
	private final String host;
	private final String baseUri;
	private final int port;
	private final boolean ssl;
	private final Duration websocketReconnectInterval;
	private final Duration websocketPingInterval;


	public MeshRestClientConfig(Builder builder) {
		this.host = Objects.requireNonNull(builder.host);
		this.port = builder.port;
		this.ssl = builder.ssl;
		this.websocketReconnectInterval = builder.websocketReconnectInterval;
		this.websocketPingInterval = builder.websocketPingInterval;
		this.baseUri = builder.baseUri;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public boolean isSsl() {
		return ssl;
	}

	public Duration getWebsocketReconnectInterval() {
		return websocketReconnectInterval;
	}

	public Duration getWebsocketPingInterval() {
		return websocketPingInterval;
	}

	public String getBaseUri() {
		return baseUri;
	}

	public static class Builder {
		private String host;
		private String baseUri = "/api/v1";
		private int port = 8080;
		private boolean ssl = false;
		private Duration websocketReconnectInterval = Duration.ofSeconds(5);
		private Duration websocketPingInterval = Duration.ofSeconds(2);

		public MeshRestClientConfig build() {
			return new MeshRestClientConfig(this);
		}

		/**
		 * Sets the host to connect to.
		 * Example: "demo.getmesh.io"
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
		 * <p>Default: 8080</p>
		 *
		 * @param port
		 * @return
		 */
		public Builder setPort(int port) {
			this.port = port;
			return this;
		}

		/**
		 * Decides if the client should use ssl to connect to mesh.
		 *
		 * <p>Default: false</p>
		 *
		 * @param ssl
		 * @return
		 */
		public Builder setSsl(boolean ssl) {
			this.ssl = ssl;
			return this;
		}

		/**
		 * Sets the amount of time to wait until the client tries to establish a new websocket connection in
		 * case of failure.
		 *
		 * <p>Default: 5 seconds</p>
		 *
		 * @param websocketReconnectInterval
		 * @return
		 */
		public Builder setWebsocketReconnectInterval(Duration websocketReconnectInterval) {
			this.websocketReconnectInterval = Objects.requireNonNull(websocketReconnectInterval);
			return this;
		}

		/**
		 * Sets the amount of time between each ping message to mesh while connected via websocket.
		 *
		 * <p>Default: 2 seconds</p>
		 *
		 * @param websocketPingInterval
		 * @return
		 */
		public Builder setWebsocketPingInterval(Duration websocketPingInterval) {
			this.websocketPingInterval = Objects.requireNonNull(websocketPingInterval);
			return this;
		}

		/**
		 * Sets the base uri that is prepended to all paths when making requests to mesh.
		 *
		 * <p>Default: "/api/v1"</p>
		 *
		 * @param baseUri
		 * @return
		 */
		public Builder setBaseUri(String baseUri) {
			this.baseUri = Objects.requireNonNull(baseUri);
			return this;
		}
	}
}
