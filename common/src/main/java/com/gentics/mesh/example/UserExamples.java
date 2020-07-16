package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.example.ExampleUuids.GROUP_EDITORS_UUID;
import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.example.ExampleUuids.TOKEN_UUID;
import static com.gentics.mesh.example.ExampleUuids.USER_EDITOR_UUID;
import static com.gentics.mesh.example.ExampleUuids.USER_WEBCLIENT_UUID;

import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResetTokenResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;

public class UserExamples extends AbstractExamples {

	/**
	 * Return a user response with user jane roe.
	 * 
	 * @return
	 */
	public UserResponse getUserResponse2() {
		UserResponse user2 = getUserResponse1("jroe");
		user2.setFirstname("Jane");
		user2.setLastname("Roe");
		user2.setEdited(createNewTimestamp());
		user2.setCreated(createOldTimestamp());
		user2.setEmailAddress("j.roe@nowhere.com");
		user2.setAdmin(true);
		user2.getGroups().add(new GroupReference().setName("webclient").setUuid(USER_WEBCLIENT_UUID));
		user2.getGroups().add(new GroupReference().setName("editors").setUuid(USER_EDITOR_UUID));
		user2.setEnabled(true);
		return user2;
	}

	/**
	 * Return a user response with user joe doe.
	 * 
	 * @param username
	 * @return
	 */
	public UserResponse getUserResponse1(String username) {
		UserResponse user = new UserResponse();
		user.setUuid(USER_EDITOR_UUID);
		user.setCreated(createOldTimestamp());
		user.setCreator(createUserReference());
		user.setEdited(createNewTimestamp());
		user.setEditor(createUserReference());
		user.setUsername(username);
		user.setFirstname("Joe");
		user.setLastname("Doe");
		user.setEnabled(true);

		NodeReference reference = new NodeReference();
		reference.setProjectName("dummy");
		reference.setUuid(NODE_DELOREAN_UUID);
		reference.setDisplayName("DeLorean DMC-12");
		reference.setSchema(getSchemaReference("vehicle"));
		
		user.setNodeReference(reference);
		user.setEmailAddress("j.doe@nowhere.com");
		user.getGroups().add(new GroupReference().setName("editors").setUuid(GROUP_EDITORS_UUID));
		user.setPermissions(READ, UPDATE, DELETE, CREATE);
		return user;
	}

	public UserListResponse getUserListResponse() {
		UserListResponse userList = new UserListResponse();
		userList.getData().add(getUserResponse1("jdoe"));
		userList.getData().add(getUserResponse2());
		setPaging(userList, 1, 10, 2, 20);
		return userList;
	}

	public UserUpdateRequest getUserUpdateRequest(String username) {
		UserUpdateRequest userUpdate = new UserUpdateRequest();
		userUpdate.setUsername(username);
		userUpdate.setPassword("iesiech0eewinioghaRa");
		userUpdate.setFirstname("Joe");
		userUpdate.setLastname("Doe");
		userUpdate.setEmailAddress("j.doe@nowhere.com");
		ExpandableNode node = getUserResponse1("jdoe").getNodeReference();
		userUpdate.setNodeReference(node);
		return userUpdate;
	}

	public UserCreateRequest getUserCreateRequest(String name) {
		UserCreateRequest userCreate = new UserCreateRequest();
		userCreate.setUsername(name);
		userCreate.setPassword("iesiech0eewinioghaRa");
		userCreate.setFirstname("Joe");
		userCreate.setLastname("Doe");
		userCreate.setEmailAddress("j.doe@nowhere.com");
		userCreate.setGroupUuid(GROUP_EDITORS_UUID);
		userCreate.setNodeReference(getUserResponse2().getNodeReference());
		return userCreate;
	}

	public UserPermissionResponse getUserPermissionResponse() {
		UserPermissionResponse userPermResponse = new UserPermissionResponse();
		userPermResponse.add(CREATE);
		userPermResponse.add(READ);
		userPermResponse.add(UPDATE);
		userPermResponse.add(DELETE);
		return userPermResponse;
	}

	public UserResetTokenResponse getTokenResponse() {
		return new UserResetTokenResponse().setToken("FDrbBDWRY3aS").setCreated(createNewTimestamp());
	}

	public UserAPITokenResponse getAPIKeyResponse() {
		return new UserAPITokenResponse().setToken(TOKEN_UUID);
	}

}
