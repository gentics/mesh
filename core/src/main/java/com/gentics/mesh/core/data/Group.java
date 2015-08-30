package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.util.InvalidArgumentException;

public interface Group extends GenericVertex<GroupResponse>, NamedVertex, IndexedVertex {

	public static final String TYPE = "group";

	/**
	 * Assign the given user to this group
	 * 
	 * @param user
	 */
	void addUser(User user);

	/**
	 * Unassign the user from the group
	 * 
	 * @param user
	 */
	void removeUser(User user);

	/**
	 * Assign the given role to this group
	 * 
	 * @param role
	 */
	void addRole(Role role);

	/**
	 * Unassign the role from this group
	 * 
	 * @param role
	 */
	void removeRole(Role role);

	List<? extends User> getUsers();

	List<? extends Role> getRoles();

	boolean hasUser(User user);

	boolean hasRole(Role role);

	Page<? extends Role> getRoles(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

	Page<? extends User> getVisibleUsers(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

}
