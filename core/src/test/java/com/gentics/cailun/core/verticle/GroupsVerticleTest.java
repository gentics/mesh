package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.rest.response.RestGroup;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.TestUtil;

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
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/groups/" + group.getUuid(), 200, "OK");
		TestUtil.assertEqualsSanitizedJson(json, response, RestGroup.class);
	}

	@Test
	public void testReadGroupByName() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"admin\"}";
		Group group = getDataProvider().getAdminGroup();
		assertNotNull("The name of the group must not be null.", group.getName());
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/groups/" + group.getName(), 200, "OK");
		TestUtil.assertEqualsSanitizedJson(json, response, RestGroup.class);
	}

}
