import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.plugin.auth.MappingResult;

MappingResult result = new MappingResult();

if (uuid == null) {
	log.debug("First time login of the user");
} else {
	log.debug("Already synced user is logging in.");
}

String username = token.getString("preferred_username");

log.debug("Mapping user in plugin");
log.trace("Token for {" + username + "}");
log.trace(token.encodePrettily());

UserUpdateRequest user = new UserUpdateRequest();

user.setUsername(username);
user.setEmailAddress("mapped@email.tld");
user.setFirstname("mapepdFirstname");
user.setLastname("mapepdLastname");
result.setUser(user);

log.info("Mapping groups in plugin");

List<GroupResponse> groupList = new ArrayList<>();

groupList.add(new GroupResponse().setName("group1"));
groupList.add(new GroupResponse()
		.setName("group2")
		.setRoles(Arrays.asList(new RoleReference().setName("role1"))));
groupList.add(new GroupResponse()
		.setName("group3")
		.setRoles(Arrays.asList(new RoleReference().setName("role1"), new RoleReference().setName("role2"))));
result.setGroups(groupList);

log.info("Mapping role in plugin");

List<RoleResponse> roleList = new ArrayList<>();

roleList.add(new RoleResponse().setName("role1"));
roleList.add(new RoleResponse().setName("role2"));
result.setRoles(roleList);

result.setGroupFilter({ groupName ->
	log.info("Handling removal of user from group {" + groupName + "}");
	// Return true here if you want to remove the group with the given name
	return false;
});

result.setRoleFilter({ groupName, roleName ->
	log.info("Handling removal of role {" + roleName + "} from {" + groupName + "}");
	// Return true here if you want to remove the role with the given name
	return false;
});

return result;
