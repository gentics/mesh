package com.gentics.mesh.core.data;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.model.MeshElement;

public interface TransformableElement<T extends RestModel> extends HibTransformableElement<T>, MeshElement {

}