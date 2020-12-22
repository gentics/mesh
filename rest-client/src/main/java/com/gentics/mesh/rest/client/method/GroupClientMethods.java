package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

/**
 * Rest Client methods for handling group requests.
 */
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
	 * @param createRequest
	 * @return
	 */
	MeshRequest<GroupResponse> createGroup(GroupCreateRequest createRequest);

	/**
	 * Create the group using the provided uuid.
	 * 
	 * @param uuid
	 * @param createRequest
	 * @return
	 */
	MeshRequest<GroupResponse> createGroup(String uuid, GroupCreateRequest createRequest);

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
	MeshRequest<EmptyResponse> deleteGroup(String uuid);

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
	MeshRequest<EmptyResponse> removeUserFromGroup(String groupUuid, String userUuid);

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
	MeshRequest<EmptyResponse> removeRoleFromGroup(String groupUuid, String roleUuid);

}
