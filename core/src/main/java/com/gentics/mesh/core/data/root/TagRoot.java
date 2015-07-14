package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Tag;

public interface TagRoot extends RootVertex<Tag> {

	void addTag(Tag tag);

	void removeTag(Tag tag);

}
