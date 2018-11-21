package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.example.ExampleUuids.GROUP_ADMIN_UUID;
import static com.gentics.mesh.example.ExampleUuids.GROUP_EDITORS_UUID;
import static com.gentics.mesh.example.ExampleUuids.ROLE_ADMIN_UUID;

import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleReference;

/**
 *	Utility class which provides.
 */
public class GroupExamples extends AbstractExamples {

	public GroupResponse getGroupResponse1(String name) {
		GroupResponse group = new GroupResponse();
		group.setUuid(GROUP_ADMIN_UUID);
		group.setCreated(createOldTimestamp());
		group.setCreator(createUserReference());
		group.setEdited(createNewTimestamp());
		group.setEditor(createUserReference());
		group.setName(name);
		group.setPermissions(READ, UPDATE, DELETE, CREATE);
		group.getRoles().add(new RoleReference().setName("admin").setUuid(ROLE_ADMIN_UUID));
		return group;
	}

	public GroupResponse getGroupResponse2() {
		GroupResponse group2 = new GroupResponse();
		group2.setUuid(GROUP_EDITORS_UUID);
		group2.setName("Editor Group");
		group2.setPermissions(READ, UPDATE, DELETE, CREATE);
		return group2;
	}

	public GroupListResponse getGroupListResponse() {
		GroupListResponse groupList = new GroupListResponse();
		groupList.getData().add(getGroupResponse1("Admin Group"));
		groupList.getData().add(getGroupResponse2());
		setPaging(groupList, 1, 10, 2, 20);
		return groupList;
	}

	public GroupUpdateRequest getGroupUpdateRequest(String name) {
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setName(name);
		return request;
	}

	public GroupCreateRequest getGroupCreateRequest(String name) {
		GroupCreateRequest groupCreate = new GroupCreateRequest();
		groupCreate.setName(name);
		return groupCreate;
	}

}
