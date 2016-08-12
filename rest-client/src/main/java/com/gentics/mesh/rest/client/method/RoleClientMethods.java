package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

public interface RoleClientMethods {

	/**
	 * Load the role.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<RoleResponse> findRoleByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Load multiple roles.
	 * 
	 * @param parameter
	 * @return
	 */
	MeshRequest<RoleListResponse> findRoles(ParameterProvider... parameter);

	/**
	 * Create a new role.
	 * 
	 * @param request
	 * @return
	 */
	MeshRequest<RoleResponse> createRole(RoleCreateRequest request);

	/**
	 * Delete the role.
	 * 
	 * @param uuid
	 * @return
	 */
	MeshRequest<GenericMessageResponse> deleteRole(String uuid);

	/**
	 * Load multiple roles that were assigned to the given group.
	 * 
	 * @param groupUuid
	 * @param parameter
	 * @return
	 */
	MeshRequest<RoleListResponse> findRolesForGroup(String groupUuid, ParameterProvider... parameter);

	/**
	 * Update the role permissions for the the given path.
	 * 
	 * @param roleUuid
	 *            Role to which the permissions should be updated
	 * @param pathToElement
	 *            Path to an element or an aggregation element
	 * @param request
	 *            Request that defines how the permissions should be changed
	 * @return
	 */
	MeshRequest<GenericMessageResponse> updateRolePermissions(String roleUuid, String pathToElement, RolePermissionRequest request);

	/**
	 * Read the role permissions for the given path.
	 * 
	 * @param roleUuid
	 * @param pathToElement
	 * @return
	 */
	MeshRequest<RolePermissionResponse> readRolePermissions(String roleUuid, String pathToElement);

	/**
	 * Update the role using the given update request.
	 * 
	 * @param uuid
	 * @param restRole
	 * @return
	 */
	MeshRequest<RoleResponse> updateRole(String uuid, RoleUpdateRequest restRole);
}
