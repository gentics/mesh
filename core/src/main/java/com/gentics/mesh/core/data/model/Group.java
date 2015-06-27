package com.gentics.mesh.core.data.model;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.impl.GroupImpl;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;

public interface Group extends GenericNode {

	String getName();

	void setName(String name);

	void addUser(MeshUser user);

	void addRole(Role role);

	List<? extends MeshUser> getUsers();

	List<? extends Role> getRoles();

	boolean hasUser(MeshUser user);

	boolean hasRole(Role role);

	Page<? extends Role> getRoles(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

	Page<? extends MeshUser> getVisibleUsers(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException;

	GroupResponse transformToRest(MeshAuthUser requestUser);

	void removeRole(Role role);

	void removeUser(MeshUser user);

	Role createRole(String name);

	MeshUser createUser(String username);

	GroupImpl getImpl();

}
