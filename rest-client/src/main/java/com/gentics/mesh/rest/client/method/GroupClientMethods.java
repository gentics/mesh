package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

public interface GroupClientMethods {

	/**
	 * Load the given group.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<GroupResponse> findGroupByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Load multiple groups.
	 * 
	 * @param parameters
	 * @return
	 */
	MeshRequest<GroupListResponse> findGroups(ParameterProvider... parameters);

	/**
	 * Create the group.
	 * 
	 * @param groupCreateRequest
	 * @return
	 */
	MeshRequest<GroupResponse> createGroup(GroupCreateRequest groupCreateRequest);

	/**
	 * Update the group.
	 * 
	 * @param uuid
	 * @param request
	 * @return
	 */
	MeshRequest<GroupResponse> updateGroup(String uuid, GroupUpdateRequest request);

	/**
	 * Delete the group.
	 * 
	 * @param uuid
	 * @return
	 */
	MeshRequest<GenericMessageResponse> deleteGroup(String uuid);

	/**
	 * Add the given user to the group.
	 * 
	 * @param groupUuid
	 * @param userUuid
	 * @return
	 */
	MeshRequest<GroupResponse> addUserToGroup(String groupUuid, String userUuid);

	/**
	 * Remove the given user from the group.
	 * 
	 * @param groupUuid
	 * @param userUuid
	 * @return
	 */
	MeshRequest<GroupResponse> removeUserFromGroup(String groupUuid, String userUuid);

	/**
	 * Add the role to the group.
	 * 
	 * @param groupUuid
	 * @param roleUuid
	 * @return
	 */
	MeshRequest<GroupResponse> addRoleToGroup(String groupUuid, String roleUuid);

	/**
	 * Remove the role from the group.
	 * 
	 * @param groupUuid
	 * @param roleUuid
	 * @return
	 */
	MeshRequest<GroupResponse> removeRoleFromGroup(String groupUuid, String roleUuid);

}
