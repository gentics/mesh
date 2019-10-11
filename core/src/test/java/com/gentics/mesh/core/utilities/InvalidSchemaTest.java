package com.gentics.mesh.core.utilities;

import static com.gentics.mesh.core.utilities.SchemaValidationUtil.DUMMY_OBJ;
import static com.gentics.mesh.core.utilities.SchemaValidationUtil.INVALID_NAME_EMPTY;
import static com.gentics.mesh.core.utilities.SchemaValidationUtil.INVALID_NAME_NUMBER;
import static com.gentics.mesh.core.utilities.SchemaValidationUtil.INVALID_NAME_SPACE;
import static com.gentics.mesh.core.utilities.SchemaValidationUtil.INVALID_NAME_SPECIAL;
import static com.gentics.mesh.core.utilities.SchemaValidationUtil.INVALID_NAME_UMLAUTE;
import static com.gentics.mesh.core.utilities.SchemaValidationUtil.MINIMAL_FIELD;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.handler.impl.HttpStatusException;
import okhttp3.Response;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class InvalidSchemaTest extends AbstractMeshTest {

	@Parameterized.Parameter(0)
	public static JsonObject schema;

	@Parameterized.Parameter(1)
	public static boolean isNode;

	@Parameterized.Parameters(name = "{index}: node: {1}")
	public static Iterable<Object[]> testData() {
		Stream<Object[]> common = Stream.of(true, false)
			.flatMap(isNode -> commonSchemas().stream()
			.map(schema -> new Object[]{schema, isNode}));

		Stream<Object[]> nodeSchemas = nodeSchemasOnly().stream()
			.map(schema -> new Object[]{schema, true});

		return Stream.concat(common, nodeSchemas)::iterator;
	}

	public static List<JsonObject> commonSchemas() {
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
//			.add(MINIMAL_FIELD.copy().put("name", true))
//			.add(MINIMAL_FIELD.copy().put("name", false))
//			.add(MINIMAL_FIELD.copy().put("name", 123))
//			.add(MINIMAL_FIELD.copy().put("name", -123))
//			.add(MINIMAL_FIELD.copy().put("name", 123.456))
			.add(MINIMAL_FIELD.copy().put("name", INVALID_NAME_EMPTY))
//			.add(MINIMAL_FIELD.copy().put("name", INVALID_NAME_NUMBER))
//			.add(MINIMAL_FIELD.copy().put("name", INVALID_NAME_SPACE))
//			.add(MINIMAL_FIELD.copy().put("name", INVALID_NAME_UMLAUTE))
//			.add(MINIMAL_FIELD.copy().put("name", INVALID_NAME_SPECIAL))
			.add(MINIMAL_FIELD.copy().put("name", new JsonArray().add("test")))
			.add(MINIMAL_FIELD.copy().put("name", DUMMY_OBJ))
			//
			// Invalid label property
//			.add(MINIMAL_FIELD.copy().put("label", true))
//			.add(MINIMAL_FIELD.copy().put("label", false))
//			.add(MINIMAL_FIELD.copy().put("label", 123))
//			.add(MINIMAL_FIELD.copy().put("label", -123))
//			.add(MINIMAL_FIELD.copy().put("label", 123.456))
			.add(MINIMAL_FIELD.copy().put("label", new JsonArray().add("test")))
			.add(MINIMAL_FIELD.copy().put("label", DUMMY_OBJ))
			//
			// Invalid allow property
			.add(MINIMAL_FIELD.copy().put("allow", true))
			.add(MINIMAL_FIELD.copy().put("allow", false))
			.add(MINIMAL_FIELD.copy().put("allow", 123))
			.add(MINIMAL_FIELD.copy().put("allow", -123))
			.add(MINIMAL_FIELD.copy().put("allow", 123.456))
			.add(MINIMAL_FIELD.copy().put("allow", "test"))
			.add(MINIMAL_FIELD.copy().put("allow", DUMMY_OBJ))
//			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray().add(true)))
//			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray().add(false)))
//			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray().add(123)))
//			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray().add(-123)))
//			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray().add(123.456)))
//			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray().add("test")))
			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray().add(DUMMY_OBJ)))
			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray().add(new JsonArray().add("test"))))
			//
			// Invalid type property
			.add(MINIMAL_FIELD.copy().put("type", true))
			.add(MINIMAL_FIELD.copy().put("type", false))
			.add(MINIMAL_FIELD.copy().put("type", 123))
			.add(MINIMAL_FIELD.copy().put("type", -123))
			.add(MINIMAL_FIELD.copy().put("type", 123.456))
			.add(MINIMAL_FIELD.copy().put("type", ""))
			.add(MINIMAL_FIELD.copy().put("type", "invalid"))
			.add(MINIMAL_FIELD.copy().put("type", new JsonArray().add("string")))
			.add(MINIMAL_FIELD.copy().put("type", new JsonObject().put("type", "string")))
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
			.add(MINIMAL_FIELD.copy().put("required", 123))
			.add(MINIMAL_FIELD.copy().put("required", -123))
			.add(MINIMAL_FIELD.copy().put("required", 123.456))
			.add(MINIMAL_FIELD.copy().put("required", "test"))
			.add(MINIMAL_FIELD.copy().put("required", DUMMY_OBJ))
			.add(MINIMAL_FIELD.copy().put("required", new JsonArray().add(true)))
			.add(MINIMAL_FIELD.copy().put("required", new JsonArray().add(false)))
			.add(MINIMAL_FIELD.copy().put("required", new JsonArray().add(123)))
			.add(MINIMAL_FIELD.copy().put("required", new JsonArray().add(-123)))
			.add(MINIMAL_FIELD.copy().put("required", new JsonArray().add(123.456)))
			.add(MINIMAL_FIELD.copy().put("required", new JsonArray().add(new JsonArray().add("something"))))
			.add(MINIMAL_FIELD.copy().put("required", new JsonArray().add(DUMMY_OBJ)))
			.forEach(value -> {
				schemas.add(new JsonObject().put("name", "test").put("fields", new JsonArray().add(value)));
			});

		// Duplicate field-names
		schemas.add(new JsonObject().put("name", "test").put("fields",
			new JsonArray().add(new JsonObject().put("name", "test").put("type", "string")).add(
				new JsonObject().put("name", "test").put("type", "number"))));

		return schemas;
	}

	public static List<JsonObject> nodeSchemasOnly() {
		List<JsonObject> schemas = new ArrayList<>();

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
					.put("fields", new JsonArray().add(MINIMAL_FIELD.copy()))
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
					.put("fields", new JsonArray().add(MINIMAL_FIELD.copy()))
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
					.put("fields", new JsonArray().add(MINIMAL_FIELD.copy()))
					.put("urlFields", value));
			});

		return schemas;
	}

	@Test
	public void testSchema() {
		String path = isNode
			? "/api/v2/utilities/validateSchema"
			: "/api/v2/utilities/validateMicroschema";
		try {
			Response r = this.httpPost(path, schema).execute();
			if (r.code() != 200) {
				throw new HttpStatusException(r.code());
			}
			JsonObject obj = new JsonObject(r.body().string());
			assertEquals("Status should be invalid. Schema: " + schema.encodePrettily(), "INVALID", obj.getString("status"));
			assertNotNull(obj.getJsonObject("message"));
		} catch (IOException e) {
			Assert.fail("Error during Request: " + e.getMessage());
		}
	}
}
