package com.gentics.mesh.core.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.impl.HttpStatusException;
import okhttp3.Response;

public class AbstractValidateSchemaTest extends AbstractMeshTest {

	public static final String INVALID_NAME_EMPTY = "";
	public static final String INVALID_NAME_NUMBER = "123";
	public static final String INVALID_NAME_SPACE = "t e s t";
	public static final String INVALID_NAME_UMLAUTE = "testäöü";
	public static final String INVALID_NAME_SPECIAL = "test:?*#+-.,;<>|!\"'§$%&/()=´`~²³{}[]\\ßµ^°@€";

	public static final JsonObject DUMMY_OBJ = new JsonObject().put("test", "hello");

	private final String path;
	private final boolean isNode;
	private final JsonArray types;

	public AbstractValidateSchemaTest(String path, boolean isNode) {
		this.path = path;
		this.isNode = isNode;
		if (this.isNode) {
			this.types = new JsonArray()
					.add("string")
					.add("number")
					.add("date")
					.add("boolean")
					.add("html")
					.add("micronode")
					.add("node")
					.add("list")
					.add("binary");
		} else {
			this.types = new JsonArray()
					.add("string")
					.add("number")
					.add("date")
					.add("boolean")
					.add("html")
					.add("node")
					.add("list")
					.add("binary");
		}
	}

