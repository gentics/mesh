package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleReference;

public class GroupExamples extends AbstractExamples {

	public GroupResponse getGroupResponse1(String name) {
		GroupResponse group = new GroupResponse();
		group.setUuid(randomUUID());
		group.setCreated(getTimestamp());
		group.setCreator(getUserReference());
		group.setEdited(getTimestamp());
		group.setEditor(getUserReference());
		group.setName(name);
		group.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		group.getRoles().add(new RoleReference().setName("admin").setUuid(randomUUID()));
		return group;
	}

	public GroupResponse getGroupResponse2() {
		GroupResponse group2 = new GroupResponse();
		group2.setUuid(randomUUID());
		group2.setName("Editor Group");
		group2.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
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
