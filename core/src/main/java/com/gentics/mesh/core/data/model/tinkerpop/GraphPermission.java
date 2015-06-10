package com.gentics.mesh.core.data.model.tinkerpop;

import org.apache.shiro.authz.Permission;

import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.generic.MeshEdge;
import com.gentics.mesh.core.data.model.generic.MeshVertex;

public class GraphPermission extends MeshEdge implements org.apache.shiro.authz.Permission {
	
	
	public Role getRole() {
		return inV().next(Role.class);
	}

	public MeshVertex getTargetNode() {
		return outV().next(MeshVertex.class);
	}

	public boolean isPermitted(PermissionType update) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean implies(Permission p) {
		// TODO Auto-generated method stub
		return false;
	}

	public void revoke(PermissionType permissionType) {
		// TODO Auto-generated method stub
		
	}

	// TODO add permissions
	
	
//	DynamicProperties permissions = new DynamicPropertiesContainer();
//
//	protected GraphPermission() {
//	}
//
//	@PersistenceConstructor
//	public GraphPermission(Role role, AbstractPersistable targetNode) {
//		this.role = role;
//		this.targetNode = targetNode;
//	}
//
//	public void grant(PermissionType type) {
//		permissions.setProperty(type.getPropertyName(), true);
//	}
//
//	public void revoke(PermissionType type) {
//		permissions.setProperty(type.getPropertyName(), false);
//	}
//
//	public boolean isPermitted(PermissionType type) {
//		return (boolean) permissions.getProperty(type.getPropertyName(), false);
//	}

//	public void grant(PermissionType permissionType);
//
//	/**
//	 * Returns the permission properties for this relationship.
//	 * 
//	 * @return
//	 */
//	public DynamicProperties getPermissions() {
//		return permissions;
//	}
//
//	@Override
//	public boolean implies(org.apache.shiro.authz.Permission p) {
//		return false;
//	}
//
//	public Role getRole() {
//		return role;
//	}
//
//	public AbstractPersistable getTargetNode() {
//		return targetNode;
//	}

//	public void revoke(PermissionType permissionType);

//	public boolean isPermitted(PermissionType read);
}
