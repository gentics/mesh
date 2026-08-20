package com.gentics.mesh.core.rest.job.warning;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a job warning.
 */
public class JobWarning implements RestModel {

	private String type;

	private String message;

	private Map<String, String> properties = new HashMap<>();

	public String getType() {
		return type;
	}

	public JobWarning setType(String type) {
		this.type = type;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public JobWarning setMessage(String message) {
		this.message = message;
		return this;
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public JobWarning setProperties(Map<String, String> properties) {
		this.properties = properties;
		return this;
	}

}
