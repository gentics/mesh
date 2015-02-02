package com.gentics.cailun.etc;

import org.codehaus.jackson.annotate.JsonProperty;

import io.vertx.core.json.JsonObject;
import lombok.Data;

@Data
public class CaiLunVerticleConfiguration {

	@JsonProperty("config")
	private JsonObject verticleConfig;

}
