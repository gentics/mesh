package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.HasPermissions;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.user.UserResponse;

/**
 * User DAO
 */
public interface UserDaoWrapper extends UserDao, DaoWrapper<HibUser>, DaoTransformable<HibUser, UserResponse> {

	/**
	 * Check whether the user has the given permission on the given element.
	 *
	 * @param user
	 * @param element
	 * @param permission
	 * @return
	 * @deprecated Use {@link #hasPermission(HibUser, HibBaseElement, InternalPermission)} instead.
	 */
	@Deprecated
	boolean hasPermission(HibUser user, MeshVertex element, InternalPermission permission);

	/**
	 * Check the read permission on the given container and return false if the needed permission to read the container is not set. This method will not return
	 * false if the user has READ permission or READ_PUBLISH permission on a published node.
	 *
	 * @param user
	 * @param container
	 * @param branchUuid
	 * @param requestedVersion
	 */
	boolean hasReadPermission(HibUser user, NodeGraphFieldContainer container, String branchUuid, String requestedVersion);

	/**
	 * Check the read permission on the given container and fail if the needed permission to read the container is not set. This method will not fail if the
	 * user has READ permission or READ_PUBLISH permission on a published node.
	 *
	 * @param container
	 * @param branchUuid
	 * @param requestedVersion
	 */
	void failOnNoReadPermission(HibUser user, NodeGraphFieldContainer container, String branchUuid, String requestedVersion);
	/**
	 * This method will set CRUD permissions to the target node for all roles that would grant the given permission on the node. The method is most often used
	 * to assign CRUD permissions on newly created elements. Example for adding CRUD permissions on a newly created project: The method will first determine the
	 * list of roles that would initially enable you to create a new project. It will do so by examining the projectRoot node. After this step the CRUD
	 * permissions will be added to the newly created project and the found roles. In this case the call would look like this:
	 * addCRUDPermissionOnRole(projectRoot, Permission.CREATE_PERM, newlyCreatedProject); This method will ensure that all users/roles that would be able to
	 * create an element will also be able to CRUD it even when the creator of the element was only assigned to one of the enabling roles. Additionally the
	 * permissions of the source node are inherited by the target node. All permissions between the source node and roles are copied to the target node.
	 *
	 * @param user
	 * @param sourceNode
	 *            Node that will be checked against to find all roles that would grant the given permission.
	 * @param permission
	 *            Permission that is used in conjunction with the node to determine the list of affected roles.
	 * @param targetNode
	 *            Node to which the CRUD permissions will be assigned.
	 * @return Fluent API
	 */
	HibUser addCRUDPermissionOnRole(HibUser user, HasPermissions sourceNode, InternalPermission permission, MeshVertex targetNode);

	/**
	 * This method adds additional permissions to the target node. The roles are selected like in method
	 * {@link #addCRUDPermissionOnRole(User, HasPermissions, InternalPermission, MeshVertex)} .
	 *
	 * @param user
	 * @param sourceNode
	 *            Node that will be checked
	 * @param permission
	 *            checked permission
	 * @param targetNode
	 *            target node
	 * @param toGrant
	 *            permissions to grant
	 * @return Fluent API
	 */
	HibUser addPermissionsOnRole(HibUser user, HasPermissions sourceNode, InternalPermission permission, MeshVertex targetNode,
		InternalPermission... toGrant);

	/**
	 * Inherit permissions edges from the source node and assign those permissions to the target node.
	 *
	 * @param user
	 * @param sourceNode
	 * @param targetNode
	 * @return Fluent API
	 * @deprecated Use {@link #inheritRolePermissions(HibUser, HibBaseElement, HibBaseElement)} instead.
	 */
	@Deprecated
	HibUser inheritRolePermissions(HibUser user, MeshVertex sourceNode, MeshVertex targetNode);
}
