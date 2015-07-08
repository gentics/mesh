package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;

public interface GroupClientMethods {

	Future<GroupResponse> findGroupByUuid(String uuid);

	Future<GroupListResponse> findGroups(PagingInfo pagingInfo);

	Future<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest);

	Future<GroupResponse> updateGroup(String uuid, GroupUpdateRequest groupUpdateRequest);

	Future<GenericMessageResponse> deleteGroup(String uuid);

	Future<GroupResponse> addUserToGroup(String groupUuid, String userUuid);

	Future<GroupResponse> removeUserFromGroup(String groupUuid, String userUuid);

	Future<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid);

	Future<GroupResponse> removeRoleFromGroup(String groupUuid, String roleUuid);

}
