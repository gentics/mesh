package com.gentics.mesh.etc.config;

import io.vertx.core.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class MeshVerticleConfiguration {

	@JsonProperty(value = "config", required = false)
	@JsonPropertyDescription("Custom verticle configuration.")
	private JsonObject verticleConfig;

	public JsonObject getVerticleConfig() {
		return verticleConfig;
	}

}
