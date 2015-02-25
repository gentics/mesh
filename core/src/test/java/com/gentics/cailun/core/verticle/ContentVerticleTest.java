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
import com.gentics.cailun.core.rest.response.RestGenericContent;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.DummyDataProvider;

public class ContentVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	ContentVerticle verticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testCreateContentByPath() throws Exception {
		String responseJson = "{\"uuid\":\"uuid-value\",\"author\":null,\"properties\":{\"filename\":\"new-page.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime again!\"},\"type\":\"content\",\"language\":\"en_US\"}";
		String requestJson = "{\"type\": \"content\",\"properties\":{\"filename\":\"new-page.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime again!\"},\"language\":\"en_US\"}";
		String response = testAuthenticatedRequest(HttpMethod.POST, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/subtag/newpage.html",
				200, "OK", requestJson);
		assertEqualsSanitizedJson(responseJson, response);
	}

	private void assertEqualsSanitizedJson(String expectedJson, String unsanitizedResponseJson) throws JsonGenerationException, JsonMappingException,
			IOException {
		RestGenericContent responseObject = new ObjectMapper().readValue(unsanitizedResponseJson, RestGenericContent.class);
		assertNotNull(responseObject);
		// Update the uuid and compare json afterwards
		responseObject.setUuid("uuid-value");
		String sanitizedJson = new ObjectMapper().writeValueAsString(responseObject);
		assertEquals("The response json did not match the expected one.", expectedJson, sanitizedJson);
	}

	@Test
	public void testReadContentByValidPath() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"},\"properties\":{\"filename\":\"english.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime!\"},\"type\":\"content\",\"language\":\"en_US\"}";
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/subtag/english.html",
				200, "OK");
		assertEqualsSanitizedJson(json, response);

	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String json = "{\"message\":\"Content not found for path {subtag/subtag2/no-valid-page.html}\"}";
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME
				+ "/contents/subtag/subtag2/no-valid-page.html", 404, "Not Found");
		assertEquals(json, response);

	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String json = "{\"message\":\"Content not found for path {subtag/subtag-no-valid-tag/no-valid-page.html}\"}";
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME
				+ "/contents/subtag/subtag-no-valid-tag/no-valid-page.html", 404, "Not Found");
		assertEquals(json, response);

	}

	@Test
	public void testReadContentByUUID() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"},\"properties\":{\"filename\":\"english.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime!\"},\"type\":\"content\",\"language\":\"en_US\"}";
		Content content = getDataProvider().getContent();
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/" + content.getUuid(),
				200, "OK");
		assertEqualsSanitizedJson(json, response);

	}

	@Test
	public void testReadContentByBogusUUID() throws Exception {
		String json = "{\"message\":\"Content not found for path {bogusUUID}\"}";
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/bogusUUID", 404,
				"Not Found");
		assertEquals(json, response);

	}

	@Test
	public void testReadContentByInvalidUUID() throws Exception {
		String json = "{\"message\":\"Content not found for uuid {dde8ba06bb7211e4897631a9ce2772f5}\"}";
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME
				+ "/contents/dde8ba06bb7211e4897631a9ce2772f5", 404, "Not Found");
		assertEquals(json, response);
	}

}
