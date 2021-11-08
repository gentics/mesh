package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.role.HibRole;

/**
 * A persisting extension to {@link RoleDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingRoleDao extends RoleDao, PersistingDaoGlobal<HibRole> {

}
