package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.group.HibGroup;

/**
 * A persisting extension to {@link SchemaDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingGroupDao extends GroupDao, PersistingDaoGlobal<HibGroup> {

}
