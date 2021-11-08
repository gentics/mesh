package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.tagfamily.HibTagFamily;

/**
 * A persisting extension to {@link TagFamilyDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingTagFamilyDao extends TagFamilyDao, PersistingDaoGlobal<HibTagFamily> {

}
