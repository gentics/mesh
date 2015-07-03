package com.gentics.mesh.core.data;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.user.UserResponse;

public interface User extends GenericNode {

	String getUsername();

	void setUsername(String string);

	String getEmailAddress();

	void setEmailAddress(String email);

	String getLastname();

	void setLastname(String lastname);

	String getFirstname();

	void setFirstname(String firstname);

	String getPasswordHash();

	void setPasswordHash(String hash);

	void setPassword(String password);

	boolean hasPermission(MeshVertex vertex, Permission permission);

	String[] getPermissionNames(MeshVertex vertex);

	UserResponse transformToRest();

	List<? extends Group> getGroups();

	void addGroup(Group parentGroup);

	void delete();

	UserImpl getImpl();

	List<? extends Role> getRoles();

	Set<Permission> getPermissions(MeshVertex node);

	long getGroupCount();

	void disable();

	boolean isEnabled();

	void enable();

	void deactivate();

	void addCRUDPermissionOnRole(MeshVertex node, Permission permission, GenericNode targetNode);

}
