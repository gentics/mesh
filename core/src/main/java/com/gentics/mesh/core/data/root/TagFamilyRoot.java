package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;

public interface TagFamilyRoot extends MeshVertex {

	TagFamily create(String name);

	TagFamilyRootImpl getImpl();

}
