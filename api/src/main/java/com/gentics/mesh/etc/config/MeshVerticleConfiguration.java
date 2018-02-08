package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.Option;

import io.vertx.core.json.JsonObject;

public class MeshVerticleConfiguration implements Option {

	@JsonProperty(value = "config", required = false)
	@JsonPropertyDescription("Custom verticle configuration.")
	private JsonObject verticleConfig;

	public JsonObject getVerticleConfig() {
		return verticleConfig;
	}

}
