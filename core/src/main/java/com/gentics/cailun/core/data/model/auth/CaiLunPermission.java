package com.gentics.cailun.core.data.model.auth;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;

import com.gentics.cailun.core.data.model.generic.AbstractPersistable;

public class CaiLunPermission implements Permission {

	private PermissionType type;
	private AbstractPersistable targetNode;

	public CaiLunPermission(AbstractPersistable targetNode, PermissionType type) {
		this.type = type;
		this.targetNode = targetNode;
	}

	@Override
	public boolean implies(Permission p) {
		// Check whether the given permission is in fact a graph permission.
		if (!(p instanceof GraphPermission)) {
			return false;
		}
		GraphPermission perm = (GraphPermission) p;
		return perm.isPermitted(type);
	}

	public AbstractPersistable getTargetNode() {
		return targetNode;
	}
	
	@Override
	public String toString() {
		//TODO check for null
		return this.targetNode.getId() + "#" + this.type.getPropertyName();
	}
}
