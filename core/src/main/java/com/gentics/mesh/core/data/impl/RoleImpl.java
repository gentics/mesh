package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;

public class RoleImpl extends AbstractGenericVertex<RoleResponse> implements Role {

	// TODO index on name
	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public List<? extends Group> getGroups() {
		return out(HAS_ROLE).has(RoleImpl.class).toListExplicit(GroupImpl.class);
	}

	@Override
	public Set<Permission> getPermissions(MeshVertex node) {
		Set<Permission> permissions = new HashSet<>();
		Set<? extends String> labels = outE(Permission.labels()).mark().inV().retain((MeshVertexImpl) node).back().label().toSet();
		for (String label : labels) {
			permissions.add(Permission.valueOfLabel(label));
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(Permission permission, GenericVertex<?> node) {
		return out(permission.label()).retain(node.getImpl()).hasNext();
	}

	@Override
	public void addGroup(Group group) {
		linkOut(group.getImpl(), HAS_ROLE);
	}

	@Override
	public void addPermissions(MeshVertex node, Permission... permissions) {
		for (Permission permission : permissions) {
			addFramedEdge(permission.label(), (MeshVertexImpl) node);
		}
	}

	@Override
	public Role transformToRest(RoutingContext rc, Handler<AsyncResult<RoleResponse>> handler) {

		RoleResponse restRole = new RoleResponse();
		restRole.setName(getName());
		fillRest(restRole, rc);

		for (Group group : getGroups()) {
			GroupResponse restGroup = new GroupResponse();
			restGroup.setName(group.getName());
			restGroup.setUuid(group.getUuid());
			restRole.getGroups().add(restGroup);
		}

		handler.handle(Future.succeededFuture(restRole));
		return this;
	}

	@Override
	public void revokePermissions(MeshVertex node, Permission... permissions) {

		for (Permission permission : permissions) {
			// System.out.println(inE(permission.label()).mark().outV().retain(node).back().next().getLabel());
			outE(permission.label()).mark().inV().retain((MeshVertexImpl) node).back().removeAll();
			// System.out.println(outE(permission.label()).mark().inV().retain(node).back().next().getLabel());
		}
	}

	@Override
	public void delete() {
		getVertex().remove();
	}

	@Override
	public RoleImpl getImpl() {
		return this;
	}

}
