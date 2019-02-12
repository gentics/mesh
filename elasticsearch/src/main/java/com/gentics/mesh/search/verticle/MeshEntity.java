package com.gentics.mesh.search.verticle;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.search.index.Transformer;

public interface MeshEntity<T extends MeshCoreVertex<? extends RestModel, T>> {
	Transformer<T> getTransformer();
	RootVertex<T> getRootVertex();
}
