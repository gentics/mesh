package com.gentics.mesh.util;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;

public class JsonUtilTest {

	@Test(expected = GenericRestException.class)
	public void testToJson() {
		JsonUtil.toJson(new Loop());
	}

	@Test
	public void testJsonList() {
		ListResponse<UserResponse> list = new ListResponse<>();
		UserResponse user = new UserResponse();
		list.getData().add(user);
		String json = JsonUtil.toJson(list);
		assertNotNull(json);
	}

	@Test
	public void testJsonFormatError() throws IOException {
		try {
			String json = "{broken}";
			JsonUtil.readValue(json, NodeCreateRequest.class);
			fail("json parsing should fail");
		} catch (GenericRestException e) {
			assertEquals("error_json_malformed", e.getMessage());
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
			assertEquals("error_json_structure_invalid", e.getMessage());
			assertThat(e.getI18nParameters()).containsExactly("1", "2", "schema");
		}
	}

	@Test
	public void testNodeJson() throws JsonParseException, JsonMappingException, IOException {
		NodeResponse node = new NodeResponse();

		StringField stringField = FieldUtil.createStringField("test");
		stringField.setString("testtext");
		node.getFields().put("test", stringField);

		String json = JsonUtil.toJson(node);
		assertNotNull(json);
		System.out.println("From POJO: " + json);

		NodeResponse node2 = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(node2);
		node2.getFields().put("extra", FieldUtil.createBooleanField(false));
		String json2 = JsonUtil.toJson(node2);
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
