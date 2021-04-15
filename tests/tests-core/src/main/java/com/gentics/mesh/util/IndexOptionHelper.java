package com.gentics.mesh.util;

import io.vertx.core.json.JsonObject;

public final class IndexOptionHelper {

	public static JsonObject getRawFieldOption() {
		return new JsonObject().put("raw", new JsonObject().put("index", true).put("type", "keyword"));
	}
}
