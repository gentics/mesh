package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.Permission;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.core.rest.role.response.RoleResponse;

public class Role extends GenericNode {

	//TODO index on name
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public List<? extends Group> getGroups() {
		return out(HAS_ROLE).has(Role.class).toListExplicit(Group.class);
	}

	public Set<Permission> getPermissions(MeshVertex node) {
		Set<Permission> permissions = new HashSet<>();
		Set<? extends String> labels = outE(Permission.labels()).mark().inV().retain(node).back().label().toSet();
		for (String label : labels) {
			permissions.add(Permission.valueOfLabel(label));
		}
		return permissions;
	}

	public void addGroup(Group group) {
		linkOut(group, HAS_ROLE);
	}

	public void addPermissions(MeshVertex node, Permission... permissions) {
		for (Permission permission : permissions) {
			addFramedEdge(permission.label(), node);
		}
	}

	//	public void addPermission(Role role, MeshVertex node, Permissions... permissions) {
	//		for(Permissions permission : permissions) {
	//			role.addFramedEdge(permission.getLabel(), node);
	//		}
	//		GraphPermission permission = getGraphPermission(role, node);
	//		// Create a new permission relation when no existing one could be found
	//		if (permission == null) {
	//			permission = permissionService.create(role, node);
	//		}
	//		for (int i = 0; i < permissionTypes.length; i++) {
	//			//TODO tinkerpop - handle grant call. Javahandler?
	//			//			permission.grant(permissionTypes[i]);
	//		}
	//	}

	public RoleResponse transformToRest() {

		RoleResponse restRole = new RoleResponse();
		restRole.setUuid(getUuid());
		restRole.setName(getName());

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
			//System.out.println(inE(permission.label()).mark().outV().retain(node).back().next().getLabel());
			outE(permission.label()).mark().inV().retain(node).back().removeAll();
			//System.out.println(outE(permission.label()).mark().inV().retain(node).back().next().getLabel());
		}
	}

	public void delete() {
		getVertex().remove();
	}

}
