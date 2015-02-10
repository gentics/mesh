package com.gentics.cailun.core.rest.model.auth;

import lombok.Data;

import org.apache.shiro.authz.Permission;

import com.gentics.cailun.core.rest.model.CaiLunNode;

@Data
public class CaiLunPermission implements Permission {

	private PermissionType type;
	private CaiLunNode targetNode;

	public CaiLunPermission(CaiLunNode targetNode, PermissionType type) {
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
}
