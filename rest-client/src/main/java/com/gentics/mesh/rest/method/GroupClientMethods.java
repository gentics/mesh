package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;

public interface GroupClientMethods {

	Future<GroupResponse> findGroupByUuid(String uuid);

	Future<GroupListResponse> findGroups();

	Future<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest);

	Future<GroupResponse> updateGroup(GroupUpdateRequest groupUpdateRequest);

	Future<GenericMessageResponse> deleteGroup(String uuid);

}
