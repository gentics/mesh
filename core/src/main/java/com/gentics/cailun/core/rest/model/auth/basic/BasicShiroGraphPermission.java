package com.gentics.cailun.core.rest.model.auth.basic;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import com.gentics.cailun.core.rest.model.GenericNode;
import com.gentics.cailun.core.rest.model.auth.AbstractShiroGraphPermission;

public class BasicShiroGraphPermission extends AbstractShiroGraphPermission {

	public static final Label PERMISSION_NODE_LABEL = DynamicLabel.label(BasicPermission.class.getSimpleName());

	protected BasicPermissionRights typeToCheck;

	public BasicShiroGraphPermission(GenericNode targetNode, BasicPermissionRights typeToCheck) {
		super(targetNode);
		this.typeToCheck = typeToCheck;
	}

	public boolean isPermitted(Node permissioNode, Node targetNode) {
		if ((boolean) permissioNode.getProperty(typeToCheck.getPropertyName()) == true) {
			return true;
		}
		return false;
	}

	public Label getPermissionNodeLabel() {
		return PERMISSION_NODE_LABEL;
	}

}
