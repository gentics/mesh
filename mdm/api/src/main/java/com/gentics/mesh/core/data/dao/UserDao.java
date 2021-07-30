package com.gentics.mesh.core.data.dao;

import java.util.Set;
import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for user operations.
 */
public interface UserDao extends DaoWrapper<HibUser>, DaoTransformable<HibUser, UserResponse> {

	/**
	 * Return the sub etag for the given user.
	 * 
	 * @param user
	 * @param ac
	 * @return
	 */
	String getSubETag(HibUser user, InternalActionContext ac);

	/**
	 * Load all users.
	 * 
	 * @return
	 */
	Result<? extends HibUser> findAll();

	/**
	 * Check the permission on the given element.
	 * 
	 * @param user
	 * @param element
	 * @param permission
	 * @return
	 */
	boolean hasPermission(HibUser user, HibBaseElement element, InternalPermission permission);

	/**
	 * Check whether the user has the given permission on the element with the given id.
	 *
	 * @param user
	 * @param elementId
	 * @param permission
	 * @return
	 */
	boolean hasPermissionForId(HibUser user, Object elementId, InternalPermission permission);

	/**
	 * Check whether the user is allowed to read the given node. Internally this check the currently configured version scope and check for
	 * {@link InternalPermission#READ_PERM} or {@link InternalPermission#READ_PUBLISHED_PERM}.
	 *
	 * @param user
	 * @param ac
	 * @param node
	 * @return
	 */
	boolean canReadNode(HibUser user, InternalActionContext ac, HibNode node);

	/**
	 * Load the user with the given uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	HibUser loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Load the page with the users.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibUser> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Load the page with the users.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends HibUser> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibUser> extraFilter);

	/**
	 * Load the user with the given uuid.
	 * 
	 * @param ac
	 * @param userUuid
	 * @param perm
	 * @return
	 */
	HibUser loadObjectByUuid(InternalActionContext ac, String userUuid, InternalPermission perm);

	/**
	 * Return the permission info object for the given vertex.
	 *
	 * @param user
	 * @param element
	 * @return
	 */
	PermissionInfo getPermissionInfo(HibUser user, HibBaseElement element);

	/**
	 * Return a set of permissions which the user got for the given vertex.
	 *
	 * @param element
	 * @return
	 */
	Set<InternalPermission> getPermissions(HibUser user, HibBaseElement element);

	/**
	 * Update the vertex using the action context information.
	 *
	 * @param user
	 * @param ac
	 * @param batch
	 *            Batch to which entries will be added in order to update the search index.
	 * @return true if the element was updated. Otherwise false
	 */
	boolean update(HibUser user, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Same as {@link HibUser#update(InternalActionContext, EventQueueBatch)}, but does not actually perform any changes.
	 *
	 * Useful to check if any changes have to be made.
	 *
	 * @param user
	 * @param ac
	 * @return true if the user would have been modified
	 */
	boolean updateDry(HibUser user, InternalActionContext ac);

	/**
	 * Create the user with the given uuid.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	HibUser create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @return
	 */
	default HibUser create(String username, HibUser creator) {
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
	HibUser create(String username, HibUser creator, String uuid);

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
	HibUser inheritRolePermissions(HibUser user, HibBaseElement source, HibBaseElement target);

	/**
	 * Set the plaintext password. Internally the password string will be hashed and the password hash will be set. This will also set
	 * {@link HibUser#setForcedPasswordChange(boolean)} to false.
	 *
	 * @param user
	 * @param password
	 * @return Fluent API
	 */
	// TODO change this to an async call since hashing of the password is
	// blocking
	HibUser setPassword(HibUser user, String password);

	/**
	 * Find the user by uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	HibUser findByUuid(String uuid);

	/**
	 * Find the user by username.
	 * 
	 * @param name
	 * @return
	 */
	HibUser findByName(String name);

	/**
	 * Find the user with the given username.
	 * 
	 * @param username
	 * @return
	 */
	HibUser findByUsername(String username);

	/**
	 * Delete the user.
	 * 
	 * @param user
	 * @param bac
	 */
	void delete(HibUser user, BulkActionContext bac);

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
	default boolean isResetTokenValid(HibUser user, String token, int maxTokenAgeMins) {
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
	HibUser addGroup(HibUser user, HibGroup group);

	/**
	 * Load the roles of the user.
	 * 
	 * @param user
	 * @return
	 */
	Iterable<? extends HibRole> getRoles(HibUser user);

	/**
	 * Load the groups of the user.
	 * 
	 * @param user
	 * @return
	 */
	Result<? extends HibGroup> getGroups(HibUser user);

	/**
	 * Load the effective roles for user via the shortcut edges.
	 * 
	 * @param fromUser
	 * @param authUser
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibRole> getRolesViaShortcut(HibUser fromUser, HibUser authUser, PagingParameters pagingInfo);

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
	Page<? extends HibGroup> getGroups(HibUser fromUser, HibUser authUser, PagingParameters pagingInfo);
}
