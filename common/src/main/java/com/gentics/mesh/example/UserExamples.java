package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserResetTokenResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.util.UUIDUtil;

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
		user2.setEdited(createTimestamp());
		user2.setCreated(createTimestamp());
		user2.setEmailAddress("j.roe@nowhere.com");
		user2.getGroups().add(new GroupReference().setName("super-editors").setUuid(randomUUID()));
		user2.getGroups().add(new GroupReference().setName("editors").setUuid(randomUUID()));
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
		user.setUuid(randomUUID());
		user.setCreated(createTimestamp());
		user.setCreator(createUserReference());
		user.setEdited(createTimestamp());
		user.setEditor(createUserReference());
		user.setUsername(username);
		user.setFirstname("Joe");
		user.setLastname("Doe");
		user.setEnabled(true);

		NodeReference reference = new NodeReference();
		reference.setProjectName("dummy");
		reference.setUuid(randomUUID());
		reference.setDisplayName("DeLorean DMC-12");
		reference.setSchema(getSchemaReference("vehicle"));
		
		user.setNodeReference(reference);
		user.setEmailAddress("j.doe@nowhere.com");
		user.getGroups().add(new GroupReference().setName("editors").setUuid(randomUUID()));
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
		userCreate.setGroupUuid(randomUUID());
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
		return new UserResetTokenResponse().setToken(randomToken()).setCreated(createTimestamp());
	}

	public UserAPITokenResponse getAPIKeyResponse() {
		return new UserAPITokenResponse().setToken(UUIDUtil.randomUUID());
	}

}
