package com.gentics.cailun.core.verticle;

import io.vertx.core.http.HttpMethod;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.rest.model.Content;
import com.gentics.cailun.test.AbstractVerticleTest;
import com.gentics.cailun.test.DummyDataProvider;

public class ContentVerticleTest extends AbstractVerticleTest {

	@Autowired
	ContentVerticle verticle;

	@Before
	public void setUp() throws Exception {
		super.setup();

		// Inject spring config
		verticle.setSpringConfig(springConfig);
		verticle.init(springConfig.vertx(), null);
		verticle.start();
		verticle.registerEndPoints();
	}

	@Test
	public void testReadContentByValidPath() throws Exception {
		String json = "{\"name\":\"english content name\",\"filename\":\"english.html\",\"content\":\"blessed mealtime!\",\"teaser\":null,\"author\":null}";
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
		String json = "tbd";
		Content content = dataProvider.getContent();
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/" + content.getUuid(), 200, "OK",
				json);
	}

	@Test
	public void testReadContentByBogusUUID() throws Exception {
		String json = "{\"message\":\"Content not found for path {bogusUUID}\"}";
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/bogusUUID", 404, "Not Found", json);
	}

	@Test
	public void testReadContentByInvalidUUID() throws Exception {
		String json = "{\"message\":\"Content not found for uuid {13371d56bb7011e48325e1565592fake}\"}";
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/13371d56bb7011e48325e1565592fake", 404, "Not Found",
				json);
	}

}
