package com.gentics.mesh.plugin.forms.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Options for the post processing of published form data
 */
public class PostProcessingOptions implements Option {
	public static final int DEFAULT_MAX_ATTEMPTS = 3;

	public static final int DEFAULT_RETRY_INTERVAL = 300;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional properties for post processing actions. Maps the key of the action to its properties.")
	private Map<String, Map<String, Object>> actions = new HashMap<>();

	@JsonProperty(required = false, defaultValue = "" + DEFAULT_MAX_ATTEMPTS)
	@JsonPropertyDescription("Maximum number of attempts for performing a post processing action, before it is marked as failed.")
	private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

	@JsonProperty(required = false, defaultValue = "" + DEFAULT_RETRY_INTERVAL)
	@JsonPropertyDescription("Interval in seconds for retrying failed and resuming interrupted post processing actions.")
	private int retryInterval = DEFAULT_RETRY_INTERVAL;

	public Map<String, Map<String, Object>> getActions() {
		return actions;
	}

	public PostProcessingOptions setActions(Map<String, Map<String, Object>> actions) {
		this.actions = actions;
		return this;
	}

	/**
	 * Get the properties for the action with given key
	 * @param key action key
	 * @return properties (empty map if nothing configured)
	 */
	public Map<String, Object> getProperties(String key) {
		if (actions == null) {
			return Collections.emptyMap();
		}
		Map<String, Object> properties = actions.get(key);
		return properties != null ? Collections.unmodifiableMap(properties) : Collections.emptyMap();
	}

	public int getMaxAttempts() {
		return maxAttempts;
	}

	public PostProcessingOptions setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
		return this;
	}

	public int getRetryInterval() {
		return retryInterval;
	}

	public PostProcessingOptions setRetryInterval(int retryInterval) {
		this.retryInterval = retryInterval;
		return this;
	}
}
