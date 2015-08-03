package com.gentics.mesh.core.data;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;

public interface User extends GenericVertex<UserResponse>, NamedNode {

	public static final String TYPE = "user";

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

	Node getReferencedNode();

	void setReferencedNode(Node node);

	boolean hasPermission(MeshVertex vertex, Permission permission);

	String[] getPermissionNames(MeshVertex vertex);

	List<? extends Group> getGroups();

	void addGroup(Group parentGroup);

	List<? extends Role> getRoles();

	Set<Permission> getPermissions(MeshVertex node);

	long getGroupCount();

	void disable();

	boolean isEnabled();

	void enable();

	void deactivate();

	void addCRUDPermissionOnRole(MeshVertex node, Permission permission, MeshVertex targetNode);

	UserReference transformToUserReference();

	UserImpl getImpl();

	void fillUpdateFromRest(RoutingContext rc, UserUpdateRequest requestModel, Handler<AsyncResult<User>> handler);

	void fillCreateFromRest(RoutingContext rc, UserCreateRequest requestModel, Group parentGroup, Handler<AsyncResult<User>> handler);


}
