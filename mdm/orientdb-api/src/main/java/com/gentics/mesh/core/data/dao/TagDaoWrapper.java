package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.madl.traversal.TraversalResult;

public interface TagDaoWrapper extends TagDao, TagRoot {

	/**
	 * Find all tags of the given tagfamily.
	 * 
	 * @param tagFamily
	 * @return
	 */
	TraversalResult<? extends Tag> findAll(TagFamily tagFamily);

}
