package com.gentics.cailun.core.rest.model.auth;

import lombok.Data;
import lombok.NoArgsConstructor;

import org.apache.shiro.authz.Permission;

import com.gentics.cailun.core.rest.model.GenericNode;
import com.gentics.cailun.core.rest.model.auth.BasicPermissionRelationship.BasicPermissionTypes;

@NoArgsConstructor
@Data
public class BasicPermissionRelationship extends AbstractPermissionRelationship {

	private static final long serialVersionUID = 491752627336037999L;

	public enum BasicPermissionTypes {
		READ, WRITE, DELETE, CREATE
	}

	public BasicPermissionRelationship(Role adminRole, GenericNode currentNode) {
		super(adminRole, currentNode);
	}

	public BasicPermissionRelationship(GenericNode object, BasicPermissionTypes read2) {
		// TODO Auto-generated constructor stub
	}

	private boolean read = false;
	private boolean write = false;
	private boolean delete = false;
	private boolean create = false;

	@Override
	public boolean implies(Permission p) {
		if (!(p instanceof BasicPermissionRelationship)) {
			return false;
		}
		BasicPermissionRelationship pp = (BasicPermissionRelationship) p;
		// TODO handle specific permission
		return pp.getObject().equals(getObject());
		// return pp.getTargetObject().equals(targetObject) && actionName.equals(pp.getActionName());
	}

}
