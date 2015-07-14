package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.util.InvalidArgumentException;

public interface Group extends GenericNode<GroupResponse>, NamedNode {

	void addUser(User user);

	void addRole(Role role);

	List<? extends User> getUsers();

	List<? extends Role> getRoles();

	boolean hasUser(User user);

	boolean hasRole(Role role);

	Page<? extends Role> getRoles(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

	Page<? extends User> getVisibleUsers(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

	void removeRole(Role role);

	void removeUser(User user);

	Role createRole(String name);

	User createUser(String username);

	GroupImpl getImpl();

}
