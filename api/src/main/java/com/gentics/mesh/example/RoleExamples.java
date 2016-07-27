package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;

public class RoleExamples extends AbstractExamples {

	public RoleResponse getRoleResponse1(String name) {
		RoleResponse role = new RoleResponse();
		role.setName(name);
		role.setCreated(getTimestamp());
		role.setCreator(getUserReference());
		role.setEdited(getTimestamp());
		role.setEditor(getUserReference());
		role.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		role.setUuid(randomUUID());
		return role;
	}

	public RolePermissionRequest getRolePermissionRequest() {
		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(false);
		request.getPermissions().add("create");
		request.getPermissions().add("read");
		request.getPermissions().add("update");
		request.getPermissions().add("delete");
		return request;
	}

	public RolePermissionResponse getRolePermissionResponse() {
		RolePermissionResponse response = new RolePermissionResponse();
		response.getPermissions().add("create");
		response.getPermissions().add("read");
		response.getPermissions().add("update");
		response.getPermissions().add("delete");
		return response;
	}

	public RoleResponse getRoleResponse2() {
		RoleResponse role = new RoleResponse();
		role.setName("Admin role");
		role.setUuid(randomUUID());
		role.setCreated(getTimestamp());
		role.setCreator(getUserReference());
		role.setEdited(getTimestamp());
		role.setEditor(getUserReference());
		role.setPermissions("READ", "UPDATE", "DELETE", "CREATE");
		List<GroupReference> groups = new ArrayList<>();
		groups.add(new GroupReference().setName("editors").setUuid(randomUUID()));
		groups.add(new GroupReference().setName("guests").setUuid(randomUUID()));
		role.setGroups(groups);
		return role;
	}

	public RoleListResponse getRoleListResponse() {
		RoleListResponse list = new RoleListResponse();
		list.getData().add(getRoleResponse1("Reader role"));
		list.getData().add(getRoleResponse2());
		setPaging(list, 1, 10, 2, 20);
		return list;
	}

	public RoleUpdateRequest getRoleUpdateRequest(String name) {
		RoleUpdateRequest roleUpdate = new RoleUpdateRequest();
		roleUpdate.setName(name);
		return roleUpdate;
	}

	public RoleCreateRequest getRoleCreateRequest(String name) {
		RoleCreateRequest roleCreate = new RoleCreateRequest();
		roleCreate.setName(name);
		return roleCreate;
	}

}
