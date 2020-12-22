package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Options for the used Vert.x instance.
 */
@GenerateDocumentation
public class VertxOptions implements Option {

	public static final int DEFAULT_WORKER_POOL_SIZE = 20;

	public static final int DEFAULT_EVENT_POOL_SIZE = 2 * Runtime.getRuntime().availableProcessors();

	public static final String MESH_VERTX_WORKER_POOL_SIZE_ENV = "MESH_VERTX_WORKER_POOL_SIZE";

	public static final String MESH_VERTX_EVENT_POOL_SIZE_ENV = "MESH_VERTX_EVENT_POOL_SIZE";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure worker pool size. Default is: " + DEFAULT_WORKER_POOL_SIZE)
	@EnvironmentVariable(name = MESH_VERTX_WORKER_POOL_SIZE_ENV, description = "Override the configured Vert.x worker pool size.")
	private int workerPoolSize = DEFAULT_WORKER_POOL_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure event pool size. Default is 2 * CPU Cores")
	@EnvironmentVariable(name = MESH_VERTX_EVENT_POOL_SIZE_ENV, description = "Override the configured Vert.x event pool size.")
	private int eventPoolSize = DEFAULT_EVENT_POOL_SIZE;

	public int getEventPoolSize() {
		return eventPoolSize;
	}

	@Setter
	public VertxOptions setEventPoolSize(int eventPoolSize) {
		this.eventPoolSize = eventPoolSize;
		return this;
	}

	public int getWorkerPoolSize() {
		return workerPoolSize;
	}

	@Setter
	public VertxOptions setWorkerPoolSize(int workerPoolSize) {
		this.workerPoolSize = workerPoolSize;
		return this;
	}

}
