package com.gentics.cailun.etc;

import io.vertx.core.json.JsonObject;

import org.codehaus.jackson.annotate.JsonProperty;

public class CaiLunVerticleConfiguration {

	@JsonProperty("config")
	private JsonObject verticleConfig;

	public JsonObject getVerticleConfig() {
		return verticleConfig;
	}

}
