package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;
import io.vertx.core.http.HttpServerOptions;

/**
 * Options for the used Vert.x instance.
 */
@GenerateDocumentation
public class VertxOptions implements Option {

	public static final int DEFAULT_WORKER_POOL_SIZE = 20;

	public static final int DEFAULT_EVENT_POOL_SIZE = 2 * Runtime.getRuntime().availableProcessors();

	public static final int DEFAULT_MAX_HEADER_SIZE = HttpServerOptions.DEFAULT_MAX_HEADER_SIZE;

	public static final boolean DEFAULT_ORDERED_BLOCKING_HANDLERS = true;

	public static final String MESH_VERTX_WORKER_POOL_SIZE_ENV = "MESH_VERTX_WORKER_POOL_SIZE";

	public static final String MESH_VERTX_EVENT_POOL_SIZE_ENV = "MESH_VERTX_EVENT_POOL_SIZE";

	public static final String MESH_VERTX_ORDERED_BLOCKING_HANDLERS_ENV = "MESH_VERTX_ORDERED_BLOCKING_HANDLERS";

	public static final String MESH_VERTX_MAX_HEADER_SIZE_ENV = "MESH_VERTX_MAX_HEADER_SIZE";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure worker pool size.")
	@EnvironmentVariable(name = MESH_VERTX_WORKER_POOL_SIZE_ENV, description = "Override the configured Vert.x worker pool size.")
	private int workerPoolSize = DEFAULT_WORKER_POOL_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure event pool size.")
	@EnvironmentVariable(name = MESH_VERTX_EVENT_POOL_SIZE_ENV, description = "Override the configured Vert.x event pool size.")
	private int eventPoolSize = DEFAULT_EVENT_POOL_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure, whether blocking handlers for mutating requests should be ordered.")
	@EnvironmentVariable(name = MESH_VERTX_ORDERED_BLOCKING_HANDLERS_ENV, description = "Override the configured Vert.x blocking handlers ordering setting.")
	private boolean orderedBlockingHandlers = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the max http header size for all incoming requests.")
	@EnvironmentVariable(name = MESH_VERTX_MAX_HEADER_SIZE_ENV, description = "Override the max http header size for all incoming requests.")
	private int maxHeaderSize = DEFAULT_MAX_HEADER_SIZE;

	@JsonProperty("eventBus")
	@JsonPropertyDescription("EventBus options")
	private VertxEventBusOptions eventBusOptions = new VertxEventBusOptions();

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

	@Getter
	public boolean isOrderedBlockingHandlers() {
		return orderedBlockingHandlers;
	}

	@Setter
	public VertxOptions setOrderedBlockingHandlers(boolean orderedBlockingHandlers) {
		this.orderedBlockingHandlers = orderedBlockingHandlers;
		return this;
	}

	@Getter
	public int getMaxHeaderSize() {
		return maxHeaderSize;
	}

	@Setter
	public VertxOptions setMaxHeaderSize(int maxHeaderSize) {
		this.maxHeaderSize = maxHeaderSize;
		return this;
	}

	public VertxEventBusOptions getEventBusOptions() {
		return eventBusOptions;
	}

	public VertxOptions setEventBusOptions(VertxEventBusOptions eventbusCheckOptions) {
		this.eventBusOptions = eventbusCheckOptions;
		if (this.eventBusOptions == null) {
			this.eventBusOptions = new VertxEventBusOptions();
		}
		return this;
	}

	@Override
	public void validate(MeshOptions options) {
		if (getEventBusOptions() != null) {
			getEventBusOptions().validate(options);
		}
	}
}
