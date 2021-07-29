package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.rest.group.GroupResponse;

/**
 * The intermediate group dao.
 * 
 */
public interface GroupDaoWrapper extends GroupDao, DaoWrapper<HibGroup>, DaoTransformable<HibGroup, GroupResponse> {

}
