package com.gentics.mesh.core.utilities;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public final class SchemaValidationUtil {
	public static final String INVALID_NAME_EMPTY = "";
	public static final String INVALID_NAME_NUMBER = "123";
	public static final String INVALID_NAME_SPACE = "t e s t";
	public static final String INVALID_NAME_UMLAUTE = "testäöü";
	public static final String INVALID_NAME_SPECIAL = "test:?*#+-.,;<>|!\"'§$%&/()=´`~²³{}[]\\ßµ^°@€";

	public static final JsonObject DUMMY_OBJ = new JsonObject().put("test", "hello");
	public static final JsonObject MINIMAL_FIELD = new JsonObject().put("name", "test").put("type", "string");
	public static final JsonObject MINIMAL_SCHEMA = new JsonObject().put("name", "test").put("fields", new JsonArray());
	public static final JsonObject BASE_SCHEMA = MINIMAL_SCHEMA.copy().put("fields", new JsonArray().add(MINIMAL_FIELD));
}
