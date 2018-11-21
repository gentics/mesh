package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.PUBLISH;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.example.ExampleUuids.GROUP_CLIENT_UUID;
import static com.gentics.mesh.example.ExampleUuids.GROUP_EDITORS_UUID;
import static com.gentics.mesh.example.ExampleUuids.ROLE_ADMIN_UUID;
import static com.gentics.mesh.example.ExampleUuids.ROLE_CLIENT_UUID;

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
		role.setCreated(createOldTimestamp());
		role.setCreator(createUserReference());
		role.setEdited(createNewTimestamp());
		role.setEditor(createUserReference());
		role.setPermissions(READ, UPDATE, DELETE, CREATE);
		role.setUuid(ROLE_CLIENT_UUID);
		return role;
	}

	public RolePermissionRequest getRolePermissionRequest() {
		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(false);
		request.getPermissions().set(CREATE, true);
		request.getPermissions().set(READ, true);
		request.getPermissions().set(UPDATE, true);
		request.getPermissions().set(DELETE, true);
		request.getPermissions().set(READ_PUBLISHED, false);
		request.getPermissions().set(PUBLISH, false);
		return request;
	}

	public RolePermissionResponse getRolePermissionResponse() {
		RolePermissionResponse response = new RolePermissionResponse();
		response.set(CREATE, true);
		response.set(READ, true);
		response.set(UPDATE, true);
		response.set(DELETE, true);
		response.set(READ_PUBLISHED, false);
		response.set(PUBLISH, false);
		return response;
	}

	public RoleResponse getRoleResponse2() {
		RoleResponse role = new RoleResponse();
		role.setName("Admin role");
		role.setUuid(ROLE_ADMIN_UUID);
		role.setCreated(createOldTimestamp());
		role.setCreator(createUserReference());
		role.setEdited(createNewTimestamp());
		role.setEditor(createUserReference());
		role.setPermissions(READ, UPDATE, DELETE, CREATE);
		List<GroupReference> groups = new ArrayList<>();
		groups.add(new GroupReference().setName("editors").setUuid(GROUP_EDITORS_UUID));
		groups.add(new GroupReference().setName("clients").setUuid(GROUP_CLIENT_UUID));
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
