package com.gentics.cailun.core.rest.model.auth.basic;

import lombok.Data;

import org.apache.shiro.authz.Permission;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import com.gentics.cailun.core.rest.model.GenericNode;

@Data
public abstract class AbstractShiroGraphPermission implements Permission {

	protected GenericNode targetNode;

	public AbstractShiroGraphPermission(GenericNode targetNode) {
		this.targetNode = targetNode;
	}

	/**
	 * We implement this method to comply with shiro api.
	 */
	@Override
	public boolean implies(Permission p) {
		return false;
	}

	public abstract Label getPermissionNodeLabel();

	public abstract boolean isPermitted(Node permissioNode, Node targetNode);

}
