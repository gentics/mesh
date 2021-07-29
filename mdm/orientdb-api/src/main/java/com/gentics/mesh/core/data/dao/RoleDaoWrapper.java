package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.rest.role.RoleResponse;

/**
 * DAO for role operations.
 */
public interface RoleDaoWrapper extends RoleDao, DaoWrapper<HibRole>, DaoTransformable<HibRole, RoleResponse> {

}
