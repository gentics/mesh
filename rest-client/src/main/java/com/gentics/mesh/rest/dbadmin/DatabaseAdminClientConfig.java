package com.gentics.mesh.rest.dbadmin;

public class DatabaseAdminClientConfig {

	private final String host;
	private final String basePath;
	private final int port;

	private DatabaseAdminClientConfig(Builder builder) {
		this.host = builder.host;
		this.basePath = builder.basePath;
		this.port = builder.port;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String host;
		private String basePath;
		private int port;

		private Builder() {
		}

		public Builder withHost(String host) {
			this.host = host;
			return this;
		}

		public Builder withBasePath(String basePath) {
			this.basePath = basePath;
			return this;
		}

		public Builder withPort(int port) {
			this.port = port;
			return this;
		}

		public DatabaseAdminClientConfig build() {
			return new DatabaseAdminClientConfig(this);
		}
	}

	public String getHost() {
		return host;
	}

	public String getBasePath() {
		return basePath;
	}

	public int getPort() {
		return port;
	}

	public String getBaseUrl() {
		return "http" + "://" + getHost() + ":" + getPort() +
			getBasePath();
	}
}
