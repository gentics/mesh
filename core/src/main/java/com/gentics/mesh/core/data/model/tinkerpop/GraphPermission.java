package com.gentics.mesh.core.data.model.tinkerpop;

import org.jglue.totorom.FramedEdge;

import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.generic.AbstractPersistable;

public class GraphPermission extends FramedEdge {

	//, org.apache.shiro.authz.Permission
	
	
	public Role getRole() {
		return inV().frame(Role.class);
	}

	public AbstractPersistable getTargetNode() {
		return outV().frame(AbstractPersistable.class);
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
