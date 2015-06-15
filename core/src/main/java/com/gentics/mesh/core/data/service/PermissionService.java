package com.gentics.mesh.core.data.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.generic.MeshVertex;
import com.gentics.mesh.core.data.model.tinkerpop.GraphPermission;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.syncleus.ferma.FramedGraph;

@Component
public class PermissionService {

	@Autowired
	private FramedGraph framedGraph;

	public GraphPermission create(Role role, MeshVertex node) {
		return role.addPermission(node);
	}

}
