package com.gentics.cailun.core.verticle;

import io.vertx.core.http.HttpMethod;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.verticle.ContentVerticle;
import com.gentics.cailun.test.AbstractVerticleTest;
import com.gentics.cailun.test.DummyDataProvider;

public class ContentVerticleTest extends AbstractVerticleTest {

	@Autowired
	ContentVerticle verticle;

	@Autowired
	DummyDataProvider dataProvider;

	@Before
	public void setup() throws Exception {
		super.setup();
		dataProvider.setup();

		springConfig.routerStorage().addProjectRouter(DummyDataProvider.PROJECT_NAME);
		// Inject spring config
		verticle.setSpringConfig(springConfig);
		verticle.init(springConfig.vertx(), null);
		verticle.start();
		verticle.registerEndPoints();
	}

	@Test
	public void testCRUD() throws Exception {
		testAuthenticatedRequest(HttpMethod.GET, "/api/v1/" + DummyDataProvider.PROJECT_NAME + "/contents/bogusUUID", 200, "OK");
		System.in.read();
	}

}
