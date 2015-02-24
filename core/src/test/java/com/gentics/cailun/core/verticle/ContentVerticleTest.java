package com.gentics.cailun.core.verticle;

import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.test.AbstractProjectRestVerticleTest;
import com.gentics.cailun.test.DummyDataProvider;

public class ContentVerticleTest extends AbstractProjectRestVerticleTest {

	@Autowired
	ContentVerticle verticle;

	@Override
	public AbstractProjectRestVerticle getVerticle() {
		return verticle;
	}

	@Test
	public void testReadContentByValidPath() throws Exception {
		String json = "{\"author\":{\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"},\"properties\":{\"filename\":\"english.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime!\"},\"language\":\"en_US\"}";
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/subtag/english.html", 200, "OK", json);
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String json = "{\"message\":\"Content not found for path {subtag/subtag2/no-valid-page.html}\"}";
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/subtag/subtag2/no-valid-page.html", 404,
				"Not Found", json);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String json = "{\"message\":\"Content not found for path {subtag/subtag-no-valid-tag/no-valid-page.html}\"}";
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME
				+ "/contents/subtag/subtag-no-valid-tag/no-valid-page.html", 404, "Not Found", json);
	}

	@Test
	public void testReadContentByUUID() throws Exception {
		String json = "{\"author\":{\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"},\"properties\":{\"filename\":\"english.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime!\"},\"language\":\"en_US\"}";
		Content content = dataProvider.getContent();
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/" + content.getUuid(), 200, "OK", json);
	}

	@Test
	public void testReadContentByBogusUUID() throws Exception {
		String json = "{\"message\":\"Content not found for path {bogusUUID}\"}";
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/bogusUUID", 404, "Not Found", json);
	}

	@Test
	public void testReadContentByInvalidUUID() throws Exception {
		String json = "{\"message\":\"Content not found for uuid {dde8ba06bb7211e4897631a9ce2772f5}\"}";
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/dde8ba06bb7211e4897631a9ce2772f5", 404,
				"Not Found", json);
	}

}
