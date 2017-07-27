package com.gentics.mesh.util;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import io.vertx.core.json.JsonObject;

public final class MeshJSONAssert {

	public static void assertEquals(String expected, JsonObject actual) throws JSONException {
		JSONAssert.assertEquals(expected, actual.toString(), false);
	}

}
