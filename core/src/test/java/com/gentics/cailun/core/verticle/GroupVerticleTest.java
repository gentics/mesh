package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.TestUtil;

public class GroupVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Autowired
	private GroupService groupService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return groupsVerticle;
	}

	@Test
	public void testReadGroupByUUID() throws Exception {
		Group group = info.getGroup();

		// Add a child group to group of the user
		Group subGroup = new Group("sub group");
		group.addGroup(subGroup);
		subGroup = groupService.save(subGroup);
		group = groupService.save(group);

		assertNotNull("The UUID of the group must not be null.", group.getUuid());
		String response = request(info, HttpMethod.GET, "/api/v1/groups/" + group.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"childGroups\":[\"sub group\"],\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
		TestUtil.assertEqualsSanitizedJson(json, response, GroupResponse.class);
	}

	@Test
	public void testReadGroupByName() throws Exception {
		Group group = info.getGroup();
		assertNotNull("The name of the group must not be null.", group.getName());
		String response = request(info, HttpMethod.GET, "/api/v1/groups/" + group.getName(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\"}";
		TestUtil.assertEqualsSanitizedJson(json, response, GroupResponse.class);
	}

	@Test
	public void testCreateGroup() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteGroupByUUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteGroupByName() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateGroup() {
		fail("Not yet implemented");
	}

}