	@Test
	public void testInvalidSchemas() {
		List<JsonObject> schemas = new ArrayList<>();

		// Missing required properties
		schemas.add(new JsonObject());
		schemas.add(new JsonObject().put("bla", "foo"));
		schemas.add(new JsonObject().put("fields", new JsonArray()));
		schemas.add(new JsonObject().put("name", "test"));

		// Invalid Name
		new JsonArray()
				.add(true)
				.add(false)
				.add(123)
				.add(-123)
				.add(123.456)
				.add(INVALID_NAME_EMPTY)
				.add(INVALID_NAME_NUMBER)
				.add(INVALID_NAME_SPACE)
				.add(INVALID_NAME_UMLAUTE)
				.add(INVALID_NAME_SPECIAL)
				.add(new JsonArray().add("test"))
				.add(DUMMY_OBJ)
				.forEach(value -> {
					schemas.add(new JsonObject().put("name", value).put("fields", new JsonArray()));
				});

		// Invalid Fields-Property
		new JsonArray()
				.add(true)
				.add(false)
				.add(123)
				.add(-123)
				.add(123.456)
				.add("test")
				.add(DUMMY_OBJ)
				.add(new JsonArray().add(true))
				.add(new JsonArray().add(false))
				.add(new JsonArray().add(123))
				.add(new JsonArray().add(-123))
				.add(new JsonArray().add(123.456))
				.add(new JsonArray().add("test"))
				.forEach(value -> {
					schemas.add(new JsonObject().put("name", "test").put("fields", value));
				});

		// Invalid Description
		new JsonArray()
				.add(true)
				.add(false)
				.add(123)
				.add(-123)
				.add(123.456)
				.add(new JsonArray().add("test"))
				.add(DUMMY_OBJ)
				.forEach(value -> {
					schemas.add(
						new JsonObject().put("name", "test").put("fields", new JsonArray()).put("description", value));
				});

		JsonObject minimalField = new JsonObject().put("name", "test").put("type", "string");
		JsonObject listField = new JsonObject().put("name", "test").put("type", "list");

		// Invalid Fields-Entries
		new JsonArray()
				// 
				// Invalid Objects/Missing name property 
				.add(new JsonObject().put("something", "no name"))
				.add(new JsonObject().put("name", "test"))
				.add(new JsonObject().put("type", "string"))
				//
				// Invalid name property
				.add(minimalField.copy().put("name", true))
				.add(minimalField.copy().put("name", false))
				.add(minimalField.copy().put("name", 123))
				.add(minimalField.copy().put("name", -123))
				.add(minimalField.copy().put("name", 123.456))
				.add(minimalField.copy().put("name", INVALID_NAME_EMPTY))
				.add(minimalField.copy().put("name", INVALID_NAME_NUMBER))
				.add(minimalField.copy().put("name", INVALID_NAME_SPACE))
				.add(minimalField.copy().put("name", INVALID_NAME_UMLAUTE))
				.add(minimalField.copy().put("name", INVALID_NAME_SPECIAL))
				.add(minimalField.copy().put("name", new JsonArray().add("test")))
				.add(minimalField.copy().put("name", DUMMY_OBJ))
				//
				// Invalid label property
				.add(minimalField.copy().put("label", true))
				.add(minimalField.copy().put("label", false))
				.add(minimalField.copy().put("label", 123))
				.add(minimalField.copy().put("label", -123))
				.add(minimalField.copy().put("label", 123.456))
				.add(minimalField.copy().put("label", new JsonArray().add("test")))
				.add(minimalField.copy().put("label", DUMMY_OBJ))
				//
				// Invalid allow property
				.add(minimalField.copy().put("allow", true))
				.add(minimalField.copy().put("allow", false))
				.add(minimalField.copy().put("allow", 123))
				.add(minimalField.copy().put("allow", -123))
				.add(minimalField.copy().put("allow", 123.456))
				.add(minimalField.copy().put("allow", "test"))
				.add(minimalField.copy().put("allow", DUMMY_OBJ))
				.add(minimalField.copy().put("allow", new JsonArray().add(true)))
				.add(minimalField.copy().put("allow", new JsonArray().add(false)))
				.add(minimalField.copy().put("allow", new JsonArray().add(123)))
				.add(minimalField.copy().put("allow", new JsonArray().add(-123)))
				.add(minimalField.copy().put("allow", new JsonArray().add(123.456)))
				.add(minimalField.copy().put("allow", new JsonArray().add("test")))
				.add(minimalField.copy().put("allow", new JsonArray().add(DUMMY_OBJ)))
				.add(minimalField.copy().put("allow", new JsonArray().add(new JsonArray().add("test"))))
				//
				// Invalid type property
				.add(minimalField.copy().put("type", true))
				.add(minimalField.copy().put("type", false))
				.add(minimalField.copy().put("type", 123))
				.add(minimalField.copy().put("type", -123))
				.add(minimalField.copy().put("type", 123.456))
				.add(minimalField.copy().put("type", ""))
				.add(minimalField.copy().put("type", "invalid"))
				.add(minimalField.copy().put("type", new JsonArray().add("string")))
				.add(minimalField.copy().put("type", new JsonObject().put("type", "string")))
				//
				// Invalid listType property
				.add(listField.copy().put("listType", true))
				.add(listField.copy().put("listType", false))
				.add(listField.copy().put("listType", 123))
				.add(listField.copy().put("listType", -123))
				.add(listField.copy().put("listType", 123.456))
				.add(listField.copy().put("listType", ""))
				.add(listField.copy().put("listType", "invalid"))
				.add(listField.copy().put("listType", new JsonArray().add("string")))
				.add(listField.copy().put("listType", DUMMY_OBJ))
				//
				// Invalid required property
				.add(minimalField.copy().put("required", true))
				.add(minimalField.copy().put("required", false))
				.add(minimalField.copy().put("required", 123))
				.add(minimalField.copy().put("required", -123))
				.add(minimalField.copy().put("required", 123.456))
				.add(minimalField.copy().put("required", "test"))
				.add(minimalField.copy().put("required", DUMMY_OBJ))
				.add(minimalField.copy().put("required", new JsonArray().add(true)))
				.add(minimalField.copy().put("required", new JsonArray().add(false)))
				.add(minimalField.copy().put("required", new JsonArray().add(123)))
				.add(minimalField.copy().put("required", new JsonArray().add(-123)))
				.add(minimalField.copy().put("required", new JsonArray().add(123.456)))
				.add(minimalField.copy().put("required", new JsonArray().add(new JsonArray().add("something"))))
				.add(minimalField.copy().put("required", new JsonArray().add(DUMMY_OBJ)))
				.forEach(value -> {
					schemas.add(new JsonObject().put("name", "test").put("fields", new JsonArray().add(value)));
				});

		// Duplicate field-names
		schemas.add(new JsonObject().put("name", "test").put("fields",
			new JsonArray().add(new JsonObject().put("name", "test").put("type", "string")).add(
				new JsonObject().put("name", "test").put("type", "number"))));

		if (this.isNode) {
			// Invalid Container
			new JsonArray()
					.add(new JsonArray())
					.add(DUMMY_OBJ)
					.add(123)
					.add(-123)
					.add(123.456)
					.add("test")
					.add(new JsonArray().add("test"))
					.add(DUMMY_OBJ)
					.forEach(value -> {
						schemas.add(new JsonObject().put("name", "test").put("fields", new JsonArray()).put("container",
							value));
					});

			// Invalid DisplayField
			new JsonArray()
					.add(true)
					.add(false)
					.add(123)
					.add(-123)
					.add(123.456)
					.add(INVALID_NAME_EMPTY)
					.add(INVALID_NAME_NUMBER)
					.add(INVALID_NAME_SPACE)
					.add(INVALID_NAME_UMLAUTE)
					.add(INVALID_NAME_SPECIAL)
					.add("something")
					.add(new JsonArray().add("test"))
					.add(DUMMY_OBJ)
					.forEach(value -> {
						schemas.add(new JsonObject()
								.put("name", "test")
								.put("fields", new JsonArray().add(minimalField.copy()))
								.put("displayField", value));
					});

			// Invalid SegmentField
			new JsonArray()
					.add(true)
					.add(false)
					.add(123)
					.add(-123)
					.add(123.456)
					.add(INVALID_NAME_EMPTY)
					.add(INVALID_NAME_NUMBER)
					.add(INVALID_NAME_SPACE)
					.add(INVALID_NAME_UMLAUTE)
					.add(INVALID_NAME_SPECIAL)
					.add("something")
					.add(new JsonArray().add("test"))
					.add(DUMMY_OBJ)
					.forEach(value -> {
						schemas.add(new JsonObject()
								.put("name", "test")
								.put("fields", new JsonArray().add(minimalField.copy()))
								.put("segmentField", value));
					});

			// Invalid URL-Fields
			new JsonArray()
					.add(true)
					.add(false)
					.add(123)
					.add(-123)
					.add(123.456)
					.add(INVALID_NAME_EMPTY)
					.add(INVALID_NAME_NUMBER)
					.add(INVALID_NAME_SPACE)
					.add(INVALID_NAME_UMLAUTE)
					.add(INVALID_NAME_SPECIAL)
					.add("something")
					.add(new JsonArray().add("something"))
					.add(DUMMY_OBJ)
					.add(new JsonArray().add(true))
					.add(new JsonArray().add(false))
					.add(new JsonArray().add(123))
					.add(new JsonArray().add(-123))
					.add(new JsonArray().add(123.456))
					.add(new JsonArray().add(INVALID_NAME_EMPTY))
					.add(new JsonArray().add(INVALID_NAME_NUMBER))
					.add(new JsonArray().add(INVALID_NAME_SPACE))
					.add(new JsonArray().add(INVALID_NAME_UMLAUTE))
					.add(new JsonArray().add(INVALID_NAME_SPECIAL))
					.add(new JsonArray().add("something"))
					.add(new JsonArray().add(new JsonArray().add("something")))
					.add(new JsonArray().add(DUMMY_OBJ))
					.forEach(value -> {
						schemas.add(new JsonObject()
								.put("name", "test")
								.put("fields", new JsonArray().add(minimalField.copy()))
								.put("urlFields", value));
					});
		}

		// Invalid Version
		new JsonArray()
				.add(true)
				.add(false)
				.add(123)
				.add(-123)
				.add(123.456)
				.add("")
				.add("hello")
				.add("-1.0")
				.add("1")
				.add("1.0.0")
				.add("1.#")
				.add("1,0")
				.add("1.-1")
				.add(new JsonArray().add("1.0"))
				.add(DUMMY_OBJ)
				.forEach(value -> {
					schemas.add(
						new JsonObject().put("name", "test").put("fields", new JsonArray()).put("version", value));
				});

		// Execute tests
		for (JsonObject schema : schemas) {
			try {
				Response r = this.httpPost(this.path, schema).execute();
				if (r.code() != 200) {
					throw new HttpStatusException(r.code());
				}
				JsonObject obj = new JsonObject(r.body().string());
				assertEquals("Status should be invalid", obj.getString("status"), "INVALID");
				assertNotNull(obj.getJsonObject("message"));
			} catch (IOException e) {
				Assert.fail("Error during Request: " + e.getMessage());
			}
		}
	}

