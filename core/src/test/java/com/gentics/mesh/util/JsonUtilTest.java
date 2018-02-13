package com.gentics.mesh.util;

import static com.gentics.mesh.core.rest.common.Permission.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.schema.SchemaModel;
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
		SchemaModel model = JsonUtil.readValue(json, SchemaResponse.class);
		assertThat(model.getField("micro", MicronodeFieldSchemaImpl.class).getAllowedMicroSchemas()).containsExactly("TestMicroschema");
	}

	@Test
	public void testPermMap() {
		UserResponse group = new UserResponse();
		group.getPermissions().setOthers(false);
		group.getPermissions().set(READ, true);
		group.getPermissions().setCreate(true);
		assertNotNull(group.toJson());
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
		node.getFields().put("test", stringField);

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
