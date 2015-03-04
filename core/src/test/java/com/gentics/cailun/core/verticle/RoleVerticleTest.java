package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.rest.response.RestRole;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.TestUtil;
import com.gentics.cailun.test.UserInfo;

public class RoleVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private RoleVerticle rolesVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return rolesVerticle;
	}

	@Test
	public void testReadRoleByUUID() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"superadmin\"}";
		UserInfo info = data().getUserInfo();
		Role adminRole = info.getRole();
		assertNotNull("The UUID of the role must not be null.", adminRole.getUuid());
		String response = request(info, HttpMethod.GET, "/api/v1/roles/" + adminRole.getUuid(), 200, "OK");
		TestUtil.assertEqualsSanitizedJson(json, response, RestRole.class);
	}

	@Test
	public void testReadRoleByName() throws Exception {
		UserInfo info = data().getUserInfo();
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"superadmin\"}";
		Role adminRole = info.getRole();
		assertNotNull("The UUID of the role must not be null.", adminRole.getUuid());
		String response = request(info, HttpMethod.GET, "/api/v1/roles/" + adminRole.getName(), 200, "OK");
		TestUtil.assertEqualsSanitizedJson(json, response, RestRole.class);
	}

	@Test
	public void testCreateRole() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteRoleByUUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteRoleByName() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateRole() {
		fail("Not yet implemented");
	}

}
