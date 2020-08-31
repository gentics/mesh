package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

// TODO move the contents of this to GroupDao once migration is done
public interface GroupDaoWrapper extends GroupDao, DaoWrapper<HibGroup>, DaoTransformable<HibGroup, GroupResponse> {

	HibGroup loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm);

	Result<? extends HibGroup> findAll();

	TransformablePage<? extends HibGroup> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	Page<? extends HibGroup> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibGroup> extraFilter);

	HibGroup findByName(String name);

	HibGroup findByUuid(String uuid);

	/**
	 * Create a new group and assign it to the group root.
	 * 
	 * @param name
	 *            Name of the group
	 * @param user
	 *            User that is used to set the creator and editor references.
	 * @return Created group
	 */
	default HibGroup create(String name, HibUser user) {
		return create(name, user, null);
	}

	/**
	 * Create a new group and assign it to the group root.
	 * 
	 * @param name
	 *            Name of the group
	 * @param user
	 *            User that is used to set the creator and editor references.
	 * @param uuid
	 *            optional uuid
	 * @return Created group
	 */
	HibGroup create(String name, HibUser user, String uuid);

	/**
	 * Create the assignment event for the given user.
	 *
	 * @param group
	 * @param user
	 * @param assignment
	 *            Direction of the assignment
	 * @return
	 */
	GroupUserAssignModel createUserAssignmentEvent(HibGroup group, HibUser user, Assignment assignment);

	/**
	 * Create the assignment event for the given role.
	 *
	 * @param group
	 * @param role
	 * @param assignment
	 * @return
	 */
	GroupRoleAssignModel createRoleAssignmentEvent(HibGroup group, HibRole role, Assignment assignment);

	/**
	 * Add the group to the aggregation vertex.
	 * 
	 * @param group
	 *            HibGroup to be added
	 */
	void addGroup(HibGroup group);

	/**
	 * Remove the group from the aggregation vertex.
	 * 
	 * @param group
	 *            HibGroup to be removed
	 */
	void removeGroup(HibGroup group);

	/**
	 * Assign the given user to this group.
	 *
	 * @param group
	 * @param user
	 */
	void addUser(HibGroup group, HibUser user);

	/**
	 * Unassign the user from the group.
	 *
	 * @param group
	 * @param user
	 */
	void removeUser(HibGroup group, HibUser user);

	/**
	 * Assign the given role to this group.
	 *
	 * @param group
	 * @param role
	 */
	void addRole(HibGroup group, HibRole role);

	/**
	 * Unassign the role from this group.
	 *
	 * @param group
	 * @param role
	 */
	void removeRole(HibGroup group, HibRole role);

	/**
	 * Return a traversal of users that are assigned to the group.
	 *
	 * @param group
	 * @return Traversal of users
	 */
	Result<? extends HibUser> getUsers(HibGroup group);

	/**
	 * Return a traversal of roles that are assigned to the group.
	 *
	 * @param group
	 * @return Traversal of roles
	 */
	Result<? extends HibRole> getRoles(HibGroup group);

	/**
	 * Check whether the user has been assigned to the group.
	 *
	 * @param group
	 * @param user
	 * @return
	 */
	boolean hasUser(HibGroup group, HibUser user);

	/**
	 * Check whether the role has been assigned to the group.
	 *
	 * @param group
	 * @param role
	 * @return
	 */
	boolean hasRole(HibGroup group, HibRole role);

	/**
	 * Return a page with all visible roles that the given user can see.
	 *
	 * @param group
	 * @param user
	 *            user User which requested the resource
	 * @param pagingInfo
	 *            Paging information
	 * @return Page which contains the retrieved items
	 */
	TransformablePage<? extends HibRole> getRoles(HibGroup group, HibUser user, PagingParameters pagingInfo);

	/**
	 * Return a page with all users that the given user can see.
	 *
	 * @param group
	 * @param requestUser
	 * @param pagingInfo
	 * @return Page with found users, an empty page is returned when no users could be found
	 */
	TransformablePage<? extends HibUser> getVisibleUsers(HibGroup group, MeshAuthUser requestUser, PagingParameters pagingInfo);

	long computeCount();

	void delete(HibGroup group, BulkActionContext bac);

	boolean update(HibGroup group, InternalActionContext ac, EventQueueBatch batch);

	HibGroup loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	HibGroup create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	String getETag(HibGroup element, InternalActionContext ac);

	String getAPIPath(HibGroup group, InternalActionContext ac);

}
