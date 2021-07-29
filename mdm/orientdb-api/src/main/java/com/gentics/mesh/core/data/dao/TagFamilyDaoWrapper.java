package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;

/**
 * Intermediate DAO for {@link HibTagFamily}. TODO MDM the methods should be moved to {@link TagFamilyDao}
 */
public interface TagFamilyDaoWrapper
	extends TagFamilyDao, DaoWrapper<HibTagFamily>, DaoTransformable<HibTagFamily, TagFamilyResponse> {

}
