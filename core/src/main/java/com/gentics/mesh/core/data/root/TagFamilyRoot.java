package com.gentics.mesh.core.data.root;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;

public interface TagFamilyRoot extends MeshVertex {

	TagFamily create(String name);

	TagFamilyRootImpl getImpl();

	List<? extends TagFamily> getTagFamilies();

	void removeTagFamily(TagFamily tagFamily);

	void addTagFamily(TagFamily tagFamily);

}
