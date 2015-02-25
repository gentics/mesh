package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.rest.response.RestGroup;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.DummyDataProvider;

public class GroupsVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return groupsVerticle;
	}

	@Test
	public void testReadGroupByUUID() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"},\"properties\":{\"filename\":\"english.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime!\"},\"type\":\"content\",\"language\":\"en_US\"}";
		Content content = getDataProvider().getContent();
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/" + content.getUuid(),
				200, "OK");
		assertEqualsSanitizedJson(json, response);
	}

	private void assertEqualsSanitizedJson(String expectedJson, String unsanitizedResponseJson) throws JsonGenerationException, JsonMappingException,
			IOException {
		RestGroup responseObject = new ObjectMapper().readValue(unsanitizedResponseJson, RestGroup.class);
		assertNotNull(responseObject);
		// Update the uuid and compare json afterwards
		responseObject.setUuid("uuid-value");
		String sanitizedJson = new ObjectMapper().writeValueAsString(responseObject);
		assertEquals("The response json did not match the expected one.", expectedJson, sanitizedJson);
	}
}
