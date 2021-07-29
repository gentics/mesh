package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.rest.tag.TagResponse;

/**
 * DAO for {@link HibTag} operations.
 */
public interface TagDaoWrapper extends TagDao, DaoWrapper<HibTag>, DaoTransformable<HibTag, TagResponse> {

}
