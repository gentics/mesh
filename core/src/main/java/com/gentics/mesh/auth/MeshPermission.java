package com.gentics.mesh.auth;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.relationship.Permission;

public class MeshPermission {

	private Permission permType;
	private MeshVertex targetNode;

	public MeshPermission(MeshVertex targetNode, Permission permType) {
		this.permType = permType;
		this.targetNode = targetNode;
	}

	public Permission getPermType() {
		return permType;
	}

	public MeshVertex getTargetNode() {
		return targetNode;
	}

}
