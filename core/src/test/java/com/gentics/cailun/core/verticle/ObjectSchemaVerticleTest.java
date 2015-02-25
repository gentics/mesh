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

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.rest.response.RestObjectSchema;
import com.gentics.cailun.core.rest.response.RestPropertyTypeSchema;
import com.gentics.cailun.test.AbstractProjectRestVerticleTest;
import com.gentics.cailun.test.DummyDataProvider;

public class ObjectSchemaVerticleTest extends AbstractProjectRestVerticleTest {

	@Autowired
	private ObjectSchemaVerticle objectSchemaVerticle;

	@Override
	public AbstractProjectRestVerticle getVerticle() {
		return objectSchemaVerticle;
	}

	@Test
	public void testReadSchemaByName() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"content\",\"description\":\"Default schema for contents\",\"propertyTypeSchemas\":[{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"content\",\"desciption\":null},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"filename\",\"desciption\":null},{\"uuid\":\"uuid-value\",\"type\":\"i18n-string\",\"key\":\"name\",\"desciption\":null}]}";
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/types/content", 200, "OK");
		assertEqualsSanitizedJson(json, response);
	}

	private void assertEqualsSanitizedJson(String expectedJson, String unsanitizedResponseJson) throws JsonGenerationException, JsonMappingException,
			IOException {
		RestObjectSchema responseObject = new ObjectMapper().readValue(unsanitizedResponseJson, RestObjectSchema.class);
		assertNotNull(responseObject);
		// Update the uuid and compare json afterwards
		responseObject.setUuid("uuid-value");
		for (RestPropertyTypeSchema schema : responseObject.getPropertyTypeSchemas()) {
			schema.setUuid("uuid-value");
		}
		String sanitizedJson = new ObjectMapper().writeValueAsString(responseObject);
		assertEquals("The response json did not match the expected one.", expectedJson, sanitizedJson);
	}

}
