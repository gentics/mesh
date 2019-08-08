package com.gentics.mesh.plugin.auth;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;

public class AuthServicePluginUtilsTest {

	@Test
	public void testRoleFilter() {
		List<RoleResponse> roles = new ArrayList<>();
		roles.add(new RoleResponse().setName("role1").setGroups(new GroupReference().setName("group1")));
		roles.add(new RoleResponse().setName("role2"));
		roles.add(new RoleResponse().setName("role3"));
		List<GroupResponse> groups = new ArrayList<>();
		groups.add(new GroupResponse().setName("group2").setRoles(new RoleReference().setName("role2")));
		groups.add(new GroupResponse().setName("group3"));

		RoleFilter filter = AuthServicePluginUtils.createRoleFilter(roles, groups);
		assertTrue(filter.filter("admin", "role"));
		assertTrue(filter.filter("group3", "role3"));
		assertFalse(filter.filter("group2", "role2"));
		assertFalse(filter.filter("group1", "role1"));
	}

	@Test
	public void testGroupFilter() {
		List<GroupResponse> groups = new ArrayList<>();
		groups.add(new GroupResponse().setName("group"));
		GroupFilter filter = AuthServicePluginUtils.createGroupFilter(groups);
		assertFalse(filter.filter("group"));
		assertTrue(filter.filter("group1"));
	}

}
