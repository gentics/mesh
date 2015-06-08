package com.gentics.mesh.core.data.service;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.tinkerpop.GraphPermission;
import com.gentics.mesh.core.data.model.tinkerpop.Role;

@Component
public class PermissionServiceImpl implements PermissionService {

	@Override
	public GraphPermission create(Role role, AbstractPersistable node) {
		// TODO Auto-generated method stub
		return null;
	}

}
