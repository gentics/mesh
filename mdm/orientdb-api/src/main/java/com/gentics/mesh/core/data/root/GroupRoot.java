package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Aggregation vertex for groups.
 */
public interface GroupRoot extends RootVertex<Group>, TransformableElementRoot<Group, GroupResponse> {

	public static final String TYPE = "groups";

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
	 * Add the group to the aggregation vertex.
	 * 
	 * @param group Group to be added
	 */
	void addGroup(Group group);

	/**
	 * Remove the group from the aggregation vertex.
	 * 
	 * @param group Group to be removed
	 */
	void removeGroup(Group group);

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
	TraversalResult<? extends User> getUsers(Group group);

	/**
	 * Return a traversal of roles that are assigned to the group.
	 *
	 * @param group
	 * @return Traversal of roles
	 */
	TraversalResult<? extends Role> getRoles(Group group);

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
	TransformablePage<? extends Role> getRoles(Group group, User user, PagingParameters pagingInfo);

	/**
	 * Return a page with all users that the given user can see.
	 *
	 * @param group
	 * @param requestUser
	 * @param pagingInfo
	 * @return Page with found users, an empty page is returned when no users could be found
	 */
	TransformablePage<? extends User> getVisibleUsers(Group group, MeshAuthUser requestUser, PagingParameters pagingInfo);

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

}
