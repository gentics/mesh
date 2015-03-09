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
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.TestDataProvider;
import com.gentics.cailun.test.UserInfo;

public class ContentVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	ContentVerticle verticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testCreateContentByPath() throws Exception {
		UserInfo info = data().getUserInfo();
		String requestJson = "{\"type\": \"content\",\"properties\":{\"filename\":\"new-page.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime again!\"},\"language\":\"en_US\"}";
		String response = request(info, HttpMethod.POST, "/api/v1/" + TestDataProvider.PROJECT_NAME + "/contents/subtag/newpage.html", 200, "OK",
				requestJson);
		String responseJson = "{\"uuid\":\"uuid-value\",\"author\":null,\"properties\":{\"filename\":\"new-page.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime again!\"},\"type\":\"content\",\"language\":\"en_US\"}";
		assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);
	}

	//
	// private void assertEqualsSanitizedJson(String expectedJson, String unsanitizedResponseJson) throws JsonGenerationException, JsonMappingException,
	// IOException {
	// ContentResponse responseObject = new ObjectMapper().readValue(unsanitizedResponseJson, ContentResponse.class);
	// assertNotNull(responseObject);
	// // Update the uuid and compare json afterwards
	// responseObject.setUuid("uuid-value");
	// String sanitizedJson = new ObjectMapper().writeValueAsString(responseObject);
	// assertEquals("The response json did not match the expected one.", expectedJson, sanitizedJson);
	// }

	@Test
	public void testReadContentByValidPath() throws Exception {
		UserInfo info = data().getUserInfo();
		String response = request(info, HttpMethod.GET, "/api/v1/" + TestDataProvider.PROJECT_NAME + "/contents/subtag/english.html", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"},\"properties\":{\"filename\":\"english.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime!\"},\"type\":\"content\",\"language\":\"en_US\"}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);

	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		UserInfo info = data().getUserInfo();
		String response = request(info, HttpMethod.GET, "/api/v1/" + TestDataProvider.PROJECT_NAME + "/contents/subtag/subtag2/no-valid-page.html",
				404, "Not Found");
		String json = "{\"message\":\"Content not found for path {subtag/subtag2/no-valid-page.html}\"}";
		assertEquals(json, response);

	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		UserInfo info = data().getUserInfo();
		String response = request(info, HttpMethod.GET, "/api/v1/" + TestDataProvider.PROJECT_NAME
				+ "/contents/subtag/subtag-no-valid-tag/no-valid-page.html", 404, "Not Found");
		String json = "{\"message\":\"Content not found for path {subtag/subtag-no-valid-tag/no-valid-page.html}\"}";
		assertEquals(json, response);

	}

	@Test
	public void testReadContentByUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		Content content = data().getContentLevel1A1();
		String response = request(info, HttpMethod.GET, "/api/v1/" + TestDataProvider.PROJECT_NAME + "/contents/" + content.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"filename\":\"test_1.en.html\",\"name\":\"test_1 english\",\"content\":\"Blessed Mealtime 1!\"},\"type\":\"content\",\"language\":\"en_US\"}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);

	}

	@Test
	public void testReadContentByBogusUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		String response = request(info, HttpMethod.GET, "/api/v1/" + TestDataProvider.PROJECT_NAME + "/contents/bogusUUID", 404, "Not Found");
		String json = "{\"message\":\"Content not found for path {bogusUUID}\"}";
		assertEquals(json, response);

	}

	@Test
	public void testReadContentByInvalidUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		String json = "{\"message\":\"Content not found for uuid {dde8ba06bb7211e4897631a9ce2772f5}\"}";
		String response = request(info, HttpMethod.GET, "/api/v1/" + TestDataProvider.PROJECT_NAME + "/contents/dde8ba06bb7211e4897631a9ce2772f5",
				404, "Not Found");
		assertEquals(json, response);
	}

}
