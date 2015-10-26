package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.query.QueryParameterProvider;

public interface GroupClientMethods {

	/**
	 * Load the given group.
	 * 
	 * @param uuid
	 * @return
	 */
	Future<GroupResponse> findGroupByUuid(String uuid);

	/**
	 * Load multiple groups.
	 * 
	 * @param parameters
	 * @return
	 */
	Future<GroupListResponse> findGroups(QueryParameterProvider... parameters);

	/**
	 * Create the group.
	 * 
	 * @param groupCreateRequest
	 * @return
	 */
	Future<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest);

	/**
	 * Update the group.
	 * 
	 * @param uuid
	 * @param request
	 * @return
	 */
	Future<GroupResponse> updateGroup(String uuid, GroupUpdateRequest request);

	/**
	 * Delete the group.
	 * 
	 * @param uuid
	 * @return
	 */
	Future<GenericMessageResponse> deleteGroup(String uuid);

	/**
	 * Add the given user to the group.
	 * 
	 * @param groupUuid
	 * @param userUuid
	 * @return
	 */
	Future<GroupResponse> addUserToGroup(String groupUuid, String userUuid);

	/**
	 * Remove the given user from the group.
	 * 
	 * @param groupUuid
	 * @param userUuid
	 * @return
	 */
	Future<GroupResponse> removeUserFromGroup(String groupUuid, String userUuid);

	/**
	 * Add the role to the group.
	 * 
	 * @param groupUuid
	 * @param roleUuid
	 * @return
	 */
	Future<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid);

	/**
	 * Remove the role from the group.
	 * 
	 * @param groupUuid
	 * @param roleUuid
	 * @return
	 */
	Future<GroupResponse> removeRoleFromGroup(String groupUuid, String roleUuid);

}
