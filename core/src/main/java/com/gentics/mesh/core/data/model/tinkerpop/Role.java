package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.Permission;

public class Role extends GenericNode {

	//TODO index on name
	public String getName() {
		return getProperty("name");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	//	public List<? extends GraphPermission> getPermissions() {
	//		return outE(HAS_PERMISSION).toList(GraphPermission.class);
	//	}

	//	@Adjacency(label = HAS_ROLE, direction = Direction.OUT)
	public List<? extends Group> getGroups() {
		return out(HAS_ROLE).toList(Group.class);
	}

	public Set<Permission> getPermissions(MeshNode node) {
		Set<Permission> permissions = new HashSet<>();
		Set<? extends String> labels = outE(Permission.labels()).mark().outV().hasId(node.getId()).back().label().toSet();
		for (String label : labels) {
			permissions.add(Permission.valueOf(label));
		}
		return permissions;
	}

	public void addGroup(Group group) {
		linkOut(group, HAS_ROLE);
	}

	public void addPermissions(MeshVertex node, Permission... permissions) {
		for (Permission permission : permissions) {
			addFramedEdge(permission.getLabel(), node);
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

	//	public GraphPermission getGraphPermission(Role role, MeshVertex node) {
	//		//		return roleRepository.findPermission(role.getId(), node.getId());
	//		return null;
	//	}

	public void revokePermissions(MeshVertex node, Permission... permissions) {

		for (Permission permission : permissions) {
			//outE(permission.getLabel()).mark().outV().hasId(node.getId()).back().remove();
		}
		//		GraphPermission permission = getGraphPermission(role, node);
		//		// Create a new permission relation when no existing one could be found
		//		if (permission == null) {
		//			return null;
		//		}
		//		for (int i = 0; i < permissions.length; i++) {
		//			permission.revoke(permissions[i]);
		//		}
		//		role.addPermission(node);
		//		//		permission = neo4jTemplate.save(permission);
		//		return permission;
	}

}
