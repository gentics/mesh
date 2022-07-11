package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Options for the vert.x eventbus
 */
@GenerateDocumentation
public class VertxEventBusOptions implements Option {
	public final static int DEFAULT_CHECK_INTERVAL = 30_000;

	public final static int DEFAULT_WARN_THRESHOLD = 60_000;

	public final static int DEFAULT_ERROR_THRESHOLD = 120_000;

	public final static String MESH_VERTX_EVENT_BUS_CHECK_INTERVAL_ENV = "MESH_VERTX_EVENT_BUS_CHECK_INTERVAL";

	public final static String MESH_VERTX_EVENT_BUS_WARN_THRESHOLD_ENV = "MESH_VERTX_EVENT_BUS_WARN_THRESHOLD";

	public final static String MESH_VERTX_EVENT_BUS_ERROR_THRESHOLD_ENV = "MESH_VERTX_EVENT_BUS_ERROR_THRESHOLD";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the check interval for the Vert.x eventBus in ms. If set to a positive value, "
			+ "Mesh will regularly send test events over the eventBus. Default is: " + DEFAULT_CHECK_INTERVAL + " ms.")
	@EnvironmentVariable(name = MESH_VERTX_EVENT_BUS_CHECK_INTERVAL_ENV, description = "Override the Vert.x eventBus check interval in ms.")
	private int checkInterval = DEFAULT_CHECK_INTERVAL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the warn threshold for the Vert.x eventBus check in ms. If this and the check interval are set to positive values, "
			+ "and the last test events was received longer than the configured threshold ago, a warn message will be logged. Default is: "
			+ DEFAULT_WARN_THRESHOLD + " ms.")
	@EnvironmentVariable(name = MESH_VERTX_EVENT_BUS_WARN_THRESHOLD_ENV, description = "Override the Vert.x eventBus warn threshold in ms.")
	private int warnThreshold = DEFAULT_WARN_THRESHOLD;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the error threshold for the Vert.x eventBus check in ms. If this and the check interval are set to positive values, "
			+ "and the last test events was received longer than the configured threshold ago, an error message will be logged, and the liveness of the instance will be set to false. Default is: "
			+ DEFAULT_ERROR_THRESHOLD)
	@EnvironmentVariable(name = MESH_VERTX_EVENT_BUS_ERROR_THRESHOLD_ENV, description = "Override the Vert.x eventBus error threshold in ms.")
	private int errorThreshold = DEFAULT_ERROR_THRESHOLD;

	public int getCheckInterval() {
		return checkInterval;
	}

	public VertxEventBusOptions setCheckInterval(int checkInterval) {
		this.checkInterval = checkInterval;
		return this;
	}

	public int getWarnThreshold() {
		return warnThreshold;
	}

	public VertxEventBusOptions setWarnThreshold(int warnThreshold) {
		this.warnThreshold = warnThreshold;
		return this;
	}

	public int getErrorThreshold() {
		return errorThreshold;
	}

	public VertxEventBusOptions setErrorThreshold(int errorThreshold) {
		this.errorThreshold = errorThreshold;
		return this;
	}

	@Override
	public void validate(MeshOptions options) {
		if (checkInterval > 0) {
			if (warnThreshold > 0 && warnThreshold < checkInterval) {
				throw new IllegalArgumentException("vertxOptions.eventBus.warnThreshold (set to " + warnThreshold
						+ ") must be greater than vertxOptions.eventBus.checkInterval (set to " + checkInterval + ")");
			}
			if (errorThreshold > 0 && errorThreshold < checkInterval) {
				throw new IllegalArgumentException("vertxOptions.eventBus.errorThreshold (set to " + errorThreshold
						+ ") must be greater than vertxOptions.eventBus.checkInterval (set to " + checkInterval + ")");
			}
			if (warnThreshold > 0 && errorThreshold > 0 && (errorThreshold <= warnThreshold)) {
				throw new IllegalArgumentException("vertxOptions.eventBus.errorThreshold (set to " + errorThreshold
						+ ") must be greater than vertxOptions.eventBus.warnThreshold (set to " + warnThreshold + ")");
			}
		}
	}
}
