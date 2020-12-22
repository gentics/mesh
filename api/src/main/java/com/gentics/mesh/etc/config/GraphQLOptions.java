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

	public static final String MESH_GRAPHQL_SLOW_THRESHOLD_ENV = "MESH_GRAPHQL_SLOW_THRESHOLD";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Threshold for logging slow graphql queries. Default: " + DEFAULT_SLOW_THRESHOLD + "ms")
	@EnvironmentVariable(name = MESH_GRAPHQL_SLOW_THRESHOLD_ENV, description = "Override the configured slow graphQl query threshold.")
	private Long slowThreshold = DEFAULT_SLOW_THRESHOLD;

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
}