	@Test
	public void testValidSchemas() {
		List<JsonObject> schemas = new ArrayList<>();

		JsonObject minimal = new JsonObject().put("name", "test").put("fields", new JsonArray());
		JsonObject minimalField = new JsonObject().put("name", "test").put("type", "string");
		JsonObject base = minimal.copy().put("fields", new JsonArray().add(minimalField));

		// Minimal Schema
		schemas.add(minimal.copy());

		// Different schema-names
		schemas.add(minimal.copy().put("name", "very_long_schema_name_with_underscores"));
		schemas.add(minimal.copy().put("name", "veryLongSchemaNameWithoutUnderscores"));
		schemas.add(minimal.copy().put("name", "nameWithNumbers1234567890"));

		// Field-Types
		this.types.forEach(type -> {
			schemas.add(minimal.copy().put("fields",
				new JsonArray().add(new JsonObject().put("name", "test").put("type", type))));
			if (!type.equals("list")) {
				schemas.add(minimal.copy().put("fields", new JsonArray()
						.add(new JsonObject().put("name", "test").put("type", "list").put("listType", type))));
			}
		});

		// Field Properties
		new JsonArray()
				.add(minimalField.copy().put("label", "hello world"))
				.add(minimalField.copy().put("required", true))
				.add(minimalField.copy().put("required", false))
				.add(minimalField.copy().put("allow", new JsonArray()))
				.add(minimalField.copy().put("allow", new JsonArray().add("something")))
				.forEach(field -> {
					schemas.add(minimal.copy().put("fields", new JsonArray().add(field)));
				});

		// Description
		schemas.add(minimal.copy().put("description",
			"literally anything 1234567890!\"\\§$%&/()=?`´ß²³@€,.-+/;:*'äöü\n\t~µ<>|"));

		if (this.isNode) {
			// Container
			schemas.add(minimal.copy().put("container", true));
			schemas.add(minimal.copy().put("container", false));

			// DisplayField
			schemas.add(base.copy().put("displayField", "test"));

			// SegmentField
			schemas.add(base.copy().put("segmentField", "test"));

			// URL-Fields
			schemas.add(base.copy().put("urlFields", new JsonArray().add("test")));
		}

		// Versions
		schemas.add(base.copy().put("version", "0.1"));
		schemas.add(base.copy().put("version", "1.0"));
		schemas.add(base.copy().put("version", "1.1"));

		// Execute tests
		for (JsonObject schema : schemas) {
			try {
				Response r = this.httpPost(this.path, schema).execute();
				if (r.code() != 200) {
					throw new HttpStatusException(r.code());
				}
				JsonObject obj = new JsonObject(r.body().string());
				assertEquals("Status should be valid", obj.getString("status"), "VALID");
				assertNotNull(obj.getJsonObject("message"));
			} catch (IOException e) {
				Assert.fail("Error during Request: " + e.getMessage());
			}
		}
	}
}
