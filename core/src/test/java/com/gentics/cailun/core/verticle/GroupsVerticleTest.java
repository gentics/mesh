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
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.rest.response.RestGroup;
import com.gentics.cailun.test.AbstractRestVerticleTest;

public class GroupsVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return groupsVerticle;
	}

	@Test
	public void testReadGroupByUUID() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"admin\"}";
		Group group = getDataProvider().getAdminGroup();
		assertNotNull("The UUID of the group must not be null.", group.getUuid());
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/groups/" + group.getUuid(),
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
