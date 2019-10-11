package com.gentics.mesh.core.utilities;

import static com.gentics.mesh.core.utilities.SchemaValidationUtil.BASE_SCHEMA;
import static com.gentics.mesh.core.utilities.SchemaValidationUtil.MINIMAL_FIELD;
import static com.gentics.mesh.core.utilities.SchemaValidationUtil.MINIMAL_SCHEMA;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.util.StreamUtil.mergeStreams;
import static org.junit.Assert.assertEquals;

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
public class ValidSchemaTest extends AbstractMeshTest {
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

		Stream<Object[]> micronodeSchemas = micronodeSchemasOnly().stream()
			.map(schema -> new Object[]{schema, false});

		return mergeStreams(common, nodeSchemas, micronodeSchemas)::iterator;
	}

	public static List<JsonObject> commonSchemas() {
		List<JsonObject> schemas = new ArrayList<>();

		// Minimal Schema
		schemas.add(BASE_SCHEMA.copy());

		// Different schema-names
		schemas.add(BASE_SCHEMA.copy().put("name", "very_long_schema_name_with_underscores"));
		schemas.add(BASE_SCHEMA.copy().put("name", "veryLongSchemaNameWithoutUnderscores"));
		schemas.add(BASE_SCHEMA.copy().put("name", "nameWithNumbers1234567890"));



		// Field Properties
		new JsonArray()
			.add(MINIMAL_FIELD.copy().put("label", "hello world"))
			.add(MINIMAL_FIELD.copy().put("required", true))
			.add(MINIMAL_FIELD.copy().put("required", false))
			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray()))
			.add(MINIMAL_FIELD.copy().put("allow", new JsonArray().add("something")))
			.forEach(field -> {
				schemas.add(MINIMAL_SCHEMA.copy().put("fields", new JsonArray().add(field)));
			});

		// Description
		schemas.add(BASE_SCHEMA.copy().put("description",
			"literally anything 1234567890!\"\\§$%&/()=?`´ß²³@€,.-+/;:*'äöü\n\t~µ<>|"));



		// Versions
		schemas.add(BASE_SCHEMA.copy().put("version", "0.1"));
		schemas.add(BASE_SCHEMA.copy().put("version", "1.0"));
		schemas.add(BASE_SCHEMA.copy().put("version", "1.1"));

		return schemas;
	}

	public static List<JsonObject> nodeSchemasOnly() {
		JsonArray listTypes = new JsonArray()
			.add("string")
			.add("number")
			.add("date")
			.add("boolean")
			.add("html")
			.add("micronode")
			.add("node");
		JsonArray types = listTypes.copy()
			.add("list")
			.add("binary");
		List<JsonObject> schemas = new ArrayList<>();

		// Field-Types
		types.stream()
			.filter(type -> !type.equals("list"))
			.forEach(type -> {
				schemas.add(MINIMAL_SCHEMA.copy().put("fields",
					new JsonArray().add(new JsonObject().put("name", "test").put("type", type))));
			});

		listTypes.forEach(type -> {
			schemas.add(MINIMAL_SCHEMA.copy().put("fields", new JsonArray()
				.add(new JsonObject().put("name", "test").put("type", "list").put("listType", type))));
		});

		// Container
		schemas.add(BASE_SCHEMA.copy().put("container", true));
		schemas.add(BASE_SCHEMA.copy().put("container", false));

		// DisplayField
		schemas.add(BASE_SCHEMA.copy().put("displayField", "test"));

		// SegmentField
		schemas.add(BASE_SCHEMA.copy().put("segmentField", "test"));

		// URL-Fields
		schemas.add(BASE_SCHEMA.copy().put("urlFields", new JsonArray().add("test")));

		return schemas;
	}

	public static List<JsonObject> micronodeSchemasOnly() {
		JsonArray listTypes = new JsonArray()
			.add("string")
			.add("number")
			.add("date")
			.add("boolean")
			.add("html")
			.add("node");
		JsonArray types = listTypes.copy()
			.add("list");

		List<JsonObject> schemas = new ArrayList<>();

		// Field-Types
		types.stream()
			.filter(type -> !type.equals("list"))
			.forEach(type -> {
				schemas.add(MINIMAL_SCHEMA.copy().put("fields",
					new JsonArray().add(new JsonObject().put("name", "test").put("type", type))));
			});

		listTypes.forEach(type -> {
			schemas.add(MINIMAL_SCHEMA.copy().put("fields", new JsonArray()
				.add(new JsonObject().put("name", "test").put("type", "list").put("listType", type))));
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
			assertEquals("Status should be valid. Schema: " + schema.encodePrettily(), "VALID", obj.getString("status"));
		} catch (IOException e) {
			Assert.fail("Error during Request: " + e.getMessage());
		}
	}
}
