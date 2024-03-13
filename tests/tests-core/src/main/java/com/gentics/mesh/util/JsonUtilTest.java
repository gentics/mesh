package com.gentics.mesh.util;

import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.test.util.TestUtils.getJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.Charsets;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.example.GraphQLExamples;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;

public class JsonUtilTest {

	@Test(expected = GenericRestException.class)
	public void testToJson() {
		JsonUtil.toJson(new Loop());
	}

	@Test
	public void testJsonEncoding() throws IOException {
		// Reading UTF16BE - The string will be converted to utf8.
		validateEncodingHandling("UTF16BE.json", Charsets.UTF_16BE, "The\uD801\uDC37 Na\uD834\uDD1Em\uD834\uDD1Ee\uD801\uDC37");
		validateEncodingHandling("ISO8859-1.json", Charsets.ISO_8859_1, "³ for ?, ¿ for ?");
		// Reading ISO8859-1 as UTF8 will result in mapping errors. This is expected and can't be avoided if the wrong encoding has been specified.
		validateEncodingHandling("ISO8859-1.json", Charsets.UTF_8, "� for ?, � for ?");
	}

	public void validateEncodingHandling(String name, Charset encoding, String expected) throws IOException {
		String json = getJson(name, encoding);
		json = EncodeUtil.ensureUtf8(json);
		SchemaUpdateRequest schema = JsonUtil.readValue(json, SchemaUpdateRequest.class);
		assertEquals("The name did not match the expected value.", expected, schema.getName());
	}

	@Test
	public void testCompareJson() {
		JsonObject a = new JsonObject();
		a.put("a", "b");
		JsonObject b = new JsonObject();
		b.put("a", "b");
		assertTrue(a.equals(b));

		b.put("a", "c");
		assertFalse(a.equals(b));

		b.put("a", "b");
		assertTrue(a.equals(b));

		a.put("another", "value");
		assertFalse(a.equals(b));
	}

	@Test
	public void testJsonList() {
		ListResponse<UserResponse> list = new ListResponse<>();
		UserResponse user = new UserResponse();
		list.getData().add(user);
		assertNotNull(list.toJson());
	}

	@Test
	public void testGraphQLResponse() {
		GraphQLExamples examples = new GraphQLExamples();
		GraphQLResponse response = examples.createResponse();
		String jsonStr = response.toJson();
		JsonObject json = new JsonObject(jsonStr);
		System.out.println(json.encodePrettily());
		GraphQLResponse response2 = JsonUtil.readValue(jsonStr, GraphQLResponse.class);
		String username = response2.getData().getJsonObject("me").getString("username");
		assertEquals("anonymous", username);
	}

	@Test
	public void testSchema() throws JsonProcessingException {
		String json = JsonUtil.getJsonSchema(NodeResponse.class);
		assertNotNull(json);
	}

	@Test
	public void testMicroschemaAllowField() {
		SchemaUpdateRequest schemaUpdate = new SchemaUpdateRequest();
		schemaUpdate.addField(FieldUtil.createMicronodeFieldSchema("micro").setAllowedMicroSchemas("TestMicroschema"));
		String json = schemaUpdate.toJson();
		SchemaVersionModel model = JsonUtil.readValue(json, SchemaResponse.class);
		assertThat(model.getField("micro", MicronodeFieldSchemaImpl.class).getAllowedMicroSchemas()).containsExactly("TestMicroschema");
	}

	@Test
	public void testPermMap() {
		UserResponse group = new UserResponse();
		PermissionInfo info = new PermissionInfo();
		info.setOthers(false);
		info.set(READ, true);
		info.setCreate(true);
		group.setPermissions(info);
		assertNotNull(group.toJson());
	}

	@Test
	public void testMinify() {
		UserResponse user = new UserResponse();
		String minUser = JsonUtil.toJson(user, true);
		assertEquals(-1, minUser.indexOf("\n"));
		assertEquals(-1, minUser.indexOf("\t"));
		assertEquals(-1, minUser.indexOf(" "));
	}

	@Test
	public void testJsonFormatError() throws IOException {
		try {
			String json = "{broken}";
			JsonUtil.readValue(json, NodeCreateRequest.class);
			fail("json parsing should fail");
		} catch (GenericRestException e) {
			assertEquals("error_json_malformed", e.getI18nKey());
			assertThat(e.getI18nParameters()).containsExactly("1", "3",
				"Unexpected character ('b' (code 98)): was expecting double-quote to start field name");
		}
	}

	@Test
	public void testJsonStructureError() throws IOException {
		try {
			String json = "{\"schema\":\"test\" }";
			JsonUtil.readValue(json, NodeCreateRequest.class);
			fail("json parsing should fail");
		} catch (GenericRestException e) {
			assertEquals("error_json_structure_invalid", e.getI18nKey());
			assertThat(e.getI18nParameters()).contains("1", "11", "schema");
		}
	}

	@Test
	public void testNodeJson() throws JsonParseException, JsonMappingException, IOException {
		NodeResponse node = new NodeResponse();

		StringField stringField = FieldUtil.createStringField("test");
		stringField.setString("testtext");
		FieldMap fields = new FieldMapImpl();
		fields.put("test", stringField);
		node.setFields(fields);

		String json = node.toJson();
		assertNotNull(json);
		System.out.println("From POJO: " + json);

		NodeResponse node2 = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(node2);
		node2.getFields().put("extra", FieldUtil.createBooleanField(false));
		String json2 = node2.toJson();
		System.out.println("From Deserialized POJO: " + json2);

		StringField field = node2.getFields().getStringField("test");
		assertNotNull(field);
		assertEquals("testtext", field.getString());
	}
}

class Loop {
	Loop loop;

	public Loop() {
		loop = this;
	}

	public Loop getLoop() {
		return loop;
	}
}
