package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.GenericNode;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;

public class RoleImpl extends AbstractGenericNode implements Role {

	// TODO index on name
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public List<? extends Group> getGroups() {
		return out(HAS_ROLE).has(RoleImpl.class).toListExplicit(GroupImpl.class);
	}

	public Set<Permission> getPermissions(MeshVertex node) {
		Set<Permission> permissions = new HashSet<>();
		Set<? extends String> labels = outE(Permission.labels()).mark().inV().retain((MeshVertexImpl) node).back().label().toSet();
		for (String label : labels) {
			permissions.add(Permission.valueOfLabel(label));
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(Permission permission, GenericNode node) {
		return out(permission.label()).retain(node.getImpl()).hasNext();
	}

	public void addGroup(Group group) {
		linkOut(group.getImpl(), HAS_ROLE);
	}

	public void addPermissions(MeshVertex node, Permission... permissions) {
		for (Permission permission : permissions) {
			addFramedEdge(permission.label(), (MeshVertexImpl) node);
		}
	}

	// public void addPermission(Role role, MeshVertex node, Permissions... permissions) {
	// for(Permissions permission : permissions) {
	// role.addFramedEdge(permission.getLabel(), node);
	// }
	// GraphPermission permission = getGraphPermission(role, node);
	// // Create a new permission relation when no existing one could be found
	// if (permission == null) {
	// permission = permissionService.create(role, node);
	// }
	// for (int i = 0; i < permissionTypes.length; i++) {
	// //TODO tinkerpop - handle grant call. Javahandler?
	// // permission.grant(permissionTypes[i]);
	// }
	// }

	@Override
	public RoleResponse transformToRest(MeshAuthUser requestUser) {

		RoleResponse restRole = new RoleResponse();
		restRole.setName(getName());
		fillRest(restRole, requestUser);

		for (Group group : getGroups()) {
			GroupResponse restGroup = new GroupResponse();
			restGroup.setName(group.getName());
			restGroup.setUuid(group.getUuid());
			restRole.getGroups().add(restGroup);
		}

		return restRole;
	}

	public void revokePermissions(MeshVertex node, Permission... permissions) {

		for (Permission permission : permissions) {
			// System.out.println(inE(permission.label()).mark().outV().retain(node).back().next().getLabel());
			outE(permission.label()).mark().inV().retain((MeshVertexImpl) node).back().removeAll();
			// System.out.println(outE(permission.label()).mark().inV().retain(node).back().next().getLabel());
		}
	}

	public void delete() {
		getVertex().remove();
	}

	@Override
	public RoleImpl getImpl() {
		return this;
	}

}
