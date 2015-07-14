package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Tag;

public interface TagRoot extends RootVertex<Tag> {

//	Page<? extends Tag> findProjectTags(MeshAuthUser requestUser, String projectName, PagingInfo pagingInfo)
//			throws InvalidArgumentException;

//	Tag findByName(String projectName, String name);

	void addTag(Tag tag);

	void removeTag(Tag tag);

}
