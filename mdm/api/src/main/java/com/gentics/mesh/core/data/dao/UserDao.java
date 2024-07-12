package com.gentics.mesh.core.data.dao;

import java.util.Collection;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for user operations.
 */
public interface UserDao extends DaoGlobal<User>, DaoTransformable<User, UserResponse> {

	/**
	 * Return the sub etag for the given user.
	 * 
	 * @param user
	 * @param ac
	 * @return
	 */
	String getSubETag(User user, InternalActionContext ac);

	/**
	 * Check the permission on the given element.
	 * 
	 * @param user
	 * @param element
	 * @param permission
	 * @return
	 */
	boolean hasPermission(User user, BaseElement element, InternalPermission permission);

	/**
	 * Check whether the user has the given permission on the element with the given id.
	 *
	 * @param user
	 * @param elementId
	 * @param permission
	 * @return
	 */
	boolean hasPermissionForId(User user, Object elementId, InternalPermission permission);

	/**
	 * Check whether the user is allowed to read the given node. Internally this check the currently configured version scope and check for
	 * {@link InternalPermission#READ_PERM} or {@link InternalPermission#READ_PUBLISHED_PERM}.
	 *
	 * @param user
	 * @param ac
	 * @param node
	 * @return
	 */
	boolean canReadNode(User user, InternalActionContext ac, Node node);

	/**
	 * Return the permission info object for the given vertex.
	 *
	 * @param user
	 * @param element
	 * @return
	 */
	PermissionInfo getPermissionInfo(User user, BaseElement element);

	/**
	 * Return a set of permissions which the user got for the given vertex.
	 *
	 * @param element
	 * @return
	 */
	Set<InternalPermission> getPermissions(User user, BaseElement element);

	/**
	 * Same as {@link User#update(InternalActionContext, EventQueueBatch)}, but does not actually perform any changes.
	 *
	 * Useful to check if any changes have to be made.
	 *
	 * @param user
	 * @param ac
	 * @return true if the user would have been modified
	 */
	boolean updateDry(User user, InternalActionContext ac);

	/**
	 * Create the user with the given uuid.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	User create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @return
	 */
	default User create(String username, User creator) {
		return create(username, creator, null);
	}

	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @param uuid
	 *            Optional uuid
	 * @return
	 */
	User create(String username, User creator, String uuid);

	/**
	 * Inherit the permissions of the source elment to the target element.
	 * 
	 * @param user
	 *            User to check the permission from
	 * @param source
	 *            Element from which the element should be loaded
	 * @param target
	 *            Element for which the perms should be applied
	 * @return
	 */
	User inheritRolePermissions(User user, BaseElement source, BaseElement target);

	/**
	 * Inherit the permissions of the source elment to the collection of target elements.
	 * 
	 * @param user
	 *            User to check the permission from
	 * @param source
	 *            Element from which the element should be loaded
	 * @param targets
	 *            Elements for which the perms should be applied
	 * @return
	 */
	User inheritRolePermissions(User user, BaseElement source, Collection<? extends BaseElement> targets);

	/**
	 * Set the plaintext password. Internally the password string will be hashed and the password hash will be set. This will also set
	 * {@link User#setForcedPasswordChange(boolean)} to false.
	 *
	 * @param user
	 * @param password
	 * @return Fluent API
	 */
	// TODO change this to an async call since hashing of the password is
	// blocking
	User setPassword(User user, String password);

	/**
	 * Find the user with the given username.
	 * 
	 * @param username
	 * @return
	 */
	User findByUsername(String username);

	/**
	 * Find the mesh auth user with the given username.
	 * 
	 * @param username
	 * @return
	 */
	MeshAuthUser findMeshAuthUserByUsername(String username);

	/**
	 * Find the mesh auth user with the given UUID.
	 * 
	 * @param userUuid
	 * @return
	 */
	MeshAuthUser findMeshAuthUserByUuid(String userUuid);

	/**
	 * Check whether the given token code is valid. This method will also clear the token code if the token has expired.
	 *
	 * @parm user
	 * @param token
	 *            Token Code
	 * @param maxTokenAgeMins
	 *            maximum allowed token age in minutes
	 * @return
	 */
	default boolean isResetTokenValid(User user, String token, int maxTokenAgeMins) {
		Long resetTokenIssueTimestamp = user.getResetTokenIssueTimestamp();
		if (token == null || resetTokenIssueTimestamp == null) {
			return false;
		}
		long maxTokenAge = 1000 * 60 * maxTokenAgeMins;
		long tokenAge = System.currentTimeMillis() - resetTokenIssueTimestamp;
		boolean isExpired = tokenAge > maxTokenAge;
		boolean isTokenMatch = token.equals(user.getResetToken());

		if (isTokenMatch && isExpired) {
			user.invalidateResetToken();
			return false;
		}
		return isTokenMatch && !isExpired;
	}

	/**
	 * Add the user to the given group.
	 *
	 * @param user
	 * @param group
	 * @return Fluent API
	 */
	User addGroup(User user, Group group);

	/**
	 * Load the roles of the user.
	 * 
	 * @param user
	 * @return
	 */
	Iterable<? extends Role> getRoles(User user);

	/**
	 * Load the groups of the user.
	 * 
	 * @param user
	 * @return
	 */
	Result<? extends Group> getGroups(User user);

	/**
	 * Load the effective roles for user via the shortcut edges.
	 * 
	 * @param fromUser
	 * @param authUser
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends Role> getRolesViaShortcut(User fromUser, User authUser, PagingParameters pagingInfo);

	/**
	 * Return the page of groups which the user is part of.
	 * 
	 * @param fromUser
	 *            User to be checked
	 * @param authUser
	 *            User to be used to permission checks
	 * @param pagingInfo
	 *            Paging to be applied
	 * @return
	 */
	Page<? extends Group> getGroups(User fromUser, User authUser, PagingParameters pagingInfo);

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
	User addCRUDPermissionOnRole(User user, BaseElement sourceNode, InternalPermission permission, BaseElement targetNode);

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
	User addPermissionsOnRole(User user, BaseElement sourceNode, InternalPermission permission, BaseElement targetNode,
		InternalPermission... toGrant);

	/**
	 * Check the read permission on the given container and return false if the needed permission to read the container is not set. This method will not return
	 * false if the user has READ permission or READ_PUBLISH permission on a published node.
	 *
	 * @param user
	 * @param container
	 * @param branchUuid
	 * @param requestedVersion
	 */
	boolean hasReadPermission(User user, NodeFieldContainer container, String branchUuid, String requestedVersion);

	/**
	 * Check the read permission on the given container and fail if the needed permission to read the container is not set. This method will not fail if the
	 * user has READ permission or READ_PUBLISH permission on a published node.
	 *
	 * @param container
	 * @param branchUuid
	 * @param requestedVersion
	 */
	void failOnNoReadPermission(User user, NodeFieldContainer container, String branchUuid, String requestedVersion);

	/**
	 * A CRC32 hash of the users {@link #getRoles roles}.
	 *
	 * @return A hash of the users roles
	 */
	String getRolesHash(User user);

	/**
	 * Set the user password hash and update forced password change flag
	 * @param passwordHash
	 */
	void updatePasswordHash(User user, String passwordHash);
}
