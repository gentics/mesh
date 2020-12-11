package com.gentics.mesh.rest.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.io.IOUtils;

public class MeshRestClientConfig {
	private final String host;
	private final String basePath;
	private final int port;
	private final boolean ssl;
	private final Duration websocketReconnectInterval;
	private final Duration websocketPingInterval;
	private final Set<byte[]> trustedCAs;
	private final byte[] clientCert;
	private final byte[] clientKey;
	private final boolean hostnameVerification;

	public MeshRestClientConfig(Builder builder) {
		this.host = Objects.requireNonNull(builder.host);
		this.port = builder.port;
		this.ssl = builder.ssl;
		this.websocketReconnectInterval = builder.websocketReconnectInterval;
		this.websocketPingInterval = builder.websocketPingInterval;
		this.hostnameVerification = builder.hostnameVerification;
		this.basePath = builder.basePath;
		this.trustedCAs = builder.trustedCAs;
		this.clientCert = builder.clientCert;
		this.clientKey = builder.clientKey;
	}

	/**
	 * Creates a new builder based on the values of this config.
	 * @return
	 */
	public MeshRestClientConfig.Builder asBuilder() {
		return new Builder(this);
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

	public String getBasePath() {
		return basePath;
	}

	public String getBaseUrl() {
		return (isSsl() ? "https" : "http") +
			"://" + getHost() + ":" + getPort() +
			getBasePath();
	}

	public boolean isVerifyHostnames() {
		return hostnameVerification;
	}

	public byte[] getClientCert() {
		return clientCert;
	}

	public byte[] getClientKey() {
		return clientKey;
	}

	public Set<byte[]> getTrustedCAs() {
		return trustedCAs;
	}

	public static Builder newConfig() {
		return new Builder();
	}

	/**
	 * Builder for the REST config
	 */
	public static class Builder {
		private String host;
		private String basePath = "/api/v1";
		private int port = 8080;
		private boolean ssl = false;
		private Duration websocketReconnectInterval = Duration.ofSeconds(5);
		private Duration websocketPingInterval = Duration.ofSeconds(2);
		public boolean hostnameVerification = false;
		private final Set<byte[]> trustedCAs;
		private byte[] clientCert;
		private byte[] clientKey;

		public Builder() {
			trustedCAs = new HashSet<>();
		}

		/**
		 * Creates a new builder based on the values of the given config.
		 * @param config
		 */
		public Builder(MeshRestClientConfig config) {
			Objects.requireNonNull(config);

			setHost(config.getHost());
			setBasePath(config.getBasePath());
			setPort(config.getPort());
			setSsl(config.isSsl());
			setWebsocketReconnectInterval(config.getWebsocketReconnectInterval());
			setWebsocketPingInterval(config.getWebsocketPingInterval());
			setHostnameVerification(config.isVerifyHostnames());
			trustedCAs = new HashSet<>(config.trustedCAs);
			if (config.getClientCert() != null) {
				setClientCert(config.getClientCert().clone());
			}
			if (config.getClientKey() != null) {
				setClientKey(config.getClientKey().clone());
			}
		}

		public MeshRestClientConfig build() {
			return new MeshRestClientConfig(this);
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
		 * Decides if the client should use ssl to connect to mesh.
		 *
		 * <p>
		 * Default: false
		 * </p>
		 *
		 * @param ssl
		 * @return
		 */
		public Builder setSsl(boolean ssl) {
			this.ssl = ssl;
			return this;
		}

		/**
		 * Set the hostname verification flag.
		 * 
		 * @param flag
		 * @return
		 */
		public Builder setHostnameVerification(boolean flag) {
			this.hostnameVerification = flag;
			return this;
		}

		/**
		 * Sets the amount of time to wait until the client tries to establish a new websocket connection in case of failure.
		 *
		 * <p>
		 * Default: 5 seconds
		 * </p>
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
		 * <p>
		 * Default: 2 seconds
		 * </p>
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
		 * @param basePath
		 * @return
		 */
		public Builder setBasePath(String basePath) {
			this.basePath = Objects.requireNonNull(basePath);
			return this;
		}

		/**
		 * Set the filesystem path to the client SSL key which is formatted in PEM format.
		 * 
		 * @param path
		 */
		public Builder setClientKey(String path) {
			this.clientKey = readFile(path);
			return this;
		}

		/**
		 * Set the InputStream from which the client SSL key in PEM format will be read.
		 * 
		 * @param ins
		 * @return
		 */
		public Builder setClientKey(InputStream ins) {
			try {
				this.clientKey = IOUtils.toByteArray(ins);
			} catch (Exception e) {
				throw new RuntimeException("Error while reading key stream", e);
			}
			return this;
		}

		/**
		 * Set the client key in PEM format.
		 * 
		 * @param data
		 * @return
		 */
		public Builder setClientKey(byte[] data) {
			this.clientKey = data;
			return this;
		}

		/**
		 * Set the filesystem path to the client SSL cert which is formatted in PEM format.
		 * 
		 * @param path
		 * @return
		 */
		public Builder setClientCert(String path) {
			this.clientCert = readFile(path);
			return this;
		}

		/**
		 * Set the InputStream from which the client SSL cert in PEM format will be read.
		 * 
		 * @param ins
		 * @return
		 */
		public Builder setClientCert(InputStream ins) {
			try {
				this.clientCert = IOUtils.toByteArray(ins);
			} catch (Exception e) {
				throw new RuntimeException("Error while reading key stream", e);
			}
			return this;
		}

		/**
		 * Set the client cert in PEM format.
		 * 
		 * @param data
		 * @return
		 */
		public Builder setClientCert(byte[] data) {
			this.clientCert = data;
			return this;
		}

		/**
		 * Add CA cert from the filesystem path to the CAs that will be trusted by the client.
		 * 
		 * @param path
		 * @return
		 */
		public Builder addTrustedCA(String path) {
			this.trustedCAs.add(readFile(path));
			return this;
		}

		/**
		 * Add CA cert from the stream to the CAs that will be trusted by the client.
		 * 
		 * @param ins
		 * @return
		 */
		public Builder addTrustedCA(InputStream ins) {
			try {
				this.trustedCAs.add(IOUtils.toByteArray(ins));
			} catch (Exception e) {
				throw new RuntimeException("Error while reading key stream", e);
			}
			return this;
		}

		/**
		 * Add CA cert in PEM format to the CAs that will be trusted by the client.
		 * 
		 * @param data
		 * @return
		 */
		public Builder addTrustedCA(byte[] data) {
			this.trustedCAs.add(data);
			return this;
		}

		private static byte[] readFile(String path) {
			Objects.requireNonNull(path);
			File keyFile = new File(path);
			if (!keyFile.exists()) {
				throw new RuntimeException("Could not find file {" + path + "}");
			}
			try (FileInputStream fis = new FileInputStream(keyFile)) {
				return IOUtils.toByteArray(fis);
			} catch (Exception e) {
				throw new RuntimeException("Error while reading file {" + path + "}", e);
			}
		}
	}
}
