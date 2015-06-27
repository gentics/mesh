package com.gentics.mesh.core.data.model.root;

import com.gentics.mesh.core.data.model.MeshVertex;
import com.gentics.mesh.core.data.model.TagFamily;
import com.gentics.mesh.core.data.model.root.impl.TagFamilyRootImpl;

public interface TagFamilyRoot extends MeshVertex {

	TagFamily create(String name);

	TagFamilyRootImpl getImpl();

}
