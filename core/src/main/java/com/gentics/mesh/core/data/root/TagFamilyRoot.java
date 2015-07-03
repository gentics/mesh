package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;

public interface TagFamilyRoot extends RootVertex<TagFamily> {

	TagFamily create(String name);

	TagFamilyRootImpl getImpl();

	void removeTagFamily(TagFamily tagFamily);

	void addTagFamily(TagFamily tagFamily);

}
