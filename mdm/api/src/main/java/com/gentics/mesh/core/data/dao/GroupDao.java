package com.gentics.mesh.core.data.dao;

import java.util.Collection;
import java.util.Map;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.group.Group;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for {@link Group}.
 */
public interface GroupDao extends DaoGlobal<Group>, DaoTransformable<Group, GroupResponse> {

	/**
	 * Create a new group and assign it to the group root.
	 * 
	 * @param name
	 *            Name of the group
	 * @param user
	 *            User that is used to set the creator and editor references.
	 * @return Created group
	 */
	default Group create(String name, User user) {
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
	Group create(String name, User user, String uuid);

	/**
	 * Create the assignment event for the given user.
	 *
	 * @param group
	 * @param user
	 * @param assignment
	 *            Direction of the assignment
	 * @return
	 */
	GroupUserAssignModel createUserAssignmentEvent(Group group, User user, Assignment assignment);

	/**
	 * Create the assignment event for the given role.
	 *
	 * @param group
	 * @param role
	 * @param assignment
	 * @return
	 */
	GroupRoleAssignModel createRoleAssignmentEvent(Group group, Role role, Assignment assignment);

	/**
	 * Assign the given user to this group.
	 *
	 * @param group
	 * @param user
	 */
	void addUser(Group group, User user);

	/**
	 * Unassign the user from the group.
	 *
	 * @param group
	 * @param user
	 */
	void removeUser(Group group, User user);

	/**
	 * Assign the given role to this group.
	 *
	 * @param group
	 * @param role
	 */
	void addRole(Group group, Role role);

	/**
	 * Unassign the role from this group.
	 *
	 * @param group
	 * @param role
	 */
	void removeRole(Group group, Role role);

	/**
	 * Return a traversal of users that are assigned to the group.
	 *
	 * @param group
	 * @return Traversal of users
	 */
	Result<? extends User> getUsers(Group group);

	/**
	 * Return a traversal of roles that are assigned to the group.
	 *
	 * @param group
	 * @return Traversal of roles
	 */
	Result<? extends Role> getRoles(Group group);

	/**
	 * Return the roles for all given groups
	 * @param groups collection of groups
	 * @return map of group to the collections of roles
	 */
	Map<Group, Collection<? extends Role>> getRoles(Collection<Group> groups);

	/**
	 * Check whether the user has been assigned to the group.
	 *
	 * @param group
	 * @param user
	 * @return
	 */
	boolean hasUser(Group group, User user);

	/**
	 * Check whether the role has been assigned to the group.
	 *
	 * @param group
	 * @param role
	 * @return
	 */
	boolean hasRole(Group group, Role role);

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
	Page<? extends Role> getRoles(Group group, User user, PagingParameters pagingInfo);

	/**
	 * Return a page with all users that the given user can see.
	 *
	 * @param group
	 * @param requestUser
	 * @param pagingInfo
	 * @return Page with found users, an empty page is returned when no users could be found
	 */
	Page<? extends User> getVisibleUsers(Group group, User requestUser, PagingParameters pagingInfo);

	/**
	 * Create the group.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	Group create(InternalActionContext ac, EventQueueBatch batch, String uuid);
}
