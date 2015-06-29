package com.gentics.mesh.etc;

import io.vertx.core.json.JsonObject;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MeshVerticleConfiguration {

	@JsonProperty("config")
	private JsonObject verticleConfig;

	public JsonObject getVerticleConfig() {
		return verticleConfig;
	}

}
