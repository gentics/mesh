package com.gentics.mesh.core.data.service;

import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.tinkerpop.GraphPermission;
import com.gentics.mesh.core.data.model.tinkerpop.Role;

public interface PermissionService {

	GraphPermission create(Role role, AbstractPersistable node);

}
