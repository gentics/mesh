package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.util.InvalidArgumentException;

public interface TagRoot extends RootVertex<Tag> {

	Page<? extends Tag> findProjectTags(MeshAuthUser requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo)
			throws InvalidArgumentException;

	Tag findByName(String projectName, String name);

	void addTag(Tag tag);

	void removeTag(Tag tag);

}
