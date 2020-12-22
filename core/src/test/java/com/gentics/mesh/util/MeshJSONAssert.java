package com.gentics.mesh.util;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import io.vertx.core.json.JsonObject;

/**
 * Asserter for JSON data.
 */
public final class MeshJSONAssert {

	/**
	 * Assert the json string with the json object.
	 * 
	 * @param expected
	 * @param actual
	 * @throws JSONException
	 */
	public static void assertEquals(String expected, JsonObject actual) throws JSONException {
		JSONAssert.assertEquals(expected, actual.toString(), false);
	}

}
