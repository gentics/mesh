package com.gentics.cailun.demo.verticle;

import org.neo4j.graphdb.DynamicLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;

import com.gentics.cailun.core.rest.model.GenericNode;
import com.gentics.cailun.core.rest.model.auth.AbstractShiroGraphPermission;

public class CustomShiroGraphPermission extends AbstractShiroGraphPermission {

	public static final Label PERMISSION_NODE_LABEL = DynamicLabel.label(CustomPermission.class.getSimpleName());

	public CustomShiroGraphPermission(GenericNode targetNode) {
		super(targetNode);
	}

	@Override
	public Label getPermissionNodeLabel() {
		return PERMISSION_NODE_LABEL;
	}

	@Override
	public boolean isPermitted(Node permissioNode, Node targetNode) {
		return (boolean) permissioNode.getProperty("customActionAllowed") == true;
	}

}
