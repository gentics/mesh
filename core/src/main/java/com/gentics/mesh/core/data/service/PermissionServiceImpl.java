package com.gentics.mesh.core.data.service;

import org.jglue.totorom.FramedGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.tinkerpop.GraphPermission;
import com.gentics.mesh.core.data.model.tinkerpop.Role;

@Component
public class PermissionServiceImpl implements PermissionService {

	@Autowired
	private FramedGraph framedGraph;

	@Override
	public GraphPermission create(Role role, AbstractPersistable node) {
		return framedGraph.addEdge(null, node.asVertex(), role.asVertex(), AuthRelationships.HAS_PERMISSION, GraphPermission.class);
	}

}
