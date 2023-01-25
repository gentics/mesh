package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * GraphQL options
 */
@GenerateDocumentation
public class GraphQLOptions implements Option {
	public static final long DEFAULT_SLOW_THRESHOLD = 60_000L;

	public static final long DEFAULT_ASYNC_WAIT_TIMEOUT = 120_000L;

	public static final String MESH_GRAPHQL_SLOW_THRESHOLD_ENV = "MESH_GRAPHQL_SLOW_THRESHOLD";

	public static final String MESH_GRAPHQL_ASYNC_WAIT_TIMEOUT_ENV = "MESH_GRAPHQL_ASYNC_WAIT_TIMEOUT";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Threshold for logging slow graphql queries. Default: " + DEFAULT_SLOW_THRESHOLD + "ms")
	@EnvironmentVariable(name = MESH_GRAPHQL_SLOW_THRESHOLD_ENV, description = "Override the configured slow graphQl query threshold.")
	private Long slowThreshold = DEFAULT_SLOW_THRESHOLD;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Threshold for waiting for asynchronous graphql queries. Default: " + DEFAULT_ASYNC_WAIT_TIMEOUT + "ms")
	@EnvironmentVariable(name = MESH_GRAPHQL_ASYNC_WAIT_TIMEOUT_ENV, description = "Override the configured graphQl async wait timeout.")
	private Long asyncWaitTimeout = DEFAULT_ASYNC_WAIT_TIMEOUT;

	/**
	 * Get the threshold for logging slow graphQl queries (in milliseconds)
	 * @return threshold in milliseconds
	 */
	public Long getSlowThreshold() {
		return slowThreshold;
	}

	/**
	 * Set the threshold for logging slow graqhQl queries (in milliseconds)
	 * @param slowThreshold threshold
	 * @return fluent API
	 */
	public GraphQLOptions setSlowThreshold(Long slowThreshold) {
		this.slowThreshold = slowThreshold;
		return this;
	}

	/**
	 * Async wait timeout for graphQl queries (in milliseconds)
	 * @return wait timeout in milliseconds
	 */
	public Long getAsyncWaitTimeout() {
		return asyncWaitTimeout;
	}

	/**
	 * Set the async wait timeout in milliseconds
	 * @param asyncWaitTimeout timeout
	 * @return fluent API
	 */
	public GraphQLOptions setAsyncWaitTimeout(Long asyncWaitTimeout) {
		this.asyncWaitTimeout = asyncWaitTimeout;
		// make sure the value is not set to null
		if (this.asyncWaitTimeout == null) {
			this.asyncWaitTimeout = DEFAULT_ASYNC_WAIT_TIMEOUT;
		}
		return this;
	}
}
