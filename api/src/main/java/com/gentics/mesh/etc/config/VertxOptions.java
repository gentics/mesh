package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Getter;
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

	public static final boolean DEFAULT_ORDERED_BLOCKING_HANDLERS = true;

	public static final String MESH_VERTX_WORKER_POOL_SIZE_ENV = "MESH_VERTX_WORKER_POOL_SIZE";

	public static final String MESH_VERTX_EVENT_POOL_SIZE_ENV = "MESH_VERTX_EVENT_POOL_SIZE";

	public static final String MESH_VERTX_ORDERED_BLOCKING_HANDLERS_ENV = "MESH_VERTX_ORDERED_BLOCKING_HANDLERS";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure worker pool size. Default is: " + DEFAULT_WORKER_POOL_SIZE)
	@EnvironmentVariable(name = MESH_VERTX_WORKER_POOL_SIZE_ENV, description = "Override the configured Vert.x worker pool size.")
	private int workerPoolSize = DEFAULT_WORKER_POOL_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure event pool size. Default is 2 * CPU Cores")
	@EnvironmentVariable(name = MESH_VERTX_EVENT_POOL_SIZE_ENV, description = "Override the configured Vert.x event pool size.")
	private int eventPoolSize = DEFAULT_EVENT_POOL_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure, whether blocking handlers for mutating requests should be ordered. Default is " + DEFAULT_ORDERED_BLOCKING_HANDLERS)
	@EnvironmentVariable(name = MESH_VERTX_ORDERED_BLOCKING_HANDLERS_ENV, description = "Override the configured Vert.x blocking handlers ordering setting.")
	private boolean orderedBlockingHandlers = true;

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
